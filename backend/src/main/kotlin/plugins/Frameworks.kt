package com.japp.plugins

import com.japp.config.loadJwtConfig
import com.japp.repositories.implementation.ActivityRepository
import com.japp.repositories.implementation.ExpenseRepository
import com.japp.repositories.implementation.GroupRepository
import com.japp.repositories.implementation.SettlementRepository
import com.japp.repositories.implementation.UserRepository
import com.japp.repositories.interfaces.IActivityRepository
import com.japp.repositories.interfaces.IExpenseRepository
import com.japp.repositories.interfaces.IGroupRepository
import com.japp.repositories.interfaces.ISettlementRepository
import com.japp.repositories.interfaces.IUserRepository
import com.japp.security.PasswordHasher
import com.japp.services.*
import io.ktor.server.application.*
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

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

    single { PasswordHasher() }

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
        GroupService(
            groupRepository = get(),
            userRepository = get(),
            activityService = get()
        )
    }

    single {
        ExpenseService(
            expenseRepository = get(),
            groupRepository = get(),
            userRepository = get(),
            activityService = get()
        )
    }

    single {
        SettlementService(
            settlementRepository = get(),
            groupRepository = get(),
            userRepository = get(),
            expenseRepository = get(),
            activityService = get()
        )
    }
}