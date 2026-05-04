package com.evandhardspace.movie.server.plugins

import com.evandhardspace.movie.server.domain.model.Genre
import com.evandhardspace.movie.server.domain.model.UserRole
import com.evandhardspace.movie.server.domain.service.AuthService
import com.evandhardspace.movie.server.domain.service.MovieService
import com.evandhardspace.movie.server.domain.service.UserService
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import java.util.UUID

private data class MovieSeed(
    val title: String,
    val description: String,
    val genre: Genre,
    val rating: Double,
    val photoUrl: String?,
)

private val SEED_MOVIES = listOf(
    MovieSeed("Inception", "A thief who steals corporate secrets through dream-sharing technology.", Genre.THRILLER, 8.8, null),
    MovieSeed("The Dark Knight", "Batman faces the Joker, a criminal mastermind who plunges Gotham into chaos.", Genre.ACTION, 9.0, null),
    MovieSeed("Interstellar", "A team of explorers travel through a wormhole in space to ensure humanity's survival.", Genre.SCI_FI, 8.6, null),
    MovieSeed("The Shawshank Redemption", "Two imprisoned men bond over years, finding solace and redemption.", Genre.DRAMA, 9.3, null),
    MovieSeed("The Grand Budapest Hotel", "A concierge and his protégé become embroiled in the theft of a priceless painting.", Genre.COMEDY, 8.1, null),
    MovieSeed("Get Out", "A Black man uncovers a disturbing secret when he visits his girlfriend's family estate.", Genre.HORROR, 7.7, null),
    MovieSeed("La La Land", "A jazz pianist and an aspiring actress fall in love while chasing their dreams in LA.", Genre.ROMANCE, 8.0, null),
    MovieSeed("Free Solo", "Alex Honnold attempts to free solo climb Yosemite's El Capitan.", Genre.DOCUMENTARY, 8.2, null),
    MovieSeed("Spider-Man: Into the Spider-Verse", "Teen Miles Morales becomes Spider-Man and crosses paths with alternate-universe spider-heroes.", Genre.ANIMATION, 8.4, null),
    MovieSeed("Parasite", "Greed and class discrimination threaten the symbiotic relationship between two families.", Genre.THRILLER, 8.5, null),
)

fun Application.configureSeed() {
    val adminEmail = System.getenv("SEED_ADMIN_EMAIL") ?: return
    val adminPassword = System.getenv("SEED_ADMIN_PASSWORD") ?: return

    val userService: UserService by dependencies
    val authService: AuthService by dependencies
    val movieService: MovieService by dependencies

    var adminId: UUID? = null

    if (!userService.hasUsers()) {
        val result = authService.signUp(adminEmail, adminPassword)
        userService.updateRole(result.userId, UserRole.SUPER_ADMIN)
        adminId = result.userId
        log.info("Seeded super_admin: $adminEmail")
    }

    if (!movieService.hasMovies()) {
        val createdBy = adminId ?: userService.findByEmail(adminEmail)?.id ?: return
        SEED_MOVIES.forEach { seed ->
            movieService.createMovie(seed.title, seed.description, seed.genre, seed.rating, seed.photoUrl, createdBy)
        }
        log.info("Seeded ${SEED_MOVIES.size} movies")
    }
}
