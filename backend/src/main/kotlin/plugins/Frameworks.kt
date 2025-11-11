package com.japp.plugins

import com.japp.repositories.GroupRepository
import com.japp.repositories.IGroupRepository
import com.japp.repositories.IUserRepository
import com.japp.repositories.UserRepository
import com.japp.security.PasswordHasher
import com.japp.services.AuthService
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
}