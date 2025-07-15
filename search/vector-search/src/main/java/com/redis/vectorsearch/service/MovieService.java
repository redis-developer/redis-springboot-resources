package com.redis.vectorsearch.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.vectorsearch.domain.Movie;
import com.redis.vectorsearch.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class MovieService {

    private static final Logger log = LoggerFactory.getLogger(MovieService.class);

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final MovieRepository movieRepository;

    public MovieService(ObjectMapper objectMapper, ResourceLoader resourceLoader, MovieRepository movieRepository) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.movieRepository = movieRepository;
    }

    public void loadAndSaveMovies(String filePath) throws Exception {
        Resource resource = resourceLoader.getResource("classpath:" + filePath);
        try (InputStream is = resource.getInputStream()) {
            List<Movie> movies = objectMapper.readValue(is, new TypeReference<>() {});
            List<Movie> unprocessedMovies = movies.stream()
                    .filter(movie -> !movieRepository.existsById(movie.getTitle()) &&
                            movie.getYear() > 1980
                    ).toList();

            int batchSize = 500;
            long startTime = System.currentTimeMillis();
            int totalSaved = 0;

            for (int i = 0; i < unprocessedMovies.size(); i += batchSize) {
                int end = Math.min(i + batchSize, unprocessedMovies.size());
                List<Movie> batch = unprocessedMovies.subList(i, end);
                movieRepository.saveAll(batch);
                totalSaved += batch.size();
            }

            long elapsedMillis = System.currentTimeMillis() - startTime;
            log.info("Saved " + totalSaved + " movies in " + elapsedMillis + " ms");
        }
    }
}