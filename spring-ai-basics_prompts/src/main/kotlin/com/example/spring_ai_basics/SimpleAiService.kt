package com.example.spring_ai_basics


import org.springframework.ai.chat.client.ChatClient // Core Spring AI interface
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service

@Service
class SimpleAiService(
    private val chatClientBuilder: ChatClient.Builder
) {

    private val chatClient = chatClientBuilder.build()

    // Existing simple method (internally creates a UserMessage)
    fun getSimpleChatResponse(prompt: String): String? {
        return try {
            chatClient.prompt()
                .user(prompt) // shorthand for .messages(UserMessage(prompt))
                .call()
                .content()
        } catch (e: Exception) {
            // Basic error handling
            println("Error calling AI service: ${e.message}")
            "Sorry, I couldn't process that request."
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

    // Helper for basic error handling
    private fun handleError(e: Exception): String {
        println("Error calling AI service: ${e.message}")
        // Consider more specific error handling based on exception type
        return "Sorry, there was an error processing your request."
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
}