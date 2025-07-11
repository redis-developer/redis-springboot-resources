package com.redis.vectorsearch.service;

import com.redis.vectorsearch.domain.Movie;
import com.redis.vectorsearch.domain.Movie$;
import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.search.stream.SearchStream;
import com.redis.om.spring.tuple.Fields;
import com.redis.om.spring.tuple.Pair;
import com.redis.om.spring.vectorize.Embedder;
import com.redis.vectorsearch.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private final MovieRepository movieRepository;
    private final EntityStream entityStream;
    private final Embedder embedder;

    public SearchService(MovieRepository movieRepository, EntityStream entityStream, Embedder embedder) {
        this.movieRepository = movieRepository;
        this.entityStream = entityStream;
        this.embedder = embedder;
    }

    public Map<String, Object> search(
            String title,
            String extract,
            List<String> actors,
            Integer year,
            List<String> genres,
            Integer numberOfNearestNeighbors
    ) {
        logger.info("Received title: {}", title);
        logger.info("Received extract: {}", extract);
        logger.info("Received cast: {}", actors);
        logger.info("Received year: {}", year);
        logger.info("Received genres: {}", genres);
        logger.info("Received nearest neighbors: {}", numberOfNearestNeighbors);

        SearchStream<Movie> stream = entityStream.of(Movie.class);

        if (extract != null) {
            float[] embeddedQuery = embedder.getTextEmbeddingsAsFloats(List.of(extract), Movie$.EXTRACT).getFirst();
            stream = stream.filter(Movie$.EMBEDDED_EXTRACT.knn(numberOfNearestNeighbors, embeddedQuery))
                            .sorted(Movie$._EMBEDDED_EXTRACT_SCORE);
        }

        long startTime = System.currentTimeMillis();
        List<Pair<Movie, Double>> matchedMovies = stream
                .filter(Movie$.TITLE.containing(title))
                .filter(Movie$.CAST.eq(actors))
                .filter(Movie$.YEAR.eq(year))
                .filter(Movie$.GENRES.eq(genres))
                .map(Fields.of(Movie$._THIS, Movie$._EMBEDDED_EXTRACT_SCORE))
                .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        long searchTime = endTime - startTime;

        logger.info("Search completed in {} ms", searchTime);

        Map<String, Object> result = new HashMap<>();
        result.put("movies", matchedMovies);
        result.put("count", matchedMovies.size());
        result.put("searchTime", searchTime);

        return result;
    }

    public Set<String> getAllGenres() {
        logger.info("Fetching all unique genres");
        long startTime = System.currentTimeMillis();

        Iterable<String> genresIterable = movieRepository.getAllGenres();
        Set<String> allGenres = new HashSet<>();
        genresIterable.forEach(allGenres::add);

        long endTime = System.currentTimeMillis();
        long fetchTime = endTime - startTime;

        logger.info("Fetched {} unique genres in {} ms", allGenres.size(), fetchTime);

        return allGenres;
    }
}
