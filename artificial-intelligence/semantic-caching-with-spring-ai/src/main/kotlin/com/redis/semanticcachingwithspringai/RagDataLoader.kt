package com.redis.semanticcachingwithspringai

import org.slf4j.LoggerFactory
import org.springframework.ai.reader.JsonReader
import org.springframework.ai.vectorstore.redis.RedisVectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.util.zip.GZIPInputStream

@Component
class RagDataLoader(
    private val beerVectorStore: RedisVectorStore
) : ApplicationRunner {

    @Value("classpath:/data/beers.json.gz")
    private lateinit var data: Resource

    override fun run(args: ApplicationArguments) {
        val indexInfo = beerVectorStore.jedis.ftInfo("beerIdx")
        if (indexInfo["num_terms"] as Long > 20000) {
            logger.info("Embeddings already loaded. Skipping")
            return
        }

        var file: Resource = data
        if (data.filename?.endsWith(".gz") == true) {
            val inputStream = GZIPInputStream(data.inputStream)
            file = InputStreamResource(inputStream, "beers.json.gz")
        }

        logger.info("Creating Embeddings (May take around 3 minutes...")
        val loader = JsonReader(file, *KEYS)
        val documents = loader.get()
        val batchSize = 500

        documents.chunked(batchSize).forEachIndexed { index, batch ->
            beerVectorStore.add(batch)
            logger.info("Inserted batch ${index + 1} with ${batch.size} documents")
        }

        logger.info("${documents.size} embeddings created.")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RagDataLoader::class.java)
        private val KEYS = arrayOf("name", "abv", "ibu", "description")
    }
}