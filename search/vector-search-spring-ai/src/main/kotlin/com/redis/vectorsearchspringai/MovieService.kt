package com.redis.vectorsearchspringai

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.redis.om.spring.ops.RedisModulesOperations
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder
import org.springframework.ai.vectorstore.redis.RedisVectorStore
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service


@Service
class MovieService(
    private val movieVectorStore: RedisVectorStore,
    private val resourceLoader: ResourceLoader,
    private val objectMapper: ObjectMapper,
    private val redisModuleOperations: RedisModulesOperations<String>,
) {
    private val log = LoggerFactory.getLogger(MovieService::class.java)

    fun loadMovies(filePath: String): List<Movie> {
        val resource: Resource = resourceLoader.getResource("classpath:$filePath")
        return resource.inputStream.use { `is` ->
            val movies: MutableList<Movie> =
                objectMapper.readValue<MutableList<Movie>>(
                    `is`,
                    object : TypeReference<MutableList<Movie>?>() {}
                )

            movies.stream().filter { movie -> movie.year > 1980 }.toList()
        }
    }

    fun storeMovies(movies: List<Movie>) {
        log.info("Storing ${movies.size} movies")
        val documents = movies.map { movie ->
            val text = movie.extract ?: ""
            val metadata = mapOf(
                "title" to (movie.title ?: ""),
                "year" to movie.year,
                "cast" to movie.cast,
                "genres" to movie.genres,
                "thumbnail" to (movie.thumbnail ?: "")
            )
            Document(text, metadata)
        }
        movieVectorStore.add(documents)
        log.info("Stored ${movies.size} movies")
    }

    fun searchMovies(
        title: String,
        extract: String,
        actors: List<String>,
        year: Int? = null,
        genres: List<String>,
        numberOfNearestNeighbors: Int
    ): Map<String, Any> {
        val b = FilterExpressionBuilder()

        val logger = LoggerFactory.getLogger("SearchService")

        logger.info("Received title: {}", title)
        logger.info("Received extract: {}", extract)
        logger.info("Received cast: {}", actors)
        logger.info("Received year: {}", year)
        logger.info("Received genres: {}", genres)
        logger.info("Received nearest neighbors: {}", numberOfNearestNeighbors)

        val filterList = mutableListOf<FilterExpressionBuilder.Op>()

        if (title.isNotBlank()) {
            filterList.add(b.`in`("title", title))
        }

        if (actors.isNotEmpty()) {
            filterList.add(b.`in`("actors", actors))
        }

        if (year != null) {
            filterList.add(b.eq("year", year))
        }

        if (genres.isNotEmpty()) {
            filterList.add(b.`in`("genres", genres))
        }

        val filterExpression = when (filterList.size) {
            0 -> null
            1 -> filterList[0]
            else -> filterList.reduce { acc, expr -> b.and(acc, expr) }
        }?.build()

        val start = System.currentTimeMillis()

        val searchResults = movieVectorStore.similaritySearch(
            SearchRequest.builder()
                .query(extract)
                .topK(numberOfNearestNeighbors)
                .filterExpression(filterExpression)
                .build()
        ) ?: emptyList()

        val transformedResults = searchResults.map { result ->
            // Log the result structure for debugging
            logger.info("Result: {}", result)
            logger.info("Result metadata: {}", result.metadata)
            logger.info("Result class: {}", result.javaClass.name)

            // Try to access properties using reflection
            val properties = result.javaClass.declaredFields
            properties.forEach { field ->
                field.isAccessible = true
                try {
                    logger.info("Property: {} = {}", field.name, field.get(result))
                } catch (e: Exception) {
                    logger.error("Error accessing property: {}", field.name, e)
                }
            }

            val metadata = result.metadata

            var extractContent: String? = null
            try {
                // Try different property names that might contain the content
                val contentField = result.javaClass.declaredFields.find { it.name == "content" || it.name == "text" || it.name == "value" }
                if (contentField != null) {
                    contentField.isAccessible = true
                    extractContent = contentField.get(result) as? String
                    logger.info("Found content in field: {}", contentField.name)
                } else {
                    extractContent = result.toString()
                    logger.info("Using result.toString() for content")
                }
            } catch (e: Exception) {
                logger.error("Error extracting content", e)
            }

            val movie = Movie(
                title = metadata["title"] as? String ?: "Unknown Title",
                year = (metadata["year"] as? String)?.toInt() ?: 0,
                cast = (metadata["cast"] as? String)?.replace("\"", "")?.replace("[", "")?.replace("]", "")?.split(",") ?: emptyList(),
                genres = (metadata["genres"] as? String)?.replace("\"", "")?.replace("[", "")?.replace("]", "")?.split(",") ?: emptyList(),
                extract = extractContent,
                thumbnail = metadata["thumbnail"] as? String
            )

            logger.info("Created movie: {}", movie)

            object {
                val first = movie
                val second = result.score
            }
        }

        val elapsed = System.currentTimeMillis() - start
        logger.info("Search completed in {} ms", elapsed)
        logger.info("Transformed {} results", transformedResults.size)

        return mapOf(
            "movies" to transformedResults,
            "count" to transformedResults.size,
            "searchTime" to elapsed
        )
    }

    fun getAllGenres(): MutableSet<String?> {
        log.info("Fetching all unique genres")
        val startTime = System.currentTimeMillis()

        val searchOps = redisModuleOperations.opsForSearch("movieIdx")
        searchOps.tagVals("genres")
        val allGenres = searchOps.tagVals("genres")

        val endTime = System.currentTimeMillis()
        val fetchTime = endTime - startTime

        log.info(
            "Fetched {} unique genres in {} ms",
            allGenres.size,
            fetchTime
        )

        return allGenres
    }
}
