package com.redis.vectorsearchspringai

import org.springframework.web.bind.annotation.*

@RestController
class SearchController(
    private val movieService: MovieService
) {

    @GetMapping("/search")
    fun search(
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) text: String?,
        @RequestParam(required = false) cast: List<String>?,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) genres: List<String>?,
        @RequestParam(required = false) numberOfNearestNeighbors: Int?
    ): Map<String, Any> {
        return movieService.searchMovies(
            title ?: "",
            text ?: "",
            cast ?: emptyList(),
            year,
            genres ?: emptyList(),
            numberOfNearestNeighbors ?: 10
        )
    }

    @GetMapping("/genres")
    fun getAllGenres(): Map<String, Any> {
        val startTime = System.currentTimeMillis()

        val genres = movieService.getAllGenres()

        val endTime = System.currentTimeMillis()
        val fetchTime = endTime - startTime

        return mapOf(
            "genres" to genres,
            "count" to genres.size,
            "fetchTime" to fetchTime
        )
    }
}