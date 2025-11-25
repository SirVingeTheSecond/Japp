package routes.group

import com.japp.database.DatabaseSchema
import com.japp.models.dto.AuthResponse
import com.japp.models.dto.GroupDto
import com.japp.models.dto.GroupMemberDto
import com.japp.module
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class GroupMemberIntegrationTest : AnnotationSpec() {

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeClass
    fun setupDatabase() {
        Database.connect(
            "jdbc:h2:mem:joinedat_test;DB_CLOSE_DELAY=-1",
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
                "database.url" to "jdbc:h2:mem:joinedat_test;DB_CLOSE_DELAY=-1",
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

        return TestData(auth1, auth2)
    }

    private data class TestData(
        val auth1: AuthResponse,
        val auth2: AuthResponse
    )

    @Test
    fun `group creator should have joinedAt timestamp`() = testApplication {
        application { module() }
        setupTestConfig()
        val testData = setupTestData()

        val createGroupResponse = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            bearerAuth(testData.auth1.token)
            setBody("""
                {
                    "name": "Test Group",
                    "description": "Test group for joinedAt"
                }
            """.trimIndent())
        }

        createGroupResponse.status shouldBe HttpStatusCode.Created
        val group = json.decodeFromString<GroupDto>(createGroupResponse.bodyAsText())

        val membersResponse = client.get("/api/groups/${group.id}/members") {
            bearerAuth(testData.auth1.token)
        }
        membersResponse.status shouldBe HttpStatusCode.OK
        val members = json.decodeFromString<List<GroupMemberDto>>(membersResponse.bodyAsText())

        members shouldHaveSize 1
        members[0].userId shouldBe testData.auth1.user.id
        members[0].joinedAt.shouldNotBeEmpty()
        members[0].isOwner shouldBe true
    }

    @Test
    fun `member who joins should have joinedAt timestamp`() = testApplication {
        application { module() }
        setupTestConfig()
        val testData = setupTestData()

        val createGroupResponse = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            bearerAuth(testData.auth1.token)
            setBody("""
                {
                    "name": "Test Group",
                    "description": "Test group for joinedAt"
                }
            """.trimIndent())
        }
        val group = json.decodeFromString<GroupDto>(createGroupResponse.bodyAsText())

        val joinResponse = client.post("/api/groups/join") {
            contentType(ContentType.Application.Json)
            bearerAuth(testData.auth2.token)
            setBody("""
                {
                    "inviteCode": "${group.inviteCode}"
                }
            """.trimIndent())
        }
        joinResponse.status shouldBe HttpStatusCode.OK

        val membersResponse = client.get("/api/groups/${group.id}/members") {
            bearerAuth(testData.auth1.token)
        }
        val members = json.decodeFromString<List<GroupMemberDto>>(membersResponse.bodyAsText())

        members shouldHaveSize 2
        val user2Member = members.find { it.userId == testData.auth2.user.id }
        user2Member shouldNotBe null
        user2Member!!.joinedAt.shouldNotBeEmpty()
        user2Member.isOwner shouldBe false
    }

    @Test
    fun `multiple members should have different joinedAt timestamps`() = testApplication {
        application { module() }
        setupTestConfig()
        val testData = setupTestData()

        val createGroupResponse = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            bearerAuth(testData.auth1.token)
            setBody("""
                {
                    "name": "Test Group",
                    "description": "Test group for joinedAt"
                }
            """.trimIndent())
        }
        val group = json.decodeFromString<GroupDto>(createGroupResponse.bodyAsText())

        delay(10)

        client.post("/api/groups/join") {
            contentType(ContentType.Application.Json)
            bearerAuth(testData.auth2.token)
            setBody("""
                {
                    "inviteCode": "${group.inviteCode}"
                }
            """.trimIndent())
        }

        val membersResponse = client.get("/api/groups/${group.id}/members") {
            bearerAuth(testData.auth1.token)
        }
        val members = json.decodeFromString<List<GroupMemberDto>>(membersResponse.bodyAsText())

        members shouldHaveSize 2
        val user1Member = members.find { it.userId == testData.auth1.user.id }!!
        val user2Member = members.find { it.userId == testData.auth2.user.id }!!

        user1Member.joinedAt.shouldNotBeEmpty()
        user2Member.joinedAt.shouldNotBeEmpty()

        user1Member.joinedAt.toLong() shouldBeLessThan user2Member.joinedAt.toLong()
    }

    @Test
    fun `joinedAt should be consistent across multiple API calls`() = testApplication {
        application { module() }
        setupTestConfig()
        val testData = setupTestData()

        val createGroupResponse = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            bearerAuth(testData.auth1.token)
            setBody("""
                {
                    "name": "Test Group",
                    "description": "Test group for joinedAt"
                }
            """.trimIndent())
        }
        val group = json.decodeFromString<GroupDto>(createGroupResponse.bodyAsText())

        val membersResponse1 = client.get("/api/groups/${group.id}/members") {
            bearerAuth(testData.auth1.token)
        }
        val members1 = json.decodeFromString<List<GroupMemberDto>>(membersResponse1.bodyAsText())

        val membersResponse2 = client.get("/api/groups/${group.id}/members") {
            bearerAuth(testData.auth1.token)
        }
        val members2 = json.decodeFromString<List<GroupMemberDto>>(membersResponse2.bodyAsText())

        members1[0].joinedAt shouldBe members2[0].joinedAt
    }

    @Test
    fun `owner should successfully remove a member`() = testApplication {
        application { module() }
        setupTestConfig()
        val testData = setupTestData()

        val createGroupResponse = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            bearerAuth(testData.auth1.token)
            setBody("""
                {
                    "name": "Test Group",
                    "description": "Test group for member removal"
                }
            """.trimIndent())
        }
        val group = json.decodeFromString<GroupDto>(createGroupResponse.bodyAsText())

        client.post("/api/groups/${group.id}/members") {
            contentType(ContentType.Application.Json)
            bearerAuth(testData.auth1.token)
            setBody("""{"userId": ${testData.auth2.user.id}}""")
        }

        val removeResponse = client.delete("/api/groups/${group.id}/members/${testData.auth2.user.id}") {
            bearerAuth(testData.auth1.token)
        }

        removeResponse.status shouldBe HttpStatusCode.OK

        val membersResponse = client.get("/api/groups/${group.id}/members") {
            bearerAuth(testData.auth1.token)
        }
        val members = json.decodeFromString<List<GroupMemberDto>>(membersResponse.bodyAsText())
        members shouldHaveSize 1
        members.none { it.userId == testData.auth2.user.id } shouldBe true
    }

    @Test
    fun `non-owner should fail to remove a member`() = testApplication {
        application { module() }
        setupTestConfig()
        val testData = setupTestData()

        val createGroupResponse = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            bearerAuth(testData.auth1.token)
            setBody("""
                {
                    "name": "Test Group",
                    "description": "Test group"
                }
            """.trimIndent())
        }
        val group = json.decodeFromString<GroupDto>(createGroupResponse.bodyAsText())

        client.post("/api/groups/${group.id}/members") {
            contentType(ContentType.Application.Json)
            bearerAuth(testData.auth1.token)
            setBody("""{"userId": ${testData.auth2.user.id}}""")
        }

        val removeResponse = client.delete("/api/groups/${group.id}/members/${testData.auth1.user.id}") {
            bearerAuth(testData.auth2.token)
        }

        removeResponse.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `owner should fail to remove themselves`() = testApplication {
        application { module() }
        setupTestConfig()
        val testData = setupTestData()

        val createGroupResponse = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            bearerAuth(testData.auth1.token)
            setBody("""
                {
                    "name": "Test Group",
                    "description": "Test group"
                }
            """.trimIndent())
        }
        val group = json.decodeFromString<GroupDto>(createGroupResponse.bodyAsText())

        val removeResponse = client.delete("/api/groups/${group.id}/members/${testData.auth1.user.id}") {
            bearerAuth(testData.auth1.token)
        }

        removeResponse.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should fail to remove non-existent member`() = testApplication {
        application { module() }
        setupTestConfig()
        val testData = setupTestData()

        val createGroupResponse = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            bearerAuth(testData.auth1.token)
            setBody("""
                {
                    "name": "Test Group",
                    "description": "Test group"
                }
            """.trimIndent())
        }
        val group = json.decodeFromString<GroupDto>(createGroupResponse.bodyAsText())

        val removeResponse = client.delete("/api/groups/${group.id}/members/99999") {
            bearerAuth(testData.auth1.token)
        }

        removeResponse.status shouldBe HttpStatusCode.NotFound
    }
}
