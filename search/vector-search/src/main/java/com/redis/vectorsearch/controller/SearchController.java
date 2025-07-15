package com.redis.vectorsearch.controller;

import com.redis.vectorsearch.repository.MovieRepository;
import com.redis.vectorsearch.service.EmbeddingStatusService;
import com.redis.vectorsearch.service.SearchService;
import com.redis.om.spring.autocomplete.Suggestion;
import com.redis.om.spring.repository.query.autocomplete.AutoCompleteOptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final EmbeddingStatusService embeddingStatusService;

    public SearchController(SearchService searchService, MovieRepository movieRepository, EmbeddingStatusService embeddingStatusService) {
        this.searchService = searchService;
        this.movieRepository = movieRepository;
        this.embeddingStatusService = embeddingStatusService;
    }

    @GetMapping("/search/{q}")
    public ResponseEntity<Object> query(@PathVariable("q") String query) {
        if (!embeddingStatusService.areEmbeddingsReady()) {
            long embeddedDocs = embeddingStatusService.getTotalDocNum();
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Embeddings are still being created (" + embeddedDocs + " of 10000 already created). This operation takes around two minutes to complete. Please try again later."));
        }

        long startTime = System.currentTimeMillis();

        List<Suggestion> suggestions = movieRepository
                .autoCompleteTitle(query, AutoCompleteOptions.get().withPayload());

        long endTime = System.currentTimeMillis();
        long autocompleteTime = endTime - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("suggestions", suggestions);
        result.put("autocompleteTime", autocompleteTime);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<String> cast,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) List<String> genres,
            @RequestParam(required = false) Integer numberOfNearestNeighbors
    ) {
        if (!embeddingStatusService.areEmbeddingsReady()) {
            long embeddedDocs = embeddingStatusService.getTotalDocNum();
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Embeddings are still being created (" + embeddedDocs + " of 10000 already created). This operation takes around two minutes to complete. Please try again later."));
        }

        return ResponseEntity.ok(searchService.search(
                title,
                text,
                cast,
                year,
                genres,
                numberOfNearestNeighbors
        ));
    }

    @GetMapping("/genres")
    public ResponseEntity<Object> getAllGenres() {
        if (!embeddingStatusService.areEmbeddingsReady()) {
            long embeddedDocs = embeddingStatusService.getTotalDocNum();
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Embeddings are still being created (" + embeddedDocs + " of 10000 already created). This operation takes around two minutes to complete. Please try again later."));
        }

        long startTime = System.currentTimeMillis();

        Set<String> genres = searchService.getAllGenres();

        long endTime = System.currentTimeMillis();
        long fetchTime = endTime - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("genres", genres);
        result.put("count", genres.size());
        result.put("fetchTime", fetchTime);

        return ResponseEntity.ok(result);
    }
}
