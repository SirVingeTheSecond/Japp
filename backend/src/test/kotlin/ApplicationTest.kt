import com.japp.database.DatabaseSchema
import com.japp.models.Currency
import com.japp.models.Result
import com.japp.models.SplitType
import com.japp.models.domain.User
import com.japp.models.dto.*
import com.japp.repositories.implementations.*
import com.japp.security.PasswordHasher
import com.japp.services.*
import com.japp.websocket.WebSocketManager
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ApplicationTest : AnnotationSpec() {
    @BeforeClass
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", user = "root", password = "")
    }

    @AfterClass
    fun teardown() {
    }

    @Before
    fun create() {
        DatabaseSchema.createTables()
    }

    @After
    fun drop() {
        DatabaseSchema.dropTables()
    }

    @Test
    suspend fun createUser() {
        val userRepository = UserRepository()
        val passwordHasher = PasswordHasher()

        // creating the mock user
        transaction {
            userRepository.create(
                User(
                    id = 0,
                    username = "Niels69",
                    firstname = "Niels",
                    lastname = "Nielsen",
                    email = "hello@gmail.com",
                    passwordHash = passwordHasher.hash("secret12345"),
                    phone = "1234567890",
                    profilePicture = null,
                    fcmToken = null,
                    createdAt = System.currentTimeMillis().toString()
                )
            )
        }

        // check if user is created
        val findUser = transaction { userRepository.findByEmail("hello@gmail.com") }
        findUser?.id shouldBe 1
    }

    @Test
    suspend fun deleteUser() {
        val userRepository = UserRepository()
        val passwordHasher = PasswordHasher()

        // creating the mock user
        transaction {
            userRepository.create(
                User(
                    id = 0,
                    username = "Niels69",
                    firstname = "Niels",
                    lastname = "Nielsen",
                    email = "hello@gmail.com",
                    passwordHash = passwordHasher.hash("secret12345"),
                    phone = "1234567890",
                    profilePicture = null,
                    fcmToken = null,
                    createdAt = System.currentTimeMillis().toString()
                )
            )
        }

        // now that user is created delete that boi

        transaction { userRepository.delete(1) }
        val findUser = transaction { userRepository.findById(1) }
        findUser.shouldBe(null)
    }

    @Test
    suspend fun signupAndLogin() {
        // using non-mocked for full logic
        val userRepository = UserRepository()
        val passwordHasher = PasswordHasher()
        val jwtSecret = "test-secret-key"
        val jwtIssuer = "test-issuer"
        val jwtAudience = "test-audience"

        // initializing the service...
        val authService = AuthService(
            userRepository,
            passwordHasher,
            jwtSecret,
            jwtIssuer,
            jwtAudience
        )

        // test user as a signuprequest
        val userTest = SignupRequest(
            email = "hello@gmail.com",
            password = "secret12345",
            username = "Niels69",
            firstname = "Niels",
            lastname = "Nielsen",
            phone = "1234567890"
        )

        // test user as a login request
        val user = LoginRequest(userTest.email, userTest.password)

        // creating the user via signup
        val result = authService.signup(userTest)
        result is Result.Success

        // verify user is created by trying to log in
        val signupResponse = authService.login(user)
        signupResponse is Result.Success
    }

    @Test
    suspend fun createGroup() {

        val userRepository = UserRepository()
        val groupRepository = GroupRepository()
        val passwordHasher = PasswordHasher()
        val activityRepository = mockk<ActivityRepository>()
        val messageRepository = mockk<MessageRepository>()
        val webSocketManager = mockk<WebSocketManager>()
        val expenseRepository = mockk<ExpenseRepository>()
        val debtHistoryRepository = mockk<DebtHistoryRepository>()

        val activityService = ActivityService(activityRepository, userRepository, groupRepository)
        val messageService = MessageService(messageRepository, groupRepository, userRepository, webSocketManager)

        // creating mock user for test
        transaction {
            userRepository.create(
                User(
                    id = 0,
                    username = "Niels69",
                    firstname = "Niels",
                    lastname = "Nielsen",
                    email = "hello@gmail.com",
                    passwordHash = passwordHasher.hash("secret12345"),
                    phone = "1234567890",
                    profilePicture = null,
                    fcmToken = null,
                    createdAt = System.currentTimeMillis().toString()
                )
            )
        }

        // create group
        val createGroupRequest = CreateGroupRequest("group name", "group description")
        val groupService = GroupService(
            groupRepository,
            userRepository,
            activityService,
            messageService,
            expenseRepository,
            debtHistoryRepository
        )

        // assert that group can be created and that user is in group
        groupService.createGroup(createGroupRequest, 0)
        groupService.getGroupById(0, 0)
    }

    @Test
    suspend fun deleteGroup() {

        val userRepository = UserRepository()
        val groupRepository = GroupRepository()
        val passwordHasher = PasswordHasher()

        val activityRepository = mockk<ActivityRepository>()
        val messageRepository = mockk<MessageRepository>()
        val webSocketManager = mockk<WebSocketManager>()
        val expenseRepository = mockk<ExpenseRepository>()
        val debtHistoryRepository = mockk<DebtHistoryRepository>()

        val activityService = ActivityService(activityRepository, userRepository, groupRepository)
        val messageService = MessageService(messageRepository, groupRepository, userRepository, webSocketManager)

        // creating mock user
        transaction {
            userRepository.create(
                User(
                    id = 0,
                    username = "Niels69",
                    firstname = "Niels",
                    lastname = "Nielsen",
                    email = "hello@gmail.com",
                    passwordHash = passwordHasher.hash("secret12345"),
                    phone = "1234567890",
                    profilePicture = null,
                    fcmToken = null,
                    createdAt = System.currentTimeMillis().toString()
                )
            )
        }

        // create group
        val createGroupRequest = CreateGroupRequest("group name", "group description")
        val groupService = GroupService(
            groupRepository,
            userRepository,
            activityService,
            messageService,
            expenseRepository,
            debtHistoryRepository
        )
        groupService.createGroup(createGroupRequest, 0)

        // leave group and assert user has left group
        groupService.leaveGroup(0, 0)

        // assert that the user is not part of group anymore TODO should be rewritten
        groupService.getUserGroups(0).toString().contains("[]")
    }

    @Test
    suspend fun createExpense() {

        val userRepository = UserRepository()
        val expenseRepository = ExpenseRepository()
        val groupRepository = GroupRepository()
        val settlementRepository = SettlementRepository()
        val activityRepository = mockk<ActivityRepository>()
        val messageRepository = mockk<MessageRepository>()
        val webSocketManager = mockk<WebSocketManager>()
        val debtHistoryRepository = mockk<DebtHistoryRepository>()

        val passwordHasher = PasswordHasher()

        val activityService = ActivityService(activityRepository, userRepository, groupRepository)
        val messageService = MessageService(messageRepository, groupRepository, userRepository, webSocketManager)
        val expenseService = ExpenseService(
            expenseRepository,
            groupRepository,
            userRepository,
            settlementRepository,
            activityService,
            messageService
        )

        transaction {
            userRepository.create(
                User(
                    id = 0,
                    username = "Niels69",
                    firstname = "Niels",
                    lastname = "Nielsen",
                    email = "hello@gmail.com",
                    passwordHash = passwordHasher.hash("secret12345"),
                    phone = "1234567890",
                    profilePicture = null,
                    fcmToken = null,
                    createdAt = System.currentTimeMillis().toString()
                )
            )
        }
        val createGroupRequest = CreateGroupRequest("group name", "group description")
        val groupService = GroupService(
            groupRepository,
            userRepository,
            activityService,
            messageService,
            expenseRepository,
            debtHistoryRepository
        )
        groupService.createGroup(createGroupRequest, 1)

        val expenseRequest = CreateExpenseRequest(1, 300.0, "test expense", null, Currency.DKK, SplitType.EQUAL)

        expenseService.createExpense(expenseRequest, 1)
        expenseService.getGroupExpenses(1, 1)
    }

    @Test
    suspend fun deleteExpense() {

        val userRepository = UserRepository()
        val expenseRepository = ExpenseRepository()
        val groupRepository = GroupRepository()
        val settlementRepository = SettlementRepository()
        val activityRepository = mockk<ActivityRepository>()
        val messageRepository = mockk<MessageRepository>()
        val webSocketManager = mockk<WebSocketManager>()
        val passwordHasher = PasswordHasher()
        val debtHistoryRepository = mockk<DebtHistoryRepository>()

        val activityService = ActivityService(activityRepository, userRepository, groupRepository)
        val messageService = MessageService(messageRepository, groupRepository, userRepository, webSocketManager)
        val expenseService = ExpenseService(
            expenseRepository,
            groupRepository,
            userRepository,
            settlementRepository,
            activityService,
            messageService
        )

        transaction {
            userRepository.create(
                User(
                    id = 0,
                    username = "Niels69",
                    firstname = "Niels",
                    lastname = "Nielsen",
                    email = "hello@gmail.com",
                    passwordHash = passwordHasher.hash("secret12345"),
                    phone = "1234567890",
                    profilePicture = null,
                    fcmToken = null,
                    createdAt = System.currentTimeMillis().toString()
                )
            )
        }
        val createGroupRequest = CreateGroupRequest("group name", "group description")
        val groupService = GroupService(
            groupRepository,
            userRepository,
            activityService,
            messageService,
            expenseRepository,
            debtHistoryRepository
        )
        groupService.createGroup(createGroupRequest, 1)

        val expenseRequest = CreateExpenseRequest(1, 300.0, "test expense", null, Currency.DKK, SplitType.EQUAL)

        expenseService.createExpense(expenseRequest, 1)
        expenseService.deleteExpense(1, 1)

        // assert that the expense is deleted TODO should be rewritten
        expenseService.getGroupExpenses(1, 1).toString().contains("[]")
    }

    @Test
    suspend fun expenseBalances() {

        val userRepository = UserRepository()
        val expenseRepository = ExpenseRepository()
        val groupRepository = GroupRepository()
        val activityRepository = mockk<ActivityRepository>()
        val settlementRepository = SettlementRepository()
        val messageRepository = mockk<MessageRepository>()
        val webSocketManager = mockk<WebSocketManager>()
        val passwordHasher = PasswordHasher()
        val debtHistoryRepository = mockk<DebtHistoryRepository>()

        val activityService = ActivityService(activityRepository, userRepository, groupRepository)
        val messageService = MessageService(messageRepository, groupRepository, userRepository, webSocketManager)
        val expenseService = ExpenseService(
            expenseRepository,
            groupRepository,
            userRepository,
            settlementRepository,
            activityService,
            messageService
        )

        transaction {
            userRepository.create(
                User(
                    id = 0,
                    username = "Niels69",
                    firstname = "Niels",
                    lastname = "Nielsen",
                    email = "hello@gmail.com",
                    passwordHash = passwordHasher.hash("secret12345"),
                    phone = "1234567890",
                    profilePicture = null,
                    fcmToken = null,
                    createdAt = System.currentTimeMillis().toString()
                )
            )
        }
        val createGroupRequest = CreateGroupRequest("group name", "group description")
        val groupService = GroupService(
            groupRepository,
            userRepository,
            activityService,
            messageService,
            expenseRepository,
            debtHistoryRepository
        )
        groupService.createGroup(createGroupRequest, 1)

        val expenseRequest = CreateExpenseRequest(1, 300.0, "test expense", null, Currency.DKK, SplitType.EQUAL)

        expenseService.createExpense(expenseRequest, 1)

        // assert that the balance is the correct amount
        expenseService.getGroupBalances(1, 1).toString().contains("300.0")
    }


    @Test
    suspend fun createSettlement() {

        val settlementRepository = SettlementRepository()
        val userRepository = UserRepository()
        val expenseRepository = ExpenseRepository()
        val groupRepository = GroupRepository()
        val passwordHasher = PasswordHasher()
        val activityRepository = mockk<ActivityRepository>()
        val messageRepository = mockk<MessageRepository>()
        val webSocketManager = mockk<WebSocketManager>()
        val debtHistoryRepository = mockk<DebtHistoryRepository>()

        val activityService = ActivityService(activityRepository, userRepository, groupRepository)
        val messageService = MessageService(messageRepository, groupRepository, userRepository, webSocketManager)
        val settlementService =
            SettlementService(
                settlementRepository, groupRepository, userRepository,
                expenseRepository, activityService, messageService
            )

        //create user
        transaction {
            userRepository.create(
                User(
                    id = 0,
                    username = "Niels69",
                    firstname = "Niels",
                    lastname = "Nielsen",
                    email = "hello@gmail.com",
                    passwordHash = passwordHasher.hash("secret12345"),
                    phone = "1234567890",
                    profilePicture = null,
                    fcmToken = null,
                    createdAt = System.currentTimeMillis().toString()
                )
            )
            userRepository.create(
                User(
                    id = 1,
                    username = "Niels79",
                    firstname = "Niels79",
                    lastname = "Nielsen79",
                    email = "hello79@gmail.com",
                    passwordHash = passwordHasher.hash("secret12345"),
                    phone = "1234567890",
                    profilePicture = null,
                    fcmToken = null,
                    createdAt = System.currentTimeMillis().toString()
                )
            )
        }

        // create group
        val createGroupRequest = CreateGroupRequest("group name", "group description")
        val groupService = GroupService(
            groupRepository,
            userRepository,
            activityService,
            messageService,
            expenseRepository,
            debtHistoryRepository
        )
        groupService.createGroup(createGroupRequest, 1)

        // create settlement request
        val settlementRequest = CreateSettlementRequest(1, 2, 300.0)

        // create settlement
        settlementService.createSettlement(settlementRequest, 1)

        // assert settlement exist on user
        settlementService.getGroupSettlements(1, 1)
    }

    @Test
    suspend fun completeSettlement() {

        val settlementRepository = SettlementRepository()
        val userRepository = UserRepository()
        val expenseRepository = ExpenseRepository()
        val groupRepository = GroupRepository()
        val passwordHasher = PasswordHasher()
        val activityRepository = mockk<ActivityRepository>()
        val messageRepository = mockk<MessageRepository>()
        val webSocketManager = mockk<WebSocketManager>()
        val debtHistoryRepository = mockk<DebtHistoryRepository>()

        val activityService = ActivityService(activityRepository, userRepository, groupRepository)
        val messageService = MessageService(messageRepository, groupRepository, userRepository, webSocketManager)
        val settlementService =
            SettlementService(
                settlementRepository, groupRepository, userRepository,
                expenseRepository, activityService, messageService
            )

        //create user
        transaction {
            userRepository.create(
                User(
                    id = 0,
                    username = "Niels69",
                    firstname = "Niels",
                    lastname = "Nielsen",
                    email = "hello@gmail.com",
                    passwordHash = passwordHasher.hash("secret12345"),
                    phone = "1234567890",
                    profilePicture = null,
                    fcmToken = null,
                    createdAt = System.currentTimeMillis().toString()
                )
            )
        }

        // create group
        val createGroupRequest = CreateGroupRequest("group name", "group description")
        val groupService = GroupService(
            groupRepository,
            userRepository,
            activityService,
            messageService,
            expenseRepository,
            debtHistoryRepository
        )
        groupService.createGroup(createGroupRequest, 1)

        // create settlement request
        val settlementRequest = CreateSettlementRequest(1, 2, 300.0)

        // create settlement
        settlementService.createSettlement(settlementRequest, 1)

        // assert settlement exist on user
        settlementService.getGroupSettlements(1, 1)

        // delete settlement
        settlementService.markSettlementCompleted(1, 1)
    }

    @Test
    suspend fun settlementSuggestions() {

        val userRepository = UserRepository()
        val settlementRepository = SettlementRepository()
        val expenseRepository = ExpenseRepository()
        val groupRepository = GroupRepository()
        val passwordHasher = PasswordHasher()
        val activityRepository = mockk<ActivityRepository>(relaxed = true)
        val messageRepository = mockk<MessageRepository>(relaxed = true)
        val webSocketManager = mockk<WebSocketManager>(relaxed = true)
        val debtHistoryRepository = DebtHistoryRepository()

        val activityService = ActivityService(activityRepository, userRepository, groupRepository)
        val messageService = MessageService(messageRepository, groupRepository, userRepository, webSocketManager)
        val expenseService = ExpenseService(
            expenseRepository,
            groupRepository,
            userRepository,
            settlementRepository,
            activityService,
            messageService
        )
        val settlementService =
            SettlementService(
                settlementRepository, groupRepository, userRepository,
                expenseRepository, activityService, messageService
            )

        // create User
        transaction {
            userRepository.create(
                User(
                    id = 0,
                    username = "Niels69",
                    firstname = "Niels",
                    lastname = "Nielsen",
                    email = "hello@gmail.com",
                    passwordHash = passwordHasher.hash("secret12345"),
                    phone = "1234567890",
                    profilePicture = null,
                    fcmToken = null,
                    createdAt = System.currentTimeMillis().toString()
                )
            )
            userRepository.create(
                User(
                    id = 1,
                    username = "Siels69",
                    firstname = "Siels",
                    lastname = "Sielsen",
                    email = "Shello@gmail.com",
                    passwordHash = passwordHasher.hash("secret12345"),
                    phone = "1234567890",
                    profilePicture = null,
                    fcmToken = null,
                    createdAt = System.currentTimeMillis().toString()
                )
            )
        }

        // create Group
        val createGroupRequest = CreateGroupRequest("group name", "group description")
        val groupService = GroupService(
            groupRepository,
            userRepository,
            activityService,
            messageService,
            expenseRepository,
            debtHistoryRepository
        )
        groupService.createGroup(createGroupRequest, 1)

        // get invite group and make user 2 join it
        val group = transaction { groupRepository.findById(1) }
        val inviteCode = group?.inviteCode
        val joinGroupRequest = JoinGroupRequest(inviteCode!!)
        groupService.joinGroup(joinGroupRequest, 2)

        // create expense request
        val expenseRequest = CreateExpenseRequest(1, 300.0, "test expense", null, Currency.DKK, SplitType.EQUAL)
        val expenseRequest2 = CreateExpenseRequest(1, 600.0, "test expense", null, Currency.DKK, SplitType.EQUAL)

        expenseService.createExpense(expenseRequest, 1)
        expenseService.createExpense(expenseRequest2, 2)

        // get suggestions
        val suggestion = settlementService.getSettlementSuggestions(1, 2)

        // assert you get a suggestion
        suggestion.shouldBeInstanceOf<Result.Success<GroupSettlementSuggestionsDto>>()
    }
}