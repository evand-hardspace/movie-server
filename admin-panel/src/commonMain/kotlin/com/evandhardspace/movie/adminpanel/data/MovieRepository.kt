package com.evandhardspace.movie.adminpanel.data

import com.evandhardspace.movie.adminpanel.domain.model.Genre
import com.evandhardspace.movie.adminpanel.domain.model.Movie
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class MovieDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val genre: Genre,
    val rating: Double? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("is_favorited") val isFavorited: Boolean = false,
)

@Serializable
private data class PagedMoviesDto(
    val items: List<MovieDto>,
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    val total: Long,
    @SerialName("total_pages") val totalPages: Int,
)

data class PagedMovies(
    val items: List<Movie>,
    val page: Int,
    val pageSize: Int,
    val total: Long,
    val totalPages: Int,
)

@Serializable
data class MovieRequest(
    val title: String,
    val description: String? = null,
    val genre: Genre,
    val rating: Double? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
)

private fun MovieDto.toDomain() = Movie(
    id = id,
    title = title,
    description = description,
    genre = genre,
    rating = rating,
    photoUrl = photoUrl,
    createdAt = createdAt,
    isFavorited = isFavorited,
)

class MovieRepository(private val apiClient: ApiClient) {
    suspend fun getMovies(): ApiResult<List<Movie>> =
        apiClient.get<List<MovieDto>>("/movies").map { list -> list.map { it.toDomain() } }

    suspend fun getMoviesPaged(page: Int = 1, pageSize: Int = 20): ApiResult<PagedMovies> =
        apiClient.get<PagedMoviesDto>("/movies?page=$page&page_size=$pageSize")
            .map { dto ->
                PagedMovies(
                    items = dto.items.map { it.toDomain() },
                    page = dto.page,
                    pageSize = dto.pageSize,
                    total = dto.total,
                    totalPages = dto.totalPages,
                )
            }

    suspend fun createMovie(request: MovieRequest): ApiResult<Movie> =
        apiClient.post<MovieDto>("/movies", request).map { it.toDomain() }

    suspend fun updateMovie(id: String, request: MovieRequest): ApiResult<Movie> =
        apiClient.put<MovieDto>("/movies/$id", request).map { it.toDomain() }

    suspend fun deleteMovie(id: String): ApiResult<Unit> =
        apiClient.delete("/movies/$id")
}
