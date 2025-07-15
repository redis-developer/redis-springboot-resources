package com.redis.vectorsearch;

import com.redis.vectorsearch.service.EmbeddingStatusService;
import com.redis.vectorsearch.service.MovieService;
import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableRedisDocumentRepositories
public class VectorSearchAndAutocompleteApplication {
    public static void main(String[] args) {
        SpringApplication.run(VectorSearchAndAutocompleteApplication.class, args);
    }

    @Bean
    CommandLineRunner loadData(EmbeddingStatusService embeddingStatusService, MovieService movieService) {
        return args -> {
            if (embeddingStatusService.areEmbeddingsReady()) {
                System.out.println("Data already loaded. Skipping data load.");
                return;
            }
            movieService.loadAndSaveMovies("movies.json");
        };
    }
}

