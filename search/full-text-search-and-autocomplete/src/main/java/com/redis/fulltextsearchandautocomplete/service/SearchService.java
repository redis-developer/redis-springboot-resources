package com.redis.fulltextsearchandautocomplete.service;

import com.redis.fulltextsearchandautocomplete.domain.Movie;
import com.redis.fulltextsearchandautocomplete.domain.Movie$;
import com.redis.fulltextsearchandautocomplete.repository.MovieRepository;
import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.search.stream.SearchStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private final EntityStream entityStream;
    private final MovieRepository movieRepository;

    public SearchService(EntityStream entityStream, MovieRepository movieRepository) {
        this.entityStream = entityStream;
        this.movieRepository = movieRepository;
    }

    public Map<String, Object> searchByExtractAndCast(
            String title,
            String extract,
            List<String> actors,
            Integer year,
            List<String> genres
    ) {
        logger.info("Received title: {}", title);
        logger.info("Received extract: {}", extract);
        logger.info("Received cast: {}", actors);
        logger.info("Received year: {}", year);
        logger.info("Received genres: {}", genres);

        long startTime = System.currentTimeMillis();

        SearchStream<Movie> stream = entityStream.of(Movie.class);
        List<Movie> matchedMovies = stream
                .filter(Movie$.TITLE.containing(title))
                .filter(Movie$.EXTRACT.containing(extract))
                .filter(Movie$.CAST.eq(actors))
                .filter(Movie$.YEAR.eq(year))
                .filter(Movie$.GENRES.eq(genres))
                .sorted(Movie$.YEAR)
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
