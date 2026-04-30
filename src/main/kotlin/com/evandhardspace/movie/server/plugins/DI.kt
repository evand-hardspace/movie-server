package com.evandhardspace.movie.server.plugins

import com.evandhardspace.movie.server.domain.service.MovieService
import com.evandhardspace.movie.server.domain.service.UserService
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*

fun Application.configureDependencyInjection() {
    dependencies {
        provide<UserService> { UserService() }
        provide<MovieService> { MovieService() }
    }
}
