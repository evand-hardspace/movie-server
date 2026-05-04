package com.evandhardspace.movie.server.domain.service

import com.evandhardspace.movie.server.domain.model.Genre
import com.evandhardspace.movie.server.domain.model.Movie
import java.util.UUID

class MovieFacade(
    private val movieService: MovieService,
    private val favoriteService: FavoriteService,
) {
    fun getMovies(genre: Genre?, userId: UUID?, onlyFavorites: Boolean): List<Movie> {
        val favIds = userId?.let { favoriteService.getFavoritedMovieIds(it) }
        val filterIds = if (onlyFavorites) favIds else null
        val movies = movieService.getMovies(genre, filterIds)
        return favIds?.let { movies.withFavoriteStatus(it) } ?: movies
    }

    fun getMoviesPaged(genre: Genre?, page: Int, pageSize: Int, userId: UUID?, onlyFavorites: Boolean): PagedMovies {
        val favIds = userId?.let { favoriteService.getFavoritedMovieIds(it) }
        val filterIds = if (onlyFavorites) favIds else null
        val paged = movieService.getMoviesPaged(genre, page, pageSize, filterIds)
        return if (favIds != null) paged.copy(items = paged.items.withFavoriteStatus(favIds)) else paged
    }

    fun getMovieById(id: UUID, userId: UUID?): Movie? {
        val movie = movieService.getMovieById(id) ?: return null
        return if (userId != null) movie.copy(isFavorited = favoriteService.isFavorited(userId, id)) else movie
    }

    fun createMovie(
        title: String,
        description: String?,
        genre: Genre,
        rating: Double?,
        photoUrl: String?,
        createdBy: UUID,
    ): Movie = movieService.createMovie(title, description, genre, rating, photoUrl, createdBy)

    fun updateMovie(
        id: UUID,
        title: String,
        description: String?,
        genre: Genre,
        rating: Double?,
        photoUrl: String?,
    ): Movie? = movieService.updateMovie(id, title, description, genre, rating, photoUrl)

    fun deleteMovie(id: UUID): Boolean = movieService.deleteMovie(id)

    private fun List<Movie>.withFavoriteStatus(favIds: Set<UUID>) =
        map { it.copy(isFavorited = UUID.fromString(it.id) in favIds) }
}
