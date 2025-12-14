package routes.message

import com.japp.database.DatabaseSchema
import com.japp.models.MessageType
import com.japp.models.dto.AuthResponse
import com.japp.models.dto.GroupDto
import com.japp.models.dto.MessageDto
import com.japp.models.dto.MessagePageDto
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

class MessageIntegrationTest : AnnotationSpec() {

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeClass
    fun setupDatabase() {
        Database.connect(
            "jdbc:h2:mem:message_test;DB_CLOSE_DELAY=-1",
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
            setBody(
                """
                {
                    "email": "user1@test.com",
                    "username": "user1",
                    "firstname": "Test",
                    "lastname": "User1",
                    "password": "Password123"
                }
            """.trimIndent()
            )
        }
        val auth1 = json.decodeFromString<AuthResponse>(user1Response.bodyAsText())

        val user2Response = client.post("/api/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "email": "user2@test.com",
                    "username": "user2",
                    "firstname": "Test",
                    "lastname": "User2",
                    "password": "Password123"
                }
            """.trimIndent()
            )
        }
        val auth2 = json.decodeFromString<AuthResponse>(user2Response.bodyAsText())

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

        val groupResponse = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${auth1.token}")
            setBody("""{"name": "Test Group", "description": "Message test group"}""")
        }
        val group = json.decodeFromString<GroupDto>(groupResponse.bodyAsText())

        client.post("/api/groups/${group.id}/members") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${auth1.token}")
            setBody("""{"userId": ${auth2.user.id}}""")
        }

        return TestData(
            token1 = auth1.token,
            token2 = auth2.token,
            token3 = auth3.token,
            user1Id = auth1.user.id,
            user2Id = auth2.user.id,
            user3Id = auth3.user.id,
            groupId = group.id
        )
    }

    data class TestData(
        val token1: String,
        val token2: String,
        val token3: String,
        val user1Id: Int,
        val user2Id: Int,
        val user3Id: Int,
        val groupId: Int
    )

    @Test
    fun `should create message successfully`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val response = client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody(
                """
                {
                    "groupId": ${data.groupId},
                    "content": "Hello, this is a test message!"
                }
            """.trimIndent()
            )
        }

        response.status shouldBe HttpStatusCode.Created
        val message = json.decodeFromString<MessageDto>(response.bodyAsText())
        message.content shouldBe "Hello, this is a test message!"
        message.userId shouldBe data.user1Id
        message.groupId shouldBe data.groupId
        message.messageType shouldBe MessageType.USER
        message.isDeleted shouldBe false
    }

    @Test
    fun `should fail to create message with empty content`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val response = client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody(
                """
                {
                    "groupId": ${data.groupId},
                    "content": ""
                }
            """.trimIndent()
            )
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should fail to create message with content exceeding max length`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val longContent = "a".repeat(2001)

        val response = client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody(
                """
                {
                    "groupId": ${data.groupId},
                    "content": "$longContent"
                }
            """.trimIndent()
            )
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should fail to create message when not a group member`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val response = client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token3}")
            setBody(
                """
                {
                    "groupId": ${data.groupId},
                    "content": "I should not be able to send this"
                }
            """.trimIndent()
            )
        }

        response.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `should retrieve messages for a group`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"groupId": ${data.groupId}, "content": "First message"}""")
        }

        client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token2}")
            setBody("""{"groupId": ${data.groupId}, "content": "Second message"}""")
        }

        client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"groupId": ${data.groupId}, "content": "Third message"}""")
        }

        val response = client.get("/api/messages/group/${data.groupId}") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response.status shouldBe HttpStatusCode.OK
        val messagePage = json.decodeFromString<MessagePageDto>(response.bodyAsText())

        val userMessages = messagePage.messages.filter { it.messageType == MessageType.USER }

        userMessages shouldHaveSize 3
        userMessages[0].content shouldBe "Third message"
        userMessages[1].content shouldBe "Second message"
        userMessages[2].content shouldBe "First message"
    }

    @Test
    fun `should fail to retrieve messages when not a group member`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val response = client.get("/api/messages/group/${data.groupId}") {
            header("Authorization", "Bearer ${data.token3}")
        }

        response.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `should support pagination with limit and cursor`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        repeat(5) { i ->
            client.post("/api/messages") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${data.token1}")
                setBody("""{"groupId": ${data.groupId}, "content": "Message ${i + 1}"}""")
            }
        }

        val response1 = client.get("/api/messages/group/${data.groupId}?limit=2") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response1.status shouldBe HttpStatusCode.OK
        val page1 = json.decodeFromString<MessagePageDto>(response1.bodyAsText())
        page1.messages shouldHaveSize 2
        page1.hasMore shouldBe true
        page1.nextCursor shouldNotBe null

        val response2 = client.get("/api/messages/group/${data.groupId}?limit=2&before=${page1.nextCursor}") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response2.status shouldBe HttpStatusCode.OK
        val page2 = json.decodeFromString<MessagePageDto>(response2.bodyAsText())
        page2.messages shouldHaveSize 2
        page2.messages[0].id shouldNotBe page1.messages[0].id
    }

    @Test
    fun `should mark messages as read`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val createResponse = client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"groupId": ${data.groupId}, "content": "Read me!"}""")
        }
        val message = json.decodeFromString<MessageDto>(createResponse.bodyAsText())

        val response = client.post("/api/messages/read?groupId=${data.groupId}") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token2}")
            setBody("""{"messageIds": [${message.id}]}""")
        }

        response.status shouldBe HttpStatusCode.OK

        val getResponse = client.get("/api/messages/group/${data.groupId}") {
            header("Authorization", "Bearer ${data.token2}")
        }
        val messagePage = json.decodeFromString<MessagePageDto>(getResponse.bodyAsText())
        val readMessage = messagePage.messages.find { it.id == message.id }
        readMessage shouldNotBe null
        readMessage!!.readBy.contains(data.user2Id) shouldBe true
    }

    @Test
    fun `should fail to mark messages as read when not a group member`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val createResponse = client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"groupId": ${data.groupId}, "content": "Test message"}""")
        }
        val message = json.decodeFromString<MessageDto>(createResponse.bodyAsText())

        val response = client.post("/api/messages/read?groupId=${data.groupId}") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token3}")
            setBody("""{"messageIds": [${message.id}]}""")
        }

        response.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `should delete message successfully`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val createResponse = client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"groupId": ${data.groupId}, "content": "Delete me"}""")
        }
        val message = json.decodeFromString<MessageDto>(createResponse.bodyAsText())

        val response = client.delete("/api/messages/${message.id}") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response.status shouldBe HttpStatusCode.OK

        val getResponse = client.get("/api/messages/group/${data.groupId}") {
            header("Authorization", "Bearer ${data.token1}")
        }
        val messagePage = json.decodeFromString<MessagePageDto>(getResponse.bodyAsText())
        val deletedMessage = messagePage.messages.find { it.id == message.id }
        deletedMessage shouldNotBe null
        deletedMessage!!.isDeleted shouldBe true
    }

    @Test
    fun `should fail to delete message when not the author`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val createResponse = client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"groupId": ${data.groupId}, "content": "You cannot delete this"}""")
        }
        val message = json.decodeFromString<MessageDto>(createResponse.bodyAsText())

        val response = client.delete("/api/messages/${message.id}") {
            header("Authorization", "Bearer ${data.token2}")
        }

        response.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `should fail to delete non-existent message`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val response = client.delete("/api/messages/99999") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `should fail without authentication`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val response = client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            setBody("""{"groupId": ${data.groupId}, "content": "Unauthenticated message"}""")
        }

        response.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `should handle multiple users reading the same message`() = testApplication {
        setupTestConfig()

        val data = setupTestData()

        val createResponse = client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"groupId": ${data.groupId}, "content": "Everyone read this!"}""")
        }
        val message = json.decodeFromString<MessageDto>(createResponse.bodyAsText())

        client.post("/api/messages/read?groupId=${data.groupId}") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token2}")
            setBody("""{"messageIds": [${message.id}]}""")
        }

        client.post("/api/messages/read?groupId=${data.groupId}") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"messageIds": [${message.id}]}""")
        }

        val getResponse = client.get("/api/messages/group/${data.groupId}") {
            header("Authorization", "Bearer ${data.token1}")
        }
        val messagePage = json.decodeFromString<MessagePageDto>(getResponse.bodyAsText())
        val readMessage = messagePage.messages.find { it.id == message.id }
        readMessage shouldNotBe null
        readMessage!!.readBy shouldHaveSize 2
        readMessage.readBy.contains(data.user1Id) shouldBe true
        readMessage.readBy.contains(data.user2Id) shouldBe true
    }
}
