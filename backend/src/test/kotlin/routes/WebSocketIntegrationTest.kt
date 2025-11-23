package routes

import com.japp.database.DatabaseSchema
import com.japp.models.WebSocketMessageType
import com.japp.models.dto.*
import com.japp.module
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class WebSocketIntegrationTest : AnnotationSpec() {

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeClass
    fun setupDatabase() {
        Database.connect(
            "jdbc:h2:mem:typing_test;DB_CLOSE_DELAY=-1",
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
                "database.url" to "jdbc:h2:mem:typing_test;DB_CLOSE_DELAY=-1",
                "database.driver" to "org.h2.Driver",
                "database.user" to "root",
                "database.password" to "",
                "database.pool.maximumPoolSize" to "5",
                "database.pool.minimumIdle" to "1",
                "database.pool.connectionTimeout" to "30000",
                "database.pool.idleTimeout" to "600000",
                "database.pool.maxLifetime" to "1800000",
                "websocket.heartbeatIntervalInSeconds" to "0"
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
            setBody("""{"name": "Test Group", "description": "Typing test group"}""")
        }
        val group = json.decodeFromString<GroupDto>(groupResponse.bodyAsText())

        client.post("/api/groups/${group.id}/members") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${auth1.token}")
            setBody("""{"userId": ${auth2.user.id}}""")
        }

        client.post("/api/groups/${group.id}/members") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${auth1.token}")
            setBody("""{"userId": ${auth3.user.id}}""")
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
    fun `should broadcast TYPING_START to subscribed group members except sender`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val wsClient = createClient {
            install(WebSockets)
        }

        coroutineScope {
            val user1Session = async {
                wsClient.webSocket("/api/ws/chat", request = {
                    header("Authorization", "Bearer ${data.token1}")
                }) {
                    // Note: Pattern that applies for all the tests:
                    incoming.receive() // connected
                    send(Frame.Text(json.encodeToString(
                        WebSocketMessage.serializer(),
                        WebSocketMessage(type = WebSocketMessageType.SUBSCRIBE, groupId = data.groupId)
                    )))
                    incoming.receive() // subscribed

                    val frame = incoming.receive() as Frame.Text
                    val message = json.decodeFromString<WebSocketMessage>(frame.readText())

                    message.type shouldBe WebSocketMessageType.TYPING_START
                    message.groupId shouldBe data.groupId
                    message.userId shouldBe data.user2Id
                    message.username shouldBe "user2"
                }
            }

            val user2Session = async {
                wsClient.webSocket("/api/ws/chat", request = {
                    header("Authorization", "Bearer ${data.token2}")
                }) {
                    incoming.receive()
                    send(Frame.Text(json.encodeToString(
                        WebSocketMessage.serializer(),
                        WebSocketMessage(type = WebSocketMessageType.SUBSCRIBE, groupId = data.groupId)
                    )))
                    incoming.receive()

                    delay(100)

                    send(Frame.Text(json.encodeToString(
                        WebSocketMessage.serializer(),
                        WebSocketMessage(
                            type = WebSocketMessageType.TYPING_START,
                            groupId = data.groupId
                        )
                    )))

                    delay(200)
                }
            }

            user2Session.await()
            user1Session.await()
        }
    }

    @Test
    fun `should broadcast TYPING_STOP to subscribed group members except sender`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val wsClient = createClient {
            install(WebSockets)
        }

        coroutineScope {
            val user1Session = async {
                wsClient.webSocket("/api/ws/chat", request = {
                    header("Authorization", "Bearer ${data.token1}")
                }) {
                    incoming.receive()
                    send(Frame.Text(json.encodeToString(
                        WebSocketMessage.serializer(),
                        WebSocketMessage(type = WebSocketMessageType.SUBSCRIBE, groupId = data.groupId)
                    )))
                    incoming.receive()

                    val frame = incoming.receive() as Frame.Text
                    val message = json.decodeFromString<WebSocketMessage>(frame.readText())

                    message.type shouldBe WebSocketMessageType.TYPING_STOP
                    message.groupId shouldBe data.groupId
                    message.userId shouldBe data.user2Id
                    message.username shouldBe "user2"
                }
            }

            val user2Session = async {
                wsClient.webSocket("/api/ws/chat", request = {
                    header("Authorization", "Bearer ${data.token2}")
                }) {
                    incoming.receive()
                    send(Frame.Text(json.encodeToString(
                        WebSocketMessage.serializer(),
                        WebSocketMessage(type = WebSocketMessageType.SUBSCRIBE, groupId = data.groupId)
                    )))
                    incoming.receive()

                    delay(100)

                    send(Frame.Text(json.encodeToString(
                        WebSocketMessage.serializer(),
                        WebSocketMessage(
                            type = WebSocketMessageType.TYPING_STOP,
                            groupId = data.groupId
                        )
                    )))

                    delay(200)
                }
            }

            user2Session.await()
            user1Session.await()
        }
    }

    @Test
    fun `should not receive own typing indicator`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val wsClient = createClient {
            install(WebSockets)
        }

        wsClient.webSocket("/api/ws/chat", request = {
            header("Authorization", "Bearer ${data.token1}")
        }) {
            incoming.receive()
            send(Frame.Text(json.encodeToString(
                WebSocketMessage.serializer(),
                WebSocketMessage(type = WebSocketMessageType.SUBSCRIBE, groupId = data.groupId)
            )))
            incoming.receive()

            send(Frame.Text(json.encodeToString(
                WebSocketMessage.serializer(),
                WebSocketMessage(
                    type = WebSocketMessageType.TYPING_START,
                    groupId = data.groupId
                )
            )))

            send(Frame.Text(json.encodeToString(
                WebSocketMessage.serializer(),
                WebSocketMessage(type = WebSocketMessageType.PING)
            )))

            val frame = incoming.receive() as Frame.Text
            val message = json.decodeFromString<WebSocketMessage>(frame.readText())

            message.type shouldBe WebSocketMessageType.PONG
        }
    }

    @Test
    fun `should not broadcast typing indicator when not subscribed to group`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val wsClient = createClient {
            install(WebSockets)
        }

        coroutineScope {
            val user1Session = async {
                wsClient.webSocket("/api/ws/chat", request = {
                    header("Authorization", "Bearer ${data.token1}")
                }) {
                    incoming.receive()
                    send(Frame.Text(json.encodeToString(
                        WebSocketMessage.serializer(),
                        WebSocketMessage(type = WebSocketMessageType.SUBSCRIBE, groupId = data.groupId)
                    )))
                    incoming.receive()

                    delay(300)

                    send(Frame.Text(json.encodeToString(
                        WebSocketMessage.serializer(),
                        WebSocketMessage(type = WebSocketMessageType.PING)
                    )))

                    val frame = incoming.receive() as Frame.Text
                    val message = json.decodeFromString<WebSocketMessage>(frame.readText())

                    message.type shouldBe WebSocketMessageType.PONG
                }
            }

            val user2Session = async {
                wsClient.webSocket("/api/ws/chat", request = {
                    header("Authorization", "Bearer ${data.token2}")
                }) {
                    incoming.receive()

                    delay(100)

                    send(Frame.Text(json.encodeToString(
                        WebSocketMessage.serializer(),
                        WebSocketMessage(
                            type = WebSocketMessageType.TYPING_START,
                            groupId = data.groupId
                        )
                    )))

                    delay(200)
                }
            }

            user2Session.await()
            user1Session.await()
        }
    }

    @Test
    fun `should ignore typing indicator without groupId`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val wsClient = createClient {
            install(WebSockets)
        }

        wsClient.webSocket("/api/ws/chat", request = {
            header("Authorization", "Bearer ${data.token1}")
        }) {
            incoming.receive()
            send(Frame.Text(json.encodeToString(
                WebSocketMessage.serializer(),
                WebSocketMessage(type = WebSocketMessageType.SUBSCRIBE, groupId = data.groupId)
            )))
            incoming.receive()

            send(Frame.Text(json.encodeToString(
                WebSocketMessage.serializer(),
                WebSocketMessage(type = WebSocketMessageType.TYPING_START)
            )))

            send(Frame.Text(json.encodeToString(
                WebSocketMessage.serializer(),
                WebSocketMessage(type = WebSocketMessageType.PING)
            )))

            val frame = incoming.receive() as Frame.Text
            val message = json.decodeFromString<WebSocketMessage>(frame.readText())

            message.type shouldBe WebSocketMessageType.PONG
        }
    }

    @Test
    fun `should broadcast typing indicator to all subscribed members except sender`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val wsClient = createClient {
            install(WebSockets)
        }

        coroutineScope {
            val user1Session = async {
                wsClient.webSocket("/api/ws/chat", request = {
                    header("Authorization", "Bearer ${data.token1}")
                }) {
                    incoming.receive()
                    send(Frame.Text(json.encodeToString(
                        WebSocketMessage.serializer(),
                        WebSocketMessage(type = WebSocketMessageType.SUBSCRIBE, groupId = data.groupId)
                    )))
                    incoming.receive()

                    val frame = incoming.receive() as Frame.Text
                    val message = json.decodeFromString<WebSocketMessage>(frame.readText())

                    message.type shouldBe WebSocketMessageType.TYPING_START
                    message.userId shouldBe data.user2Id
                    message.username shouldBe "user2"
                }
            }

            val user3Session = async {
                wsClient.webSocket("/api/ws/chat", request = {
                    header("Authorization", "Bearer ${data.token3}")
                }) {
                    incoming.receive()
                    send(Frame.Text(json.encodeToString(
                        WebSocketMessage.serializer(),
                        WebSocketMessage(type = WebSocketMessageType.SUBSCRIBE, groupId = data.groupId)
                    )))
                    incoming.receive()

                    val frame = incoming.receive() as Frame.Text
                    val message = json.decodeFromString<WebSocketMessage>(frame.readText())

                    message.type shouldBe WebSocketMessageType.TYPING_START
                    message.userId shouldBe data.user2Id
                    message.username shouldBe "user2"
                }
            }

            val user2Session = async {
                wsClient.webSocket("/api/ws/chat", request = {
                    header("Authorization", "Bearer ${data.token2}")
                }) {
                    incoming.receive()
                    send(Frame.Text(json.encodeToString(
                        WebSocketMessage.serializer(),
                        WebSocketMessage(type = WebSocketMessageType.SUBSCRIBE, groupId = data.groupId)
                    )))
                    incoming.receive()

                    delay(100)

                    send(Frame.Text(json.encodeToString(
                        WebSocketMessage.serializer(),
                        WebSocketMessage(
                            type = WebSocketMessageType.TYPING_START,
                            groupId = data.groupId
                        )
                    )))

                    delay(200)
                }
            }

            user2Session.await()
            user1Session.await()
            user3Session.await()
        }
    }
}
