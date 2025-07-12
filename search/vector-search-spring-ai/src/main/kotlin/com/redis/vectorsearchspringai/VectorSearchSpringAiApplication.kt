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
            movieService.loadMovies("movies.json").let { movies ->
                movieService.storeMovies(movies)
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<VectorSearchSpringAiApplication>(*args)
}
