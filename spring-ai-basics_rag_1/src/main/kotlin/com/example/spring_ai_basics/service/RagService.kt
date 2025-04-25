package com.example.spring_ai_basics.service

// Imports needed for the basic structure
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.reader.TextReader
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets

@Service
class RagService(
    // Inject the configured EmbeddingModel (e.g., OpenAiEmbeddingModel)
    private val embeddingModel: EmbeddingModel,
    // Inject the VectorStore bean WE DEFINED in VectorStoreConfig
    private val vectorStore: VectorStore,
    // Inject the Resource using Spring's @Value annotation
    @Value("classpath:/static/product-info.txt") private val productInfoResource: Resource
) {
    // Initialize logger for the class
    private val log = LoggerFactory.getLogger(RagService::class.java)

    // Add back the PostConstruct annotation and loadData method
    @PostConstruct
    fun loadData() {
        log.info("RagService initialized. VectorStore type: ${vectorStore.javaClass.simpleName}")
        log.info("Attempting to load data from resource: ${productInfoResource.filename}")

        // Idempotency Check: Only load if store appears empty
        try {
            val checkRequest = SearchRequest.builder()
                .query("Project Quantum Leap") // Use a term likely in your data
                .topK(1)
                .build()
            if (vectorStore.similaritySearch(checkRequest)?.isNotEmpty()!!) {
                log.info("Vector store already contains data. Skipping data loading.")
                return // Exit loadData if data exists
            }
        } catch (e: Exception) {
            log.warn("Could not perform check on vector store, proceeding with loading: ${e.message}")
            // Handle cases where the store might be empty or doesn't support search yet
        }

        // 1. Load the resource using TextReader
        val textReader = TextReader(productInfoResource)
        textReader.setCharset(StandardCharsets.UTF_8) // Good practice to set charset
        val documents: List<Document> = textReader.get() // Reads the file content

        log.info("Loaded ${documents.size} document(s) from the resource.")
        // Log content snippet for verification (optional)
        documents.firstOrNull()?.let {
            log.info("First document content snippet: ${it.text?.take(100)}...")
        }

        // 2. Split the document(s) into smaller chunks
        // TokenTextSplitter uses tokenizer info from the EmbeddingModel bean implicitly
        val textSplitter = TokenTextSplitter() // Default chunk/overlap sizes
        val chunkedDocuments = textSplitter.apply(documents) // Apply splitter to the list
        log.info("Split document into ${chunkedDocuments.size} chunks.")
        chunkedDocuments.firstOrNull()?.let {
            log.info("First chunk content snippet: ${it.text?.take(100)}...")
        }

        // Next step: Add the chunked documents to the vector store...
        // 3. Add the chunked documents to the vector store
        // SimpleVectorStore will use the configured EmbeddingModel to generate embeddings
        try {
            log.info("Adding ${chunkedDocuments.size} chunks to the vector store...")
            vectorStore.add(chunkedDocuments) // Add the chunks
            log.info("Successfully added document chunks to the vector store.")
        } catch (e: Exception) {
            log.error("Error adding documents to vector store: ${e.message}", e)
        }
    }
}