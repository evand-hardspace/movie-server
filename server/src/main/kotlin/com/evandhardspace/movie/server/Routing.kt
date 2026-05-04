package com.evandhardspace.movie.server

import com.evandhardspace.movie.server.domain.service.AuthService
import com.evandhardspace.movie.server.domain.service.FavoriteService
import com.evandhardspace.movie.server.domain.service.MovieFacade
import com.evandhardspace.movie.server.domain.service.UserService
import com.evandhardspace.movie.server.routes.authRoutes
import com.evandhardspace.movie.server.routes.favoriteRoutes
import com.evandhardspace.movie.server.routes.movieRoutes
import com.evandhardspace.movie.server.routes.userRoutes
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val authService: AuthService by dependencies
    val userService: UserService by dependencies
    val movieFacade: MovieFacade by dependencies
    val favoriteService: FavoriteService by dependencies
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        authRoutes(authService)
        movieRoutes(movieFacade, userService)
        favoriteRoutes(favoriteService)
        userRoutes(userService)
    }
}
