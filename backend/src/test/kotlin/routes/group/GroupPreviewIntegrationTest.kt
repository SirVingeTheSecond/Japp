package routes.group

import com.japp.database.DatabaseSchema
import com.japp.models.dto.AuthResponse
import com.japp.models.dto.GroupDto
import com.japp.models.dto.GroupPreviewDto
import com.japp.module
import io.kotest.core.spec.style.AnnotationSpec
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

class GroupPreviewIntegrationTest : AnnotationSpec() {

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeClass
    fun setupDatabase() {
        Database.connect(
            "jdbc:h2:mem:group_preview_test;DB_CLOSE_DELAY=-1",
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

    @Test
    fun `should preview group with valid invite code without authentication`() = testApplication {
        setupTestConfig()

        val userResponse = client.post("/api/auth/signup") {
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
        val auth = json.decodeFromString<AuthResponse>(userResponse.bodyAsText())

        val groupResponse = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${auth.token}")
            setBody("""{"name": "Test Group", "description": "A test group for preview"}""")
        }
        val group = json.decodeFromString<GroupDto>(groupResponse.bodyAsText())

        val response = client.get("/api/groups/preview/${group.inviteCode}")

        response.status shouldBe HttpStatusCode.OK
        val preview = json.decodeFromString<GroupPreviewDto>(response.bodyAsText())
        preview.name shouldBe "Test Group"
        preview.description shouldBe "A test group for preview"
        preview.memberCount shouldBe 1
        preview.createdAt shouldNotBe null
    }

    @Test
    fun `should fail with invalid invite code`() = testApplication {
        setupTestConfig()


        val response = client.get("/api/groups/preview/INVALD")

        response.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `should fail with wrong invite code length`() = testApplication {
        setupTestConfig()


        val response = client.get("/api/groups/preview/ABC")

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should preview group without Authorization header`() = testApplication {
        setupTestConfig()


        val userResponse = client.post("/api/auth/signup") {
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
        val auth = json.decodeFromString<AuthResponse>(userResponse.bodyAsText())

        val groupResponse = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${auth.token}")
            setBody("""{"name": "Public Preview Group"}""")
        }
        val group = json.decodeFromString<GroupDto>(groupResponse.bodyAsText())

        val response = client.get("/api/groups/preview/${group.inviteCode}")

        response.status shouldBe HttpStatusCode.OK
        val preview = json.decodeFromString<GroupPreviewDto>(response.bodyAsText())
        preview.name shouldBe "Public Preview Group"
    }
}