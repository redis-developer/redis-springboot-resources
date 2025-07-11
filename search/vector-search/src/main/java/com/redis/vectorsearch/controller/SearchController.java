package com.redis.vectorsearch.controller;

import com.redis.vectorsearch.repository.MovieRepository;
import com.redis.vectorsearch.service.SearchService;
import com.redis.om.spring.autocomplete.Suggestion;
import com.redis.om.spring.repository.query.autocomplete.AutoCompleteOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

@RestController
public class SearchController {

    private final SearchService searchService;
    private final MovieRepository movieRepository;

    public SearchController(SearchService searchService, MovieRepository movieRepository) {
        this.searchService = searchService;
        this.movieRepository = movieRepository;
    }

    @GetMapping("/search/{q}")
    public Map<String, Object> query(@PathVariable("q") String query) {
        long startTime = System.currentTimeMillis();

        List<Suggestion> suggestions = movieRepository
                .autoCompleteTitle(query, AutoCompleteOptions.get().withPayload());

        long endTime = System.currentTimeMillis();
        long autocompleteTime = endTime - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("suggestions", suggestions);
        result.put("autocompleteTime", autocompleteTime);

        return result;
    }

    @GetMapping("/search")
    public Map<String, Object> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<String> cast,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) List<String> genres,
            @RequestParam(required = false) Integer numberOfNearestNeighbors
    ) {

        return searchService.search(
                title,
                text,
                cast,
                year,
                genres,
                numberOfNearestNeighbors
        );
    }

    @GetMapping("/genres")
    public Map<String, Object> getAllGenres() {
        long startTime = System.currentTimeMillis();

        Set<String> genres = searchService.getAllGenres();

        long endTime = System.currentTimeMillis();
        long fetchTime = endTime - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("genres", genres);
        result.put("count", genres.size());
        result.put("fetchTime", fetchTime);

        return result;
    }
}
