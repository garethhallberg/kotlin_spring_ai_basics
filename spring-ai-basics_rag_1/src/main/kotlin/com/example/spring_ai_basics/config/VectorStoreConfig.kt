package com.example.spring_ai_basics.config

import jakarta.annotation.PreDestroy
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File // Import File
import org.slf4j.LoggerFactory

@Configuration
class VectorStoreConfig {

    private val log = LoggerFactory.getLogger(VectorStoreConfig::class.java)

    // Read the persistence path from application.properties
    @Value("\${spring.ai.vectorstore.simple.store.path:vectorstore.json}") // Default value if property not set
    private lateinit var vectorStorePath: String

    // Keep track of the bean instance for saving
    private lateinit var vectorStoreBeanInstance: SimpleVectorStore
    private lateinit var vectorStoreFileInstance: File

    @Bean
    fun simpleVectorStore(embeddingModel: EmbeddingModel): VectorStore {
        val builder = SimpleVectorStore.builder(embeddingModel) // Pass EmbeddingModel to builder

        val simpleVectorStore = builder.build() // Build the instance
        this.vectorStoreBeanInstance = simpleVectorStore
        // Handle persistence: Load from file if it exists AFTER creating the instance
        val vectorStoreFile = File(vectorStorePath)
        if (vectorStoreFile.exists()) {
            println("Loading SimpleVectorStore from file: ${vectorStoreFile.absolutePath}")
            // Pass the file path to the instance for loading
            simpleVectorStore.load(vectorStoreFile)
        } else {
            println("Vector store file not found, starting fresh: ${vectorStoreFile.absolutePath}")
        }
        return simpleVectorStore
    }

    // Explicitly save the vector store on application shutdown
    @PreDestroy
    fun saveVectorStore() {
        // Check if the bean instance was initialized
        if (::vectorStoreBeanInstance.isInitialized) {
            // Create the File object directly using the injected path
            val saveFile = File(vectorStorePath)
            log.info("Attempting to save SimpleVectorStore to file: ${saveFile.absolutePath}")
            try {
                // Use the stored bean instance to save
                vectorStoreBeanInstance.save(saveFile)
                log.info("SimpleVectorStore saved successfully.")
            } catch (e: Exception) {
                log.error("Error saving SimpleVectorStore to file: ${e.message}", e)
            }
        } else {
            log.warn("VectorStore bean was not initialized, skipping save.")
        }
    }
}