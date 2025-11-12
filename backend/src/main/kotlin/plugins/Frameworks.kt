package com.japp.plugins

import com.japp.repositories.ExpenseRepository
import com.japp.repositories.GroupRepository
import com.japp.repositories.IExpenseRepository
import com.japp.repositories.IGroupRepository
import com.japp.repositories.ISettlementRepository
import com.japp.repositories.IUserRepository
import com.japp.repositories.SettlementRepository
import com.japp.repositories.UserRepository
import com.japp.security.PasswordHasher
import com.japp.services.AuthService
import com.japp.services.GroupService
import com.japp.services.ExpenseService
import com.japp.services.SettlementService
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
    single(named("jwtSecret")) {
        application.environment.config.propertyOrNull("jwt.secret")?.getString()
            ?: System.getenv("JWT_SECRET")
            ?: "change-this-secret-in-production"
    }
    single(qualifier = named("jwtIssuer")) {
        application.environment.config.propertyOrNull("jwt.issuer")?.getString()
            ?: "japp-issuer"
    }
    single(qualifier = named("jwtAudience")) {
        application.environment.config.propertyOrNull("jwt.audience")?.getString()
            ?: "japp-audience"
    }

    single<IUserRepository> { UserRepository() }
    single<IGroupRepository> { GroupRepository() }
    single<IExpenseRepository> { ExpenseRepository() }
    single<ISettlementRepository> { SettlementRepository() }

    single { PasswordHasher() }

    single {
        AuthService(
            userRepository = get(),
            passwordHasher = get(),
            jwtSecret = get(named("jwtSecret")),
            jwtIssuer = get(qualifier = named("jwtIssuer")),
            jwtAudience = get(qualifier = named("jwtAudience"))
        )
    }

    single {
        GroupService(
            groupRepository = get(),
            userRepository = get()
        )
    }
    
    single {
        ExpenseService(
            expenseRepository = get(),
            groupRepository = get(),
            userRepository = get()
        )
    }

    single {
        SettlementService(
            settlementRepository = get(),
            groupRepository = get(),
            userRepository = get(),
            expenseRepository = get()
        )
    }
}