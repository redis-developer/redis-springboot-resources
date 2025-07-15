package com.redis.vectorsearch.service;

import com.redis.vectorsearch.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingStatusService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingStatusService.class);
    private final MovieRepository movieRepository;

    public EmbeddingStatusService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    /**
     * Checks if the embeddings are ready by verifying that there are at least 10,000 documents in the repository.
     * @return true if the embeddings are ready, false otherwise
     */
    public boolean areEmbeddingsReady() {
        try {
            long embeddedDocs = movieRepository.count();
            logger.info("Number of embedded documents: {}", embeddedDocs);
            return embeddedDocs >= 10000;
        } catch (Exception e) {
            logger.error("Error checking embedding status", e);
            return false;
        }
    }

    /**
     * Gets the total number of documents in the repository.
     * @return the number of documents
     */
    public long getTotalDocNum() {
        try {
            return movieRepository.count();
        } catch (Exception e) {
            logger.error("Error getting total document count", e);
            return 0;
        }
    }
}