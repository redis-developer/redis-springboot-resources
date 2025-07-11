package com.redis.vectorsearch.repository;

import com.redis.vectorsearch.domain.Movie;
import com.redis.om.spring.autocomplete.Suggestion;
import com.redis.om.spring.repository.RedisDocumentRepository;
import com.redis.om.spring.repository.query.autocomplete.AutoCompleteOptions;

import java.util.List;

public interface MovieRepository extends RedisDocumentRepository<Movie, String> {
    List<Suggestion> autoCompleteTitle(String title, AutoCompleteOptions options);

    Iterable<String> getAllGenres();
}