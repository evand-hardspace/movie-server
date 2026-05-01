package com.evandhardspace.movie.server.plugins

import com.evandhardspace.movie.server.domain.service.AuthService
import com.evandhardspace.movie.server.domain.service.FavoriteService
import com.evandhardspace.movie.server.domain.service.MovieService
import com.evandhardspace.movie.server.domain.service.UserService
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*

fun Application.configureDependencyInjection() {
    val supabaseUrl = environment.config.property("supabase.url").getString()
    val supabaseAnonKey = environment.config.property("supabase.anon_key").getString()
    dependencies {
        provide<AuthService> { AuthService(supabaseUrl, supabaseAnonKey) }
        provide<UserService> { UserService() }
        provide<MovieService> { MovieService() }
        provide<FavoriteService> { FavoriteService() }
    }
}
