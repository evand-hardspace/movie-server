package com.evandhardspace.movie.server.plugins

import com.evandhardspace.movie.server.domain.service.AuthService
import com.evandhardspace.movie.server.domain.service.FavoriteService
import com.evandhardspace.movie.server.domain.service.MovieFacade
import com.evandhardspace.movie.server.domain.service.MovieService
import com.evandhardspace.movie.server.domain.service.UserService
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*

fun Application.configureDependencyInjection() {
    dependencies {
        provide<UserService> { UserService() }
        provide<AuthService> { AuthService(resolve("jwt.secret"), resolve()) }
        provide<MovieService> { MovieService() }
        provide<FavoriteService> { FavoriteService() }
        provide<MovieFacade> { MovieFacade(resolve(), resolve()) }
    }
}

fun Application.configureDIVariables() {
    dependencies {
        provide("jwt.secret") { System.getenv("JWT_SECRET") }
        provide("jwt.audience") { "authenticated" }
    }
}

