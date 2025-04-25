package com.example.spring_ai_basics.service


import org.springframework.ai.chat.client.ChatClient // Core Spring AI interface
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service

import org.springframework.ai.chat.memory.ChatMemory // Import ChatMemory
import org.springframework.ai.chat.memory.InMemoryChatMemory // Import InMemoryChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.model.ChatResponse
import java.util.concurrent.ConcurrentHashMap // For basic thread safety

@Service
class SimpleAiService(private val chatClientBuilder: ChatClient.Builder) {

    private val chatClient = chatClientBuilder.build()

    // Store ChatMemory instances, keyed by conversation ID
    // Using ConcurrentHashMap for basic thread safety in a multi-user scenario
    private val chatMemories = ConcurrentHashMap<String, ChatMemory>()

    // Existing simple method (internally creates a UserMessage)
    fun getSimpleChatResponse(prompt: String): String? {
        return try {
            chatClient.prompt()
                .user(prompt) // shorthand for .messages(UserMessage(prompt))
                .call()
                .content()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    /**
     * Uses System and User messages for more context.
     */
    fun getResponseWithSystemPrompt(systemText: String, userText: String): String? {
        val systemMessage = SystemMessage(systemText) // Create a SystemMessage
        val userMessage = UserMessage(userText)       // Create a UserMessage

        // Create a Prompt object with a list of messages
        val prompt = Prompt(listOf(systemMessage, userMessage))

        return try {
            chatClient.prompt(prompt) // Pass the Prompt object
                .call()
                .content()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    // Existing method to get full response
    fun getChatResponseWithDetails(promptText: String): ChatResponse? {
        return try {
            val prompt = Prompt(UserMessage(promptText))
            chatClient.prompt(prompt).call().chatResponse()
        } catch (e: Exception) {
            println("Error calling AI service: ${e.message}")
            null
        }
    }
    /**
     * Handles a stateful chatbot turn, managing history with ChatMemory.
     */
    fun getChatbotResponse(conversationId: String, newUserQuery: String): String? {
        // 1. Get or create ChatMemory for this conversation
        // computeIfAbsent ensures we create a new InMemoryChatMemory only if the ID is new
        val chatMemory = chatMemories.computeIfAbsent(conversationId) { InMemoryChatMemory() }

        // 2. Retrieve message history (optional: limit the number of messages)
        // The 'messages' method requires context, which we can provide via a map.
        // Let's retrieve the last, say, 10 messages to manage context window size.
        // Note: The exact map keys required might vary or evolve in Spring AI.
        // As of some versions, just providing the conversation ID might suffice if
        // the memory implementation uses it implicitly. Let's assume we don't need
        // explicit keys for retrieval with InMemoryChatMemory for simplicity here,
        // but check documentation for specific ChatMemory implementations.*
        // A simpler approach: InMemoryChatMemory might just return all messages with get()
        val history: List<Message> = chatMemory.get(conversationId, 200) // Get all messages for this ID

        // TODO: Implement smarter history truncation if 'history' exceeds token limits

        // 3. Construct the prompt for the LLM
        val userMessage = UserMessage(newUserQuery)
        // Combine retrieved history with the new user message
        val promptMessages = history + userMessage
        val prompt = Prompt(promptMessages)

        println("--- Sending Prompt for Conversation ID: $conversationId ---")
        prompt.instructions.forEach { println("${it.messageType}: ${it.text.take(100)}...") } // Log prompt
        println("--- End Prompt ---")

        return try {
            // 4. Call the LLM
            val chatResponse = chatClient.prompt(prompt).call().chatResponse()
            val assistantMessage: AssistantMessage? = chatResponse?.result?.output

            // 5. Add the user query AND the assistant's response to memory
            // Use the 'add' method to store the latest exchange
            chatMemory.add(conversationId, listOf(userMessage, assistantMessage))

            println("Added to memory for $conversationId: User query, Assistant response")

            // 6. Return the content
            assistantMessage?.text

        } catch (e: Exception) {
            handleError(e) // Use existing error handler
        }
    }

    // Helper for basic error handling (from previous post)
    private fun handleError(e: Exception): String {
        println("Error calling AI service: ${e.message}")
        return "Sorry, there was an error processing your request."
    }
}