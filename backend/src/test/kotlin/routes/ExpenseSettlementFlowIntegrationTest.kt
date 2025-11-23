
package routes

import com.japp.database.DatabaseSchema
import com.japp.models.SettlementStatus
import com.japp.models.SplitType
import com.japp.models.dto.*
import com.japp.module
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExpenseSettlementFlowIntegrationTest : AnnotationSpec() {

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeClass
    fun setupDatabase() {
        Database.connect(
            "jdbc:h2:mem:flow_test;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "root",
            password = ""
        )
    }

    @Before
    fun createTables() {
        DatabaseSchema.createTables()
    }

    @After
    fun dropTables() {
        transaction {
            DatabaseSchema.dropTables()
        }
    }

    private fun ApplicationTestBuilder.setupTestConfig() {
        environment {
            config = MapApplicationConfig(
                "jwt.secret" to "test-secret",
                "jwt.issuer" to "test-issuer",
                "jwt.audience" to "test-audience",
                "jwt.realm" to "test-realm",
                "jwt.expirationDays" to "7",
                "database.url" to "jdbc:h2:mem:flow_test;DB_CLOSE_DELAY=-1",
                "database.driver" to "org.h2.Driver",
                "database.user" to "root",
                "database.password" to "",
                "database.pool.maximumPoolSize" to "5",
                "database.pool.minimumIdle" to "1",
                "database.pool.connectionTimeout" to "30000",
                "database.pool.idleTimeout" to "600000",
                "database.pool.maxLifetime" to "1800000"
            )
        }
    }

    private data class TestData(
        val user1Id: Int,
        val user2Id: Int,
        val user3Id: Int,
        val token1: String,
        val token2: String,
        val token3: String,
        val groupId: Int
    )

    private suspend fun ApplicationTestBuilder.setupTestData(): TestData {
        val user1Response = client.post("/api/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "rolf@skibidi.com",
                    "username": "rofl",
                    "firstname": "Rolf",
                    "lastname": "Skibidi",
                    "password": "password123"
                }
            """.trimIndent())
        }
        val user1Auth = json.decodeFromString<AuthResponse>(user1Response.bodyAsText())

        val user2Response = client.post("/api/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "marius@skibidi.com",
                    "username": "marius",
                    "firstname": "Marius",
                    "lastname": "Skibidi",
                    "password": "password123"
                }
            """.trimIndent())
        }
        val user2Auth = json.decodeFromString<AuthResponse>(user2Response.bodyAsText())

        val user3Response = client.post("/api/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "marcus@skibidi.com",
                    "username": "marcus",
                    "firstname": "Marcus",
                    "lastname": "Skibidi",
                    "password": "password123"
                }
            """.trimIndent())
        }
        val user3Auth = json.decodeFromString<AuthResponse>(user3Response.bodyAsText())

        val groupResponse = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${user1Auth.token}")
            setBody("""
                {
                    "name": "Test Group",
                    "description": "Test Description"
                }
            """.trimIndent())
        }
        val group = json.decodeFromString<GroupDto>(groupResponse.bodyAsText())

        client.post("/api/groups/${group.id}/members") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${user1Auth.token}")
            setBody("""{"userId": ${user2Auth.user.id}}""")
        }

        client.post("/api/groups/${group.id}/members") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${user1Auth.token}")
            setBody("""{"userId": ${user3Auth.user.id}}""")
        }

        return TestData(
            user1Id = user1Auth.user.id,
            user2Id = user2Auth.user.id,
            user3Id = user3Auth.user.id,
            token1 = user1Auth.token,
            token2 = user2Auth.token,
            token3 = user3Auth.token,
            groupId = group.id
        )
    }

    @Test
    fun `should complete full expense and settlement flow with correct balances`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        // Scenario: Rolf pays 300 for dinner, split equally among all three
        val expenseResponse = client.post("/api/expenses") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "amount": 300.0,
                    "description": "Dinner",
                    "currency": "DKK",
                    "category": "FOOD",
                    "splitType": "EQUAL"
                }
            """.trimIndent())
        }
        expenseResponse.status shouldBe HttpStatusCode.Created
        val expense = json.decodeFromString<ExpenseDto>(expenseResponse.bodyAsText())

        expense.amount shouldBe 300.0
        expense.paidBy shouldBe data.user1Id
        expense.splitType shouldBe SplitType.EQUAL

        val balancesResponse = client.get("/api/expenses/group/${data.groupId}/balances") {
            header("Authorization", "Bearer ${data.token1}")
        }
        val balances = json.decodeFromString<GroupBalanceSummaryDto>(balancesResponse.bodyAsText())

        // Rolf paid 300, owes 100 = net +200
        val user1Balance = balances.balances.find { it.userId == data.user1Id }!!
        user1Balance.balance shouldBe 200.0

        // Marius and Marcus each owe 100
        balances.balances.find { it.userId == data.user2Id }!!.balance shouldBe -100.0
        balances.balances.find { it.userId == data.user3Id }!!.balance shouldBe -100.0

        val suggestionsResponse = client.get("/api/settlements/group/${data.groupId}/suggestions") {
            header("Authorization", "Bearer ${data.token2}")
        }
        val suggestions = json.decodeFromString<GroupSettlementSuggestionsDto>(suggestionsResponse.bodyAsText())

        // Should minimize to exactly 2 transactions
        suggestions.suggestions shouldHaveSize 2
        suggestions.suggestions.forEach { suggestion ->
            suggestion.toUserId shouldBe data.user1Id
            suggestion.amount shouldBe 100.0
        }

        // Marius creates settlement to Rolf
        val settlement1Response = client.post("/api/settlements") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token2}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "toUserId": ${data.user1Id},
                    "amount": 100.0
                }
            """.trimIndent())
        }
        settlement1Response.status shouldBe HttpStatusCode.Created
        val settlement1 = json.decodeFromString<SettlementDto>(settlement1Response.bodyAsText())

        settlement1.status shouldBe SettlementStatus.PENDING
        settlement1.completedAt shouldBe null
        settlement1.fromUserId shouldBe data.user2Id
        settlement1.toUserId shouldBe data.user1Id

        // Marcus creates settlement to Rolf
        val settlement2Response = client.post("/api/settlements") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token3}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "toUserId": ${data.user1Id},
                    "amount": 100.0
                }
            """.trimIndent())
        }
        val settlement2 = json.decodeFromString<SettlementDto>(settlement2Response.bodyAsText())

        val pendingResponse = client.get("/api/settlements/group/${data.groupId}?pending=true") {
            header("Authorization", "Bearer ${data.token1}")
        }
        val pending = json.decodeFromString<List<SettlementDto>>(pendingResponse.bodyAsText())
        pending shouldHaveSize 2

        // Rolf completes both settlements
        val complete1Response = client.patch("/api/settlements/${settlement1.id}/complete") {
            header("Authorization", "Bearer ${data.token1}")
        }
        complete1Response.status shouldBe HttpStatusCode.OK
        val completed1 = json.decodeFromString<SettlementDto>(complete1Response.bodyAsText())

        completed1.status shouldBe SettlementStatus.COMPLETED
        completed1.completedAt shouldNotBe null

        client.patch("/api/settlements/${settlement2.id}/complete") {
            header("Authorization", "Bearer ${data.token1}")
        }.status shouldBe HttpStatusCode.OK

        val finalPendingResponse = client.get("/api/settlements/group/${data.groupId}?pending=true") {
            header("Authorization", "Bearer ${data.token1}")
        }
        val finalPending = json.decodeFromString<List<SettlementDto>>(finalPendingResponse.bodyAsText())
        finalPending shouldHaveSize 0

        val allSettlementsResponse = client.get("/api/settlements/group/${data.groupId}") {
            header("Authorization", "Bearer ${data.token1}")
        }
        val allSettlements = json.decodeFromString<List<SettlementDto>>(allSettlementsResponse.bodyAsText())
        allSettlements shouldHaveSize 2
        allSettlements.forEach { it.status shouldBe SettlementStatus.COMPLETED }
    }

    @Test
    fun `should handle custom split with correct balance calculations`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        // Scenario: Rolf pays 500, but Marius owes 300 and Marcus owes 200
        val expenseResponse = client.post("/api/expenses") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "amount": 500.0,
                    "description": "Groceries",
                    "currency": "DKK",
                    "category": "FOOD",
                    "splitType": "CUSTOM",
                    "splits": [
                        {"userId": ${data.user2Id}, "shareAmount": 300.0},
                        {"userId": ${data.user3Id}, "shareAmount": 200.0}
                    ]
                }
            """.trimIndent())
        }
        expenseResponse.status shouldBe HttpStatusCode.Created

        val balancesResponse = client.get("/api/expenses/group/${data.groupId}/balances") {
            header("Authorization", "Bearer ${data.token1}")
        }
        val balances = json.decodeFromString<GroupBalanceSummaryDto>(balancesResponse.bodyAsText())

        // Rolf paid 500, owes 0 = net +500
        balances.balances.find { it.userId == data.user1Id }!!.balance shouldBe 500.0
        balances.balances.find { it.userId == data.user2Id }!!.balance shouldBe -300.0
        balances.balances.find { it.userId == data.user3Id }!!.balance shouldBe -200.0

        val suggestionsResponse = client.get("/api/settlements/group/${data.groupId}/suggestions") {
            header("Authorization", "Bearer ${data.token1}")
        }
        val suggestions = json.decodeFromString<GroupSettlementSuggestionsDto>(suggestionsResponse.bodyAsText())

        suggestions.suggestions shouldHaveSize 2
        val mariusSuggestion = suggestions.suggestions.find { it.fromUserId == data.user2Id }!!
        val marcusSuggestion = suggestions.suggestions.find { it.fromUserId == data.user3Id }!!

        mariusSuggestion.amount shouldBe 300.0
        marcusSuggestion.amount shouldBe 200.0
    }

    @Test
    fun `should prevent user from settling to themselves`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val response = client.post("/api/settlements") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "toUserId": ${data.user1Id},
                    "amount": 100.0
                }
            """.trimIndent())
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should prevent settlement with negative amount`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val response = client.post("/api/settlements") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "toUserId": ${data.user2Id},
                    "amount": -50.0
                }
            """.trimIndent())
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should prevent settlement with zero amount`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val response = client.post("/api/settlements") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "toUserId": ${data.user2Id},
                    "amount": 0.0
                }
            """.trimIndent())
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should prevent unintended user from completing settlement`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val settlementResponse = client.post("/api/settlements") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token2}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "toUserId": ${data.user1Id},
                    "amount": 100.0
                }
            """.trimIndent())
        }
        val settlement = json.decodeFromString<SettlementDto>(settlementResponse.bodyAsText())

        // Marcus tries to complete Marius's settlement to Rolf
        val completeResponse = client.patch("/api/settlements/${settlement.id}/complete") {
            header("Authorization", "Bearer ${data.token3}")
        }

        completeResponse.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `should prevent completing already completed settlement`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val settlementResponse = client.post("/api/settlements") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token2}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "toUserId": ${data.user1Id},
                    "amount": 100.0
                }
            """.trimIndent())
        }
        val settlement = json.decodeFromString<SettlementDto>(settlementResponse.bodyAsText())

        client.patch("/api/settlements/${settlement.id}/complete") {
            header("Authorization", "Bearer ${data.token1}")
        }.status shouldBe HttpStatusCode.OK

        // Try to complete again
        val secondCompleteResponse = client.patch("/api/settlements/${settlement.id}/complete") {
            header("Authorization", "Bearer ${data.token1}")
        }

        secondCompleteResponse.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should prevent expense creation with negative amount`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val response = client.post("/api/expenses") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "amount": -100.0,
                    "description": "Invalid",
                    "currency": "DKK",
                    "category": "FOOD",
                    "splitType": "EQUAL"
                }
            """.trimIndent())
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should prevent custom split with amounts not summing to total`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        // Total is 300 but splits sum to 250
        val response = client.post("/api/expenses") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "amount": 300.0,
                    "description": "Invalid split",
                    "currency": "DKK",
                    "category": "FOOD",
                    "splitType": "CUSTOM",
                    "splits": [
                        {"userId": ${data.user2Id}, "shareAmount": 150.0},
                        {"userId": ${data.user3Id}, "shareAmount": 100.0}
                    ]
                }
            """.trimIndent())
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should minimize settlements optimally for complex scenario`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        // Scenario: Multiple expenses creating complex debt network
        // Rolf pays 300
        client.post("/api/expenses") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "amount": 300.0,
                    "description": "Expense 1",
                    "currency": "DKK",
                    "category": "FOOD",
                    "splitType": "EQUAL"
                }
            """.trimIndent())
        }

        // Marius pays 600
        client.post("/api/expenses") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token2}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "amount": 600.0,
                    "description": "Expense 2",
                    "currency": "DKK",
                    "category": "FOOD",
                    "splitType": "EQUAL"
                }
            """.trimIndent())
        }

        // Marcus pays 300
        client.post("/api/expenses") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token3}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "amount": 300.0,
                    "description": "Expense 3",
                    "currency": "DKK",
                    "category": "FOOD",
                    "splitType": "EQUAL"
                }
            """.trimIndent())
        }

        val balancesResponse = client.get("/api/expenses/group/${data.groupId}/balances") {
            header("Authorization", "Bearer ${data.token1}")
        }
        val balances = json.decodeFromString<GroupBalanceSummaryDto>(balancesResponse.bodyAsText())

        // Each person paid and owes 400 (1200/3), so:
        // Rolf: 300 - 400 = -100
        // Marius: 600 - 400 = +200
        // Marcus: 300 - 400 = -100
        balances.balances.find { it.userId == data.user1Id }!!.balance shouldBe -100.0
        balances.balances.find { it.userId == data.user2Id }!!.balance shouldBe 200.0
        balances.balances.find { it.userId == data.user3Id }!!.balance shouldBe -100.0

        val suggestionsResponse = client.get("/api/settlements/group/${data.groupId}/suggestions") {
            header("Authorization", "Bearer ${data.token1}")
        }
        val suggestions = json.decodeFromString<GroupSettlementSuggestionsDto>(suggestionsResponse.bodyAsText())

        // Should minimize to 2 transactions (Rolf→Marius 100, Marcus→Marius 100)
        suggestions.suggestions shouldHaveSize 2
        suggestions.suggestions.forEach { suggestion ->
            suggestion.toUserId shouldBe data.user2Id
            suggestion.amount shouldBe 100.0
        }
    }

    @Test
    fun `should prevent settlement to non-group member`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val nonMemberResponse = client.post("/api/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "outsider@test.com",
                    "username": "outsider",
                    "firstname": "Outside",
                    "lastname": "User",
                    "password": "password123"
                }
            """.trimIndent())
        }
        val nonMemberAuth = json.decodeFromString<AuthResponse>(nonMemberResponse.bodyAsText())

        val response = client.post("/api/settlements") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "toUserId": ${nonMemberAuth.user.id},
                    "amount": 100.0
                }
            """.trimIndent())
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should handle percentage custom split correctly`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        // Scenario: Rolf pays 1000, Marius owes 60%, Marcus owes 40%
        val expenseResponse = client.post("/api/expenses") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "amount": 1000.0,
                    "description": "Rent",
                    "currency": "DKK",
                    "category": "OTHER",
                    "splitType": "CUSTOM",
                    "splits": [
                        {"userId": ${data.user2Id}, "sharePercentage": 60.0},
                        {"userId": ${data.user3Id}, "sharePercentage": 40.0}
                    ]
                }
            """.trimIndent())
        }
        expenseResponse.status shouldBe HttpStatusCode.Created

        val balancesResponse = client.get("/api/expenses/group/${data.groupId}/balances") {
            header("Authorization", "Bearer ${data.token1}")
        }
        val balances = json.decodeFromString<GroupBalanceSummaryDto>(balancesResponse.bodyAsText())

        balances.balances.find { it.userId == data.user1Id }!!.balance shouldBe 1000.0
        balances.balances.find { it.userId == data.user2Id }!!.balance shouldBe -600.0
        balances.balances.find { it.userId == data.user3Id }!!.balance shouldBe -400.0
    }
}