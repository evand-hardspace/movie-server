package com.evandhardspace.movie.server

import com.evandhardspace.movie.server.plugins.*
import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

suspend fun Application.module() {
    configureDependencyInjection()
    configureDIVariables()
    configureHttp()
    configureSerialization()
    configureDatabase()
    configureSecurity()
    configureRouting()
    configureSeed()
}