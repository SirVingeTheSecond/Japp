package com.japp.plugins

import com.japp.config.loadJwtConfig
import com.japp.config.loadStorageConfig
import com.japp.repositories.implementations.ActivityRepository
import com.japp.repositories.implementations.AttachmentRepository
import com.japp.repositories.implementations.DebtHistoryRepository
import com.japp.repositories.implementations.ExpenseRepository
import com.japp.repositories.implementations.GroupRepository
import com.japp.repositories.implementations.MessageRepository
import com.japp.repositories.implementations.SettlementRepository
import com.japp.repositories.implementations.UserRepository
import com.japp.services.interfaces.IActivityRepository
import com.japp.services.interfaces.IAttachmentRepository
import com.japp.services.interfaces.IDebtHistoryRepository
import com.japp.services.interfaces.IExpenseRepository
import com.japp.services.interfaces.IGroupRepository
import com.japp.services.interfaces.IMessageRepository
import com.japp.services.interfaces.ISettlementRepository
import com.japp.services.interfaces.IUserRepository
import com.japp.security.PasswordHasher
import com.japp.services.*
import com.japp.websocket.WebSocketManager
import io.ktor.server.application.*
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import kotlin.time.Duration.Companion.seconds

fun Application.configureFrameworks() {
    install(Koin) {
        slf4jLogger()
        modules(appModule(this@configureFrameworks))
    }
}

fun appModule(application: Application) = module {
    val jwtConfig = application.loadJwtConfig()

    single { jwtConfig }
    single(named("jwtSecret")) { jwtConfig.secret }
    single(named("jwtIssuer")) { jwtConfig.issuer }
    single(named("jwtAudience")) { jwtConfig.audience }

    single<IUserRepository> { UserRepository() }
    single<IGroupRepository> { GroupRepository() }
    single<IExpenseRepository> { ExpenseRepository() }
    single<ISettlementRepository> { SettlementRepository() }
    single<IActivityRepository> { ActivityRepository() }
    single<IMessageRepository> { MessageRepository() }
    single<IAttachmentRepository> { AttachmentRepository() }

    single { PasswordHasher() }

    single {
        val heartbeatSeconds = application.environment.config
            .propertyOrNull("websocket.heartbeatIntervalInSeconds")
            ?.getString()
            ?.toLongOrNull()
            ?: 20L

        println("DEBUG: heartbeatSeconds = $heartbeatSeconds")

        val heartbeatInterval = if (heartbeatSeconds > 0) {
            heartbeatSeconds.seconds
        } else {
            null
        }

        println("DEBUG: heartbeatInterval = $heartbeatInterval")

        WebSocketManager(heartbeatInterval = heartbeatInterval)
    }

    single {
        AuthService(
            userRepository = get(),
            passwordHasher = get(),
            jwtSecret = get(named("jwtSecret")),
            jwtIssuer = get(named("jwtIssuer")),
            jwtAudience = get(named("jwtAudience"))
        )
    }

    single {
        UserService(
            userRepository = get()
        )
    }

    single {
        ActivityService(
            activityRepository = get(),
            groupRepository = get(),
            userRepository = get()
        )
    }

    single {
        MessageService(
            messageRepository = get(),
            groupRepository = get(),
            userRepository = get(),
            webSocketManager = get()
        )
    }

    single {
        ChatWebSocketService(
            messageService = get(),
            webSocketManager = get(),
            userRepository = get()
        )
    }

    single {
        GroupService(
            groupRepository = get(),
            userRepository = get(),
            activityService = get(),
            messageService = get(),
            expenseRepository = get(),
            debtHistoryRepository = get()
        )
    }

    single {
        ExpenseService(
            expenseRepository = get(),
            groupRepository = get(),
            userRepository = get(),
            settlementRepository = get(),
            activityService = get(),
            messageService = get()
        )
    }

    single {
        SettlementService(
            settlementRepository = get(),
            groupRepository = get(),
            userRepository = get(),
            expenseRepository = get(),
            activityService = get(),
            messageService = get()
        )
    }

    single {
        val storageConfig = application.loadStorageConfig()
        AttachmentService(
            attachmentRepository = get(),
            expenseRepository = get(),
            groupRepository = get(),
            userRepository = get(),
            activityService = get(),
            storageBasePath = storageConfig.attachmentsBasePath
        )
    }

    single<IDebtHistoryRepository> { DebtHistoryRepository() }
}