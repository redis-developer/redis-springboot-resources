package com.redis.fulltextsearchandautocomplete;

import com.redis.fulltextsearchandautocomplete.service.MovieService;
import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableRedisDocumentRepositories
public class FullTextSearchAndAutocompleteApplication {
    public static void main(String[] args) {
        SpringApplication.run(FullTextSearchAndAutocompleteApplication.class, args);
    }

    @Bean
    CommandLineRunner loadData(MovieService movieService) {
        return args -> {
            if (movieService.isDataLoaded()) {
                System.out.println("Data already loaded. Skipping data load.");
                return;
            }
            movieService.loadAndSaveMovies("movies.json");
        };
    }
}

