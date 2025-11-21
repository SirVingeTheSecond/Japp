package routes

import com.japp.database.DatabaseSchema
import com.japp.models.dto.*
import com.japp.module
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File

class AttachmentIntegrationTest : AnnotationSpec() {

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeClass
    fun setupDatabase() {
        Database.connect(
            "jdbc:h2:mem:attachment_test;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "root",
            password = ""
        )
    }

    @Before
    fun createTables() {
        DatabaseSchema.createTables()

        // Create test attachments directory
        File("./test-attachments").mkdirs()
    }

    @After
    fun dropTables() {
        transaction {
            DatabaseSchema.dropTables()
        }

        // Clean up test attachments
        File("./test-attachments").deleteRecursively()
    }

    private fun ApplicationTestBuilder.setupTestConfig() {
        environment {
            config = MapApplicationConfig(
                "jwt.secret" to "test-secret",
                "jwt.issuer" to "test-issuer",
                "jwt.audience" to "test-audience",
                "jwt.realm" to "test-realm",
                "jwt.expirationDays" to "7",
                "database.url" to "jdbc:h2:mem:attachment_test;DB_CLOSE_DELAY=-1",
                "database.driver" to "org.h2.Driver",
                "database.user" to "root",
                "database.password" to "",
                "database.pool.maximumPoolSize" to "5",
                "database.pool.minimumIdle" to "1",
                "database.pool.connectionTimeout" to "30000",
                "database.pool.idleTimeout" to "600000",
                "database.pool.maxLifetime" to "1800000",
                "storage.attachments.basePath" to "./test-attachments"
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
            setBody("""{"name": "Test Group", "description": "Attachment test group"}""")
        }
        val group = json.decodeFromString<GroupDto>(groupResponse.bodyAsText())

        client.post("/api/groups/${group.id}/members") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${auth1.token}")
            setBody("""{"userId": ${auth2.user.id}}""")
        }

        val expenseResponse = client.post("/api/expenses") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${auth1.token}")
            setBody("""
                {
                    "groupId": ${group.id},
                    "amount": 100.0,
                    "description": "Test expense",
                    "splitType": "equal"
                }
            """.trimIndent())
        }
        val expense = json.decodeFromString<ExpenseDto>(expenseResponse.bodyAsText())

        return TestData(
            token1 = auth1.token,
            token2 = auth2.token,
            token3 = auth3.token,
            user1Id = auth1.user.id,
            user2Id = auth2.user.id,
            user3Id = auth3.user.id,
            groupId = group.id,
            expenseId = expense.id
        )
    }

    data class TestData(
        val token1: String,
        val token2: String,
        val token3: String,
        val user1Id: Int,
        val user2Id: Int,
        val user3Id: Int,
        val groupId: Int,
        val expenseId: Int
    )

    @Test
    fun `should upload attachment successfully`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        // small test image
        val testImageBytes = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4.toByte(),
            0x89.toByte(), 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54,
            0x78, 0x9C.toByte(), 0x63, 0x00, 0x01, 0x00, 0x00, 0x05,
            0x00, 0x01, 0x0D, 0x0A, 0x2D, 0xB4.toByte(), 0x00, 0x00,
            0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE.toByte(), 0x42,
            0x60, 0x82.toByte()
        )

        val response = client.post("/api/attachments") {
            header("Authorization", "Bearer ${data.token1}")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("expenseId", data.expenseId.toString())
                        append("file", testImageBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"receipt.png\"")
                        })
                    }
                )
            )
        }

        response.status shouldBe HttpStatusCode.Created
        val attachment = json.decodeFromString<AttachmentDto>(response.bodyAsText())
        attachment.expenseId shouldBe data.expenseId
        attachment.uploadedBy shouldBe data.user1Id
        attachment.fileName shouldBe "receipt.png"
        attachment.mimeType shouldBe "image/png"
        attachment.downloadUrl shouldNotBe null
    }

    @Test
    fun `should fail to upload attachment without authentication`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val testImageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

        val response = client.post("/api/attachments") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("expenseId", data.expenseId.toString())
                        append("file", testImageBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"receipt.png\"")
                        })
                    }
                )
            )
        }

        response.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `should fail to upload attachment when not a group member`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val testImageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

        val response = client.post("/api/attachments") {
            header("Authorization", "Bearer ${data.token3}")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("expenseId", data.expenseId.toString())
                        append("file", testImageBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"receipt.png\"")
                        })
                    }
                )
            )
        }

        response.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `should fail to upload attachment with invalid file type`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val testFileBytes = "This is not an image".toByteArray()

        val response = client.post("/api/attachments") {
            header("Authorization", "Bearer ${data.token1}")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("expenseId", data.expenseId.toString())
                        append("file", testFileBytes, Headers.build {
                            append(HttpHeaders.ContentType, "text/plain")
                            append(HttpHeaders.ContentDisposition, "filename=\"document.txt\"")
                        })
                    }
                )
            )
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should fail to upload attachment exceeding size limit`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        // Create 9MB file (exceeds 8MB limit)
        val largeFileBytes = ByteArray(9 * 1024 * 1024) { 0 }

        val response = client.post("/api/attachments") {
            header("Authorization", "Bearer ${data.token1}")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("expenseId", data.expenseId.toString())
                        append("file", largeFileBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"large.png\"")
                        })
                    }
                )
            )
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `should list attachments for expense`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val testImageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

        // Upload two attachments
        client.post("/api/attachments") {
            header("Authorization", "Bearer ${data.token1}")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("expenseId", data.expenseId.toString())
                        append("file", testImageBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"receipt1.png\"")
                        })
                    }
                )
            )
        }

        client.post("/api/attachments") {
            header("Authorization", "Bearer ${data.token2}")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("expenseId", data.expenseId.toString())
                        append("file", testImageBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=\"receipt2.jpg\"")
                        })
                    }
                )
            )
        }

        val response = client.get("/api/attachments/expense/${data.expenseId}") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response.status shouldBe HttpStatusCode.OK
        val attachmentList = json.decodeFromString<AttachmentListDto>(response.bodyAsText())
        attachmentList.expenseId shouldBe data.expenseId
        attachmentList.attachments shouldHaveSize 2
    }

    @Test
    fun `should download attachment successfully`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val testImageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

        val uploadResponse = client.post("/api/attachments") {
            header("Authorization", "Bearer ${data.token1}")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("expenseId", data.expenseId.toString())
                        append("file", testImageBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"receipt.png\"")
                        })
                    }
                )
            )
        }
        val attachment = json.decodeFromString<AttachmentDto>(uploadResponse.bodyAsText())

        val response = client.get("/api/attachments/${attachment.id}/download") {
            header("Authorization", "Bearer ${data.token2}")
        }

        response.status shouldBe HttpStatusCode.OK
        response.contentType()?.contentType shouldBe "image"
        response.headers[HttpHeaders.ContentDisposition] shouldNotBe null
    }

    @Test
    fun `should fail to download attachment when not a group member`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val testImageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

        val uploadResponse = client.post("/api/attachments") {
            header("Authorization", "Bearer ${data.token1}")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("expenseId", data.expenseId.toString())
                        append("file", testImageBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"receipt.png\"")
                        })
                    }
                )
            )
        }
        val attachment = json.decodeFromString<AttachmentDto>(uploadResponse.bodyAsText())

        val response = client.get("/api/attachments/${attachment.id}/download") {
            header("Authorization", "Bearer ${data.token3}")
        }

        response.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `should delete attachment successfully`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val testImageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

        val uploadResponse = client.post("/api/attachments") {
            header("Authorization", "Bearer ${data.token1}")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("expenseId", data.expenseId.toString())
                        append("file", testImageBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"receipt.png\"")
                        })
                    }
                )
            )
        }
        val attachment = json.decodeFromString<AttachmentDto>(uploadResponse.bodyAsText())

        val response = client.delete("/api/attachments/${attachment.id}") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response.status shouldBe HttpStatusCode.OK

        // Verify file is deleted from disk
        val file = File("./test-attachments/${data.expenseId}/${attachment.id}")
        file.exists() shouldBe false
    }

    @Test
    fun `should fail to delete attachment when not the uploader`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val testImageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

        val uploadResponse = client.post("/api/attachments") {
            header("Authorization", "Bearer ${data.token1}")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("expenseId", data.expenseId.toString())
                        append("file", testImageBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"receipt.png\"")
                        })
                    }
                )
            )
        }
        val attachment = json.decodeFromString<AttachmentDto>(uploadResponse.bodyAsText())

        val response = client.delete("/api/attachments/${attachment.id}") {
            header("Authorization", "Bearer ${data.token2}")
        }

        response.status shouldBe HttpStatusCode.Forbidden
    }

    @Test
    fun `should fail to delete non-existent attachment`() = testApplication {
        setupTestConfig()
        application { module() }
        val data = setupTestData()

        val response = client.delete("/api/attachments/99999") {
            header("Authorization", "Bearer ${data.token1}")
        }

        response.status shouldBe HttpStatusCode.NotFound
    }
}
