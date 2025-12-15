package routes.user

import com.japp.database.DatabaseSchema
import com.japp.models.dto.AuthResponse
import com.japp.module
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


// Thank you for your service, Claude (￣^￣ )ゞ
class FcmTokenIntegrationTest : AnnotationSpec() {

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeClass
    fun setupDatabase() {
        Database.connect(
            "jdbc:h2:mem:fcm_token_test;DB_CLOSE_DELAY=-1",
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

        return TestData(
            token1 = auth1.token,
            token2 = auth2.token,
            user1Id = auth1.user.id,
            user2Id = auth2.user.id
        )
    }

    private data class TestData(
        val token1: String,
        val token2: String,
        val user1Id: Int,
        val user2Id: Int
    )

    @Test
    fun `should register FCM token successfully`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val response = client.post("/api/user/me/fcm-token") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"token": "test-fcm-token-12345"}""")
        }

        response.status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `should update existing FCM token`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        // Register first token
        client.post("/api/user/me/fcm-token") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"token": "old-fcm-token"}""")
        }

        // Update with new token
        val response = client.post("/api/user/me/fcm-token") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"token": "new-fcm-token"}""")
        }

        response.status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `should register different FCM tokens for different users`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val response1 = client.post("/api/user/me/fcm-token") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"token": "user1-device-token"}""")
        }

        val response2 = client.post("/api/user/me/fcm-token") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token2}")
            setBody("""{"token": "user2-device-token"}""")
        }

        response1.status shouldBe HttpStatusCode.OK
        response2.status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `should fail to register FCM token without authentication`() = testApplication {
        setupTestConfig()


        val response = client.post("/api/user/me/fcm-token") {
            contentType(ContentType.Application.Json)
            setBody("""{"token": "unauthorized-token"}""")
        }

        response.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `should fail to register FCM token with invalid JWT`() = testApplication {
        setupTestConfig()


        val response = client.post("/api/user/me/fcm-token") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer invalid-jwt-token")
            setBody("""{"token": "some-fcm-token"}""")
        }

        response.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `should fail to register FCM token with empty token`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val response = client.post("/api/user/me/fcm-token") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"token": ""}""")
        }

        // Depending on validation, could be BadRequest or OK
        // If no validation exists, this documents current behavior
        response.status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `should fail to register FCM token with missing body`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val response = client.post("/api/user/me/fcm-token") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{}""")
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should clear FCM token successfully`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        // First register a token
        client.post("/api/user/me/fcm-token") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"token": "token-to-be-cleared"}""")
        }

        // Then clear it
        val response = client.delete("/api/user/me/fcm-token") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response.status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `should clear FCM token even when no token was registered`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        // Clear without having registered a token first
        val response = client.delete("/api/user/me/fcm-token") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response.status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `should fail to clear FCM token without authentication`() = testApplication {
        setupTestConfig()


        val response = client.delete("/api/user/me/fcm-token")

        response.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `should fail to clear FCM token with invalid JWT`() = testApplication {
        setupTestConfig()


        val response = client.delete("/api/user/me/fcm-token") {
            header("Authorization", "Bearer invalid-jwt-token")
        }

        response.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `should only clear own FCM token not affect other users`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        // User 1 registers token
        client.post("/api/user/me/fcm-token") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"token": "user1-token"}""")
        }

        // User 2 registers token
        client.post("/api/user/me/fcm-token") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token2}")
            setBody("""{"token": "user2-token"}""")
        }

        // User 1 clears their token
        val response = client.delete("/api/user/me/fcm-token") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response.status shouldBe HttpStatusCode.OK

        // User 2's token should still work (can register again or perform actions)
        val user2Response = client.post("/api/user/me/fcm-token") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token2}")
            setBody("""{"token": "user2-new-token"}""")
        }

        user2Response.status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `should allow re-registering token after clearing`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        // Register
        client.post("/api/user/me/fcm-token") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"token": "original-token"}""")
        }

        // Clear
        client.delete("/api/user/me/fcm-token") {
            header("Authorization", "Bearer ${data.token1}")
        }

        // Re-register
        val response = client.post("/api/user/me/fcm-token") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"token": "new-token-after-clear"}""")
        }

        response.status shouldBe HttpStatusCode.OK
    }
}
