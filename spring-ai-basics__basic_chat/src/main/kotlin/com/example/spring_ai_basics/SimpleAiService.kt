package com.example.spring_ai_basics


import org.springframework.ai.chat.client.ChatClient // Core Spring AI interface
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.stereotype.Service

@Service
class SimpleAiService(
    private val chatClientBuilder: ChatClient.Builder
) {

    // Build the ChatClient instance, potentially customizing it further
    private val chatClient = chatClientBuilder.build()

    /**
     * Sends a prompt to the configured LLM and returns the response content.
     */
    fun getSimpleChatResponse(prompt: String): String? {
        return try {
            // The simplest way to call: provide prompt, get content string back
            chatClient.prompt()
                .user(prompt) // Set the user's message
                .call()       // Make the API call
                .content()    // Extract the String content from the response
        } catch (e: Exception) {
            // Basic error handling
            println("Error calling AI service: ${e.message}")
            "Sorry, I couldn't process that request."
        }
    }

    // Optional: A slightly more detailed way showing access to the full response
    fun getChatResponseWithDetails(prompt: String): ChatResponse? {
        return try {
            // Uses the Prompt object explicitly
            chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse() // Get the full ChatResponse object
        } catch (e: Exception) {
            println("Error calling AI service: ${e.message}")
            null
        }
    }
}