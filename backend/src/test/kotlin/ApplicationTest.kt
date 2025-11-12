import com.japp.database.DatabaseSchema
import com.japp.models.dto.AuthResponse
import com.japp.models.dto.SignupRequest
import com.japp.repositories.UserRepository
import com.japp.security.PasswordHasher
import com.japp.services.AuthService
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.Database
import io.mockk.*

class ApplicationTest : AnnotationSpec() {
    @BeforeClass
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", user = "root", password = "")
    }

    @AfterClass
    fun teardown() {
    }

    @Before
    fun create() {
        DatabaseSchema.createTables()
    }

    @After
    fun drop(){
        DatabaseSchema.dropTables()
    }

    @Test
    suspend fun createUserTest() {
        val userRepository = mockk<UserRepository>()
        val userTest = SignupRequest(
            email = "hello@gmail.com",
            password = "secret12345",
            name = "Niels",
            phone = "1234567890"
        )
        userRepository.create()
    }
}