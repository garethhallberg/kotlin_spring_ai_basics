package com.example.spring_ai_basics.service

// Imports needed for the basic structure
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.PromptTemplate
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
    private val embeddingModel: EmbeddingModel,
    private val vectorStore: VectorStore,
    private val chatModel: ChatModel, // Inject the ChatModel
    @Value("classpath:/static/hull-info.txt") private val productInfoResource: Resource
) {
    private val log = LoggerFactory.getLogger(RagService::class.java)

    @PostConstruct
    fun loadData() {
        log.info("RagService initialized. VectorStore type: ${vectorStore.javaClass.simpleName}")
        log.info("Attempting to load data from resource: ${productInfoResource.filename}")

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

        val textReader = TextReader(productInfoResource)
        textReader.setCharset(StandardCharsets.UTF_8) // Good practice to set charset
        val documents: List<Document> = textReader.get() // Reads the file content

        log.info("Loaded ${documents.size} document(s) from the resource.")
        documents.firstOrNull()?.let {
            log.info("First document content snippet: ${it.text?.take(100)}...")
        }

        val textSplitter = TokenTextSplitter() // Default chunk/overlap sizes
        val chunkedDocuments = textSplitter.apply(documents) // Apply splitter to the list
        log.info("Split document into ${chunkedDocuments.size} chunks.")
        chunkedDocuments.firstOrNull()?.let {
            log.info("First chunk content snippet: ${it.text?.take(100)}...")
        }

        try {
            log.info("Adding ${chunkedDocuments.size} chunks to the vector store...")
            vectorStore.add(chunkedDocuments) // Add the chunks
            log.info("Successfully added document chunks to the vector store.")
        } catch (e: Exception) {
            log.error("Error adding documents to vector store: ${e.message}", e)
        }
    }

    // New method for retrieval
    fun findRelevantDocuments(query: String, topK: Int = 4): List<Document> {
        log.info("Retrieving relevant documents for query: '$query'")

        // No need to generate embedding manually here IF using VectorStore directly
        // The VectorStore implementation (like SimpleVectorStore) often handles it.
        // We create a SearchRequest instead.

        val searchRequest = SearchRequest.builder()
            .query(query) // The user's query string
            .topK(topK)   // How many top results to retrieve [cite: 62]
            .build()      // Build the request [cite: 62]

        try {
            val similarDocuments = vectorStore.similaritySearch(searchRequest) // [cite: 28, 63, 74]
            log.info("Found ${similarDocuments?.size} relevant documents.")
            return similarDocuments as List<Document>
        } catch (e: Exception) {
            log.error("Error performing similarity search: ${e.message}", e)
            return emptyList()
        }
    }

    // Helper function (could be inside RagService or elsewhere)
    fun formatContext(documents: List<Document>): String {
        return documents.joinToString("\n\n---\n\n") { it.text ?: "" } // Use .text or .getContent() [cite: 9, 65, 67]
    }

    // New method for generation
    fun generateAnswer(query: String): String? {
        log.info("Generating answer for query: '$query'")

        // 1. Retrieve relevant documents
        val relevantDocuments = findRelevantDocuments(query)
        if (relevantDocuments.isEmpty()) {
            log.warn("No relevant documents found for query: '$query'")
            // Decide how to handle this - maybe return a default message
            // or let the LLM handle it without context. For this example,
            // we'll proceed but the context will be empty.
        }

        // 2. Create the context string
        val context = formatContext(relevantDocuments)

        // 3. Build the prompt using a template
        val promptTemplate = PromptTemplate(
            """
            Please answer the following question based *only* on the provided context.
            If the context does not contain the answer, state that you cannot answer based on the provided information.

            Context:
            ---
            {context}
            ---

            Question: {query}
            """
        )

        val prompt = promptTemplate.create(mapOf("context" to context, "query" to query))

        // 4. Call the ChatModel
        try {
            val chatResponse = chatModel.call(prompt) // Use the created Prompt object
            val answer = chatResponse.result?.output?.text // Extract the answer text
            log.info("Generated answer successfully.")
            return answer
        } catch (e: Exception) {
            log.error("Error calling ChatModel: ${e.message}", e)
            return "Sorry, I encountered an error trying to generate an answer."
        }
    }

}

