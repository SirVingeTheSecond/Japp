package routes

import com.japp.module
import com.japp.database.DatabaseSchema
import com.japp.models.dto.*
import com.japp.models.SettlementStatus
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SettlementIntegrationTest : AnnotationSpec() {

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeClass
    fun setupDatabase() {
        Database.connect(
            "jdbc:h2:mem:settlement_test;DB_CLOSE_DELAY=-1",
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
                "database.url" to "jdbc:h2:mem:settlement_test;DB_CLOSE_DELAY=-1",
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
            setBody("""{"name": "Test Group", "description": "Settlement test group"}""")
        }
        val group = json.decodeFromString<GroupDto>(groupResponse.bodyAsText())

        client.post("/api/groups/${group.id}/members") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${auth1.token}")
            setBody("""{"userId": ${auth2.user.id}}""")
        }

        return TestData(auth1.token, auth2.token, auth1.user.id, auth2.user.id, group.id)
    }

    data class TestData(val token1: String, val token2: String, val user1Id: Int, val user2Id: Int, val groupId: Int)

    @Test
    fun `should create settlement successfully`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val response = client.post("/api/settlements") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"groupId": ${data.groupId}, "toUserId": ${data.user2Id}, "amount": 150.0}""")
        }

        response.status shouldBe HttpStatusCode.Created
        val settlement = json.decodeFromString<SettlementDto>(response.bodyAsText())
        settlement.amount shouldBe 150.0
        settlement.status shouldBe SettlementStatus.PENDING
    }

    @Test
    fun `should fail when settling to yourself`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val response = client.post("/api/settlements") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"groupId": ${data.groupId}, "toUserId": ${data.user1Id}, "amount": 50.0}""")
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should fail with negative amount`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val response = client.post("/api/settlements") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"groupId": ${data.groupId}, "toUserId": ${data.user2Id}, "amount": -50.0}""")
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should fail without authentication`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val response = client.post("/api/settlements") {
            contentType(ContentType.Application.Json)
            setBody("""{"groupId": ${data.groupId}, "toUserId": ${data.user2Id}, "amount": 50.0}""")
        }

        response.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `should mark settlement as completed`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val createResponse = client.post("/api/settlements") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"groupId": ${data.groupId}, "toUserId": ${data.user2Id}, "amount": 100.0}""")
        }
        val settlement = json.decodeFromString<SettlementDto>(createResponse.bodyAsText())

        val response = client.patch("/api/settlements/${settlement.id}/complete") {
            header("Authorization", "Bearer ${data.token2}")
        }

        response.status shouldBe HttpStatusCode.OK
        val completed = json.decodeFromString<SettlementDto>(response.bodyAsText())
        completed.status shouldBe SettlementStatus.COMPLETED
    }

    @Test
    fun `should fail when non-recipient tries to complete`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val createResponse = client.post("/api/settlements") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"groupId": ${data.groupId}, "toUserId": ${data.user2Id}, "amount": 100.0}""")
        }
        val settlement = json.decodeFromString<SettlementDto>(createResponse.bodyAsText())

        val response = client.patch("/api/settlements/${settlement.id}/complete") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response.status shouldBe HttpStatusCode.Forbidden
    }
}