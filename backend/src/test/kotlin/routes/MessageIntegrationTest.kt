package routes

import com.japp.database.DatabaseSchema
import com.japp.models.MessageType
import com.japp.models.WebSocketMessageType
import com.japp.models.dto.*
import com.japp.module
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
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
            config = MapApplicationConfig(
                "jwt.secret" to "test-secret",
                "jwt.issuer" to "test-issuer",
                "jwt.audience" to "test-audience",
                "jwt.realm" to "test-realm",
                "jwt.expirationDays" to "7",
                "database.url" to "jdbc:h2:mem:message_test;DB_CLOSE_DELAY=-1",
                "database.driver" to "org.h2.Driver",
                "database.user" to "root",
                "database.password" to "",
                "database.pool.maximumPoolSize" to "5",
                "database.pool.minimumIdle" to "1",
                "database.pool.connectionTimeout" to "30000",
                "database.pool.idleTimeout" to "600000",
                "database.pool.maxLifetime" to "1800000",
                "websocket.heartbeatIntervalInSeconds" to "0" // Scuffed solution until it is included in the environment config
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

        val user3Response = client.post("/api/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "user3@test.com",
                    "username": "user3",
                    "firstname": "Test",
                    "lastname": "User3",
                    "password": "Password123"
                }
            """.trimIndent())
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
        application { module() }
        val data = setupTestData()

        val response = client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "content": "Hello, this is a test message!"
                }
            """.trimIndent())
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
        application { module() }
        val data = setupTestData()

        val response = client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "content": ""
                }
            """.trimIndent())
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should fail to create message with content exceeding max length`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val longContent = "a".repeat(2001)

        val response = client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "content": "$longContent"
                }
            """.trimIndent())
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should fail to create message when not a group member`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val response = client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token3}")
            setBody("""
                {
                    "groupId": ${data.groupId},
                    "content": "I shouldn't be able to send this"
                }
            """.trimIndent())
        }

        response.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `should retrieve messages for a group`() = testApplication {
        setupTestConfig()
        application { module() }
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
        application { module() }
        val data = setupTestData()

        val response = client.get("/api/messages/group/${data.groupId}") {
            header("Authorization", "Bearer ${data.token3}")
        }

        response.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `should support pagination with limit and cursor`() = testApplication {
        setupTestConfig()
        application { module() }
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
        application { module() }
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
        application { module() }
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
        application { module() }
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
        application { module() }
        val data = setupTestData()

        val createResponse = client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${data.token1}")
            setBody("""{"groupId": ${data.groupId}, "content": "You can't delete this"}""")
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
        application { module() }
        val data = setupTestData()

        val response = client.delete("/api/messages/99999") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `should fail without authentication`() = testApplication {
        setupTestConfig()
        application { module() }
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
        application { module() }
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

    @Test
    fun `should connect to WebSocket and receive connected message`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/ws/chat", request = {
            header("Authorization", "Bearer ${data.token1}")
        }) {
            val frame = incoming.receive() as Frame.Text
            val message = json.decodeFromString<WebSocketMessage>(frame.readText())

            message.type shouldBe WebSocketMessageType.CONNECTED
            message.userId shouldBe data.user1Id
        }
    }

    @Test
    fun `should send message via WebSocket and receive MESSAGE_SENT acknowledgment`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/ws/chat", request = {
            header("Authorization", "Bearer ${data.token1}")
        }) {
            // Receive CONNECTED message
            val connectedFrame = incoming.receive() as Frame.Text
            val connectedMsg = json.decodeFromString<WebSocketMessage>(connectedFrame.readText())
            connectedMsg.type shouldBe WebSocketMessageType.CONNECTED

            // Subscribe to group
            val subscribeMessage = WebSocketMessage(
                type = WebSocketMessageType.SUBSCRIBE,
                groupId = data.groupId
            )
            send(Frame.Text(json.encodeToString(WebSocketMessage.serializer(), subscribeMessage)))

            // Receive SUBSCRIBED confirmation
            val subscribedFrame = incoming.receive() as Frame.Text
            val subscribedMsg = json.decodeFromString<WebSocketMessage>(subscribedFrame.readText())
            subscribedMsg.type shouldBe WebSocketMessageType.SUBSCRIBED

            // Send NEW_MESSAGE via WebSocket
            val newMessage = WebSocketMessage(
                type = WebSocketMessageType.NEW_MESSAGE,
                groupId = data.groupId,
                message = MessageDto(
                    id = 0,
                    groupId = data.groupId,
                    userId = null,
                    userName = null,
                    content = "WebSocket message test!",
                    messageType = MessageType.USER,
                    createdAt = "",
                    editedAt = null,
                    isDeleted = false
                )
            )
            send(Frame.Text(json.encodeToString(WebSocketMessage.serializer(), newMessage)))

            // Should receive MESSAGE_SENT acknowledgment
            val ackFrame = incoming.receive() as Frame.Text
            val ackMsg = json.decodeFromString<WebSocketMessage>(ackFrame.readText())
            ackMsg.type shouldBe WebSocketMessageType.MESSAGE_SENT
            ackMsg.message shouldNotBe null
            ackMsg.message!!.content shouldBe "WebSocket message test!"
            ackMsg.error shouldBe null
        }
    }

    @Test
    fun `should return ERROR when sending message with missing groupId`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/ws/chat", request = {
            header("Authorization", "Bearer ${data.token1}")
        }) {
            incoming.receive() // CONNECTED

            // Send message without groupId
            val invalidMessage = WebSocketMessage(
                type = WebSocketMessageType.NEW_MESSAGE,
                groupId = null,
                message = MessageDto(
                    id = 0,
                    groupId = 0,
                    userId = null,
                    userName = null,
                    content = "This should fail",
                    messageType = MessageType.USER,
                    createdAt = "",
                    editedAt = null,
                    isDeleted = false
                )
            )
            send(Frame.Text(json.encodeToString(WebSocketMessage.serializer(), invalidMessage)))

            // Should receive ERROR
            val errorFrame = incoming.receive() as Frame.Text
            val errorMsg = json.decodeFromString<WebSocketMessage>(errorFrame.readText())
            errorMsg.type shouldBe WebSocketMessageType.ERROR
            errorMsg.error shouldBe "groupId and content are required"
        }
    }

    @Test
    fun `should return ERROR when sending message to group user is not member of`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/ws/chat", request = {
            header("Authorization", "Bearer ${data.token3}")
        }) {
            incoming.receive() // CONNECTED

            // User3 tries to send message to group they're not in
            val unauthorizedMessage = WebSocketMessage(
                type = WebSocketMessageType.NEW_MESSAGE,
                groupId = data.groupId,
                message = MessageDto(
                    id = 0,
                    groupId = data.groupId,
                    userId = null,
                    userName = null,
                    content = "I shouldn't be able to send this",
                    messageType = MessageType.USER,
                    createdAt = "",
                    editedAt = null,
                    isDeleted = false
                )
            )
            send(Frame.Text(json.encodeToString(WebSocketMessage.serializer(), unauthorizedMessage)))

            // Should receive ERROR
            val errorFrame = incoming.receive() as Frame.Text
            val errorMsg = json.decodeFromString<WebSocketMessage>(errorFrame.readText())
            errorMsg.type shouldBe WebSocketMessageType.ERROR
            errorMsg.error shouldBe "Not a member of this group"
        }
    }

    @Test
    fun `should fail to subscribe to group when not a member`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/ws/chat", request = {
            header("Authorization", "Bearer ${data.token3}")
        }) {
            incoming.receive()

            send(Frame.Text(json.encodeToString(
                WebSocketMessage.serializer(),
                WebSocketMessage(
                    type = WebSocketMessageType.SUBSCRIBE,
                    groupId = data.groupId
                )
            )))

            val frame = incoming.receive() as Frame.Text
            val message = json.decodeFromString<WebSocketMessage>(frame.readText())

            message.type shouldBe WebSocketMessageType.ERROR
            message.groupId shouldBe data.groupId
            message.error shouldBe "Not a member of this group"
        }
    }

    @Test
    fun `should unsubscribe from group successfully`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/ws/chat", request = {
            header("Authorization", "Bearer ${data.token1}")
        }) {
            incoming.receive()

            // Subscribe
            send(Frame.Text(json.encodeToString(
                WebSocketMessage.serializer(),
                WebSocketMessage(
                    type = WebSocketMessageType.SUBSCRIBE,
                    groupId = data.groupId
                )
            )))
            incoming.receive()

            // Unsubscribe
            send(Frame.Text(json.encodeToString(
                WebSocketMessage.serializer(),
                WebSocketMessage(
                    type = WebSocketMessageType.UNSUBSCRIBE,
                    groupId = data.groupId
                )
            )))

            val frame = incoming.receive() as Frame.Text
            val message = json.decodeFromString<WebSocketMessage>(frame.readText())

            message.type shouldBe WebSocketMessageType.UNSUBSCRIBED
            message.groupId shouldBe data.groupId
        }
    }

    @Test
    fun `should receive new message via WebSocket when subscribed`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/ws/chat", request = {
            header("Authorization", "Bearer ${data.token1}")
        }) {
            incoming.receive() // connected

            // Subscribe to group
            send(Frame.Text(json.encodeToString(
                WebSocketMessage.serializer(),
                WebSocketMessage(
                    type = WebSocketMessageType.SUBSCRIBE,
                    groupId = data.groupId
                )
            )))
            incoming.receive() // subscribed

            // Create message via HTTP in parallel
            val messageJob = async {
                delay(100)
                this@testApplication.client.post("/api/messages") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer ${data.token2}")
                    setBody("""{"groupId": ${data.groupId}, "content": "WebSocket test!"}""")
                }
            }

            // Should receive the new message via WebSocket
            val frame = incoming.receive() as Frame.Text
            val wsMessage = json.decodeFromString<WebSocketMessage>(frame.readText())

            wsMessage.type shouldBe WebSocketMessageType.NEW_MESSAGE
            wsMessage.message shouldNotBe null
            wsMessage.message!!.content shouldBe "WebSocket test!"

            messageJob.await()
        }
    }
}