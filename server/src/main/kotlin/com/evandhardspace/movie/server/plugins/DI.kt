package com.evandhardspace.movie.server.plugins

import com.evandhardspace.movie.server.domain.service.AuthService
import com.evandhardspace.movie.server.domain.service.FavoriteService
import com.evandhardspace.movie.server.domain.service.MovieService
import com.evandhardspace.movie.server.domain.service.UserService
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*

fun Application.configureDependencyInjection() {
    val jwtSecret = environment.config.property("jwt.secret").getString()
    dependencies {
        provide<UserService> { UserService() }
        provide<AuthService> { AuthService(jwtSecret, resolve()) }
        provide<MovieService> { MovieService() }
        provide<FavoriteService> { FavoriteService() }
    }
}
