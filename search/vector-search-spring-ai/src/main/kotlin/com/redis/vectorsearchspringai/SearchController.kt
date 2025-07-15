package com.redis.vectorsearchspringai

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class SearchController(
    private val movieService: MovieService,
    private val embeddingStatusService: EmbeddingStatusService
) {

    @GetMapping("/actors")
    fun getAllActors(): ResponseEntity<Any> {
        if (!embeddingStatusService.areEmbeddingsReady()) {
            val embeddedDocs = embeddingStatusService.getTotalDocNum()
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to "Embeddings are still being created ($embeddedDocs of 10000 already created). This operation takes around two minutes to complete. Please try again later."))
        }

        // This is a placeholder since we don't have the actual implementation
        // In a real implementation, this would call a service method to get all actors
        return ResponseEntity.ok(mapOf(
            "actors" to emptyList<String>(),
            "count" to 0,
            "fetchTime" to 0
        ))
    }

    @GetMapping("/search")
    fun search(
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) text: String?,
        @RequestParam(required = false) cast: List<String>?,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) genres: List<String>?,
        @RequestParam(required = false) numberOfNearestNeighbors: Int?
    ): ResponseEntity<Any> {
        if (!embeddingStatusService.areEmbeddingsReady()) {
            val embeddedDocs = embeddingStatusService.getTotalDocNum()
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to "Embeddings are still being created ($embeddedDocs of 10000 already created). This operation takes around two minutes to complete. Please try again later."))
        }

        return ResponseEntity.ok(movieService.searchMovies(
            title ?: "",
            text ?: "",
            cast ?: emptyList(),
            year,
            genres ?: emptyList(),
            numberOfNearestNeighbors ?: 10
        ))
    }

    @GetMapping("/genres")
    fun getAllGenres(): ResponseEntity<Any> {
        if (!embeddingStatusService.areEmbeddingsReady()) {
            val embeddedDocs = embeddingStatusService.getTotalDocNum()
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to "Embeddings are still being created ($embeddedDocs of 10000 already created). This operation takes around two minutes to complete. Please try again later."))
        }

        val startTime = System.currentTimeMillis()

        val genres = movieService.getAllGenres()

        val endTime = System.currentTimeMillis()
        val fetchTime = endTime - startTime

        return ResponseEntity.ok(mapOf(
            "genres" to genres,
            "count" to genres.size,
            "fetchTime" to fetchTime
        ))
    }
}
