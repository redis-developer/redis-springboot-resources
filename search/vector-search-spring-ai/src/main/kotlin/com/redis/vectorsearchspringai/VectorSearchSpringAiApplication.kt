package com.redis.vectorsearchspringai

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class VectorSearchSpringAiApplication {
    @Bean
    fun loadData(movieService: MovieService): CommandLineRunner {
        return CommandLineRunner {
            val movies = movieService.loadMovies("movies.json")
            val batchSize = 500

            movies.chunked(batchSize).forEach { batch ->
                movieService.storeMovies(batch)
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<VectorSearchSpringAiApplication>(*args)
}
