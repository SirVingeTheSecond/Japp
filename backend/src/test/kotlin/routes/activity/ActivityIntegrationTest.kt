package routes.activity

import com.japp.database.DatabaseSchema
import com.japp.models.ActivityType
import com.japp.models.dto.ActivityDto
import com.japp.models.dto.AuthResponse
import com.japp.models.dto.GroupActivitiesDto
import com.japp.models.dto.GroupDto
import com.japp.module
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ActivityIntegrationTest : AnnotationSpec() {

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeClass
    fun setupDatabase() {
        Database.Companion.connect(
            "jdbc:h2:mem:activity_test;DB_CLOSE_DELAY=-1",
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
            config = ApplicationConfig("application.yaml")
        }
    }

    private suspend fun ApplicationTestBuilder.setupTestData(): TestData {
        val user1Response = client.post("/api/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "user1@test.com",
                    "username": "user1",
                    "firstname": "Test",
                    "lastname": "User1",
                    "password": "Password123"
                }
            """.trimIndent())
        }
        val auth1 = json.decodeFromString<AuthResponse>(user1Response.bodyAsText())

        val user2Response = client.post("/api/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "user2@test.com",
                    "username": "user2",
                    "firstname": "Test",
                    "lastname": "User2",
                    "password": "Password123"
                }
            """.trimIndent())
        }
        val auth2 = json.decodeFromString<AuthResponse>(user2Response.bodyAsText())

        val groupResponse = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${auth1.token}")
            setBody("""{"name": "Test Group", "description": "Activity test group"}""")
        }
        val group = json.decodeFromString<GroupDto>(groupResponse.bodyAsText())

        client.post("/api/groups/${group.id}/members") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${auth1.token}")
            setBody("""{"userId": ${auth2.user.id}}""")
        }

        return TestData(auth1.token, auth2.token, auth1.user.id, auth2.user.id, group.id)
    }

    data class TestData(
        val token1: String,
        val token2: String,
        val user1Id: Int,
        val user2Id: Int,
        val groupId: Int
    )

    @Test
    fun `should get user activities for all groups`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        // setupTestData already created activities: GROUP_CREATED, MEMBER_JOINED
        client.post("/api/expenses") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody(
                """
            {
                "groupId": ${data.groupId},
                "amount": 100.0,
                "description": "Test expense",
                "splitType": "equal"
            }
        """.trimIndent()
            )
        }

        val response = client.get("/api/activities") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response.status shouldBe HttpStatusCode.Companion.OK
        val activities = json.decodeFromString<List<ActivityDto>>(response.bodyAsText())

        activities shouldHaveSize 3
        activities.map { it.actionType } shouldContain ActivityType.GROUP_CREATED
        activities.map { it.actionType } shouldContain ActivityType.EXPENSE_CREATED
    }

    @Test
    fun `should get group activities successfully`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        // Create an expense to generate more activities
        client.post("/api/expenses") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody(
                """
            {
                "groupId": ${data.groupId},
                "amount": 150.0,
                "description": "Group dinner",
                "splitType": "equal"
            }
        """.trimIndent()
            )
        }

        val response = client.get("/api/activities/group/${data.groupId}") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response.status shouldBe HttpStatusCode.Companion.OK
        val groupActivities = json.decodeFromString<GroupActivitiesDto>(response.bodyAsText())

        groupActivities.groupId shouldBe data.groupId
        groupActivities.groupName shouldBe "Test Group"
        groupActivities.activities shouldHaveSize 3
        groupActivities.activities.map { it.actionType } shouldContain ActivityType.GROUP_CREATED
        groupActivities.activities.map { it.actionType } shouldContain ActivityType.MEMBER_JOINED
        groupActivities.activities.map { it.actionType } shouldContain ActivityType.EXPENSE_CREATED
    }

    @Test
    fun `should fail to get group activities when not a member`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        // Create a third user who is NOT in the group
        val user3Response = client.post("/api/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "email": "user3@test.com",
                "username": "user3",
                "firstname": "Test",
                "lastname": "User3",
                "password": "Password123"
            }
        """.trimIndent()
            )
        }
        val auth3 = json.decodeFromString<AuthResponse>(user3Response.bodyAsText())

        val response = client.get("/api/activities/group/${data.groupId}") {
            header("Authorization", "Bearer ${auth3.token}")
        }

        response.status shouldBe HttpStatusCode.Companion.Forbidden
    }

    @Test
    fun `should fail with invalid group ID`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val response = client.get("/api/activities/group/99999") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response.status shouldBe HttpStatusCode.Companion.Forbidden
    }

    @Test
    fun `should fail without authentication`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val response = client.get("/api/activities/group/${data.groupId}")

        response.status shouldBe HttpStatusCode.Companion.Unauthorized
    }

    @Test
    fun `should respect limit parameter for user activities`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        // Create multiple expenses to generate more activities
        repeat(5) { i ->
            client.post("/api/expenses") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${data.token1}")
                setBody(
                    """
                {
                    "groupId": ${data.groupId},
                    "amount": ${(i + 1) * 10.0},
                    "description": "Expense $i",
                    "splitType": "equal"
                }
            """.trimIndent()
                )
            }
        }

        // Total activities: 2 (setup) + 5 (expenses)
        // Request only 3
        val response = client.get("/api/activities?limit=3") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response.status shouldBe HttpStatusCode.Companion.OK
        val activities = json.decodeFromString<List<ActivityDto>>(response.bodyAsText())

        activities shouldHaveSize 3
    }

    @Test
    fun `should respect limit parameter for group activities`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        // Create multiple expenses
        repeat(5) { i ->
            client.post("/api/expenses") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${data.token1}")
                setBody(
                    """
                {
                    "groupId": ${data.groupId},
                    "amount": ${(i + 1) * 10.0},
                    "description": "Expense $i",
                    "splitType": "equal"
                }
            """.trimIndent()
                )
            }
        }

        // Total activities: 2 (setup) + 5 (expenses)
        // Request only 2
        val response = client.get("/api/activities/group/${data.groupId}?limit=2") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response.status shouldBe HttpStatusCode.Companion.OK
        val groupActivities = json.decodeFromString<GroupActivitiesDto>(response.bodyAsText())

        groupActivities.activities shouldHaveSize 2
    }
}