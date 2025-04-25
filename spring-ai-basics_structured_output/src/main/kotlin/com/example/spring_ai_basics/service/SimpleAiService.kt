package com.example.spring_ai_basics.service


import com.example.spring_ai_basics.dto.ActorCharacterInfo
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
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.converter.BeanOutputConverter
import java.util.concurrent.ConcurrentHashMap // For basic thread safety



@Service
class SimpleAiService(private val chatClientBuilder: ChatClient.Builder) {

    private val chatClient = chatClientBuilder.build()
    private val chatMemories = ConcurrentHashMap<String, ChatMemory>()

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

    fun getChatResponseWithDetails(promptText: String): ChatResponse? {
        return try {
            val prompt = Prompt(UserMessage(promptText))
            chatClient.prompt(prompt).call().chatResponse()
        } catch (e: Exception) {
            println("Error calling AI service: ${e.message}")
            null
        }
    }

    fun getChatbotResponse(conversationId: String, newUserQuery: String): String? {
        val chatMemory = chatMemories.computeIfAbsent(conversationId) { InMemoryChatMemory() }
        val history: List<Message> = chatMemory.get(conversationId, 200) // Get all messages for this ID

        // TODO: Implement smarter history truncation if 'history' exceeds token limits
        val userMessage = UserMessage(newUserQuery)
        val promptMessages = history + userMessage
        val prompt = Prompt(promptMessages)

        println("--- Sending Prompt for Conversation ID: $conversationId ---")
        prompt.instructions.forEach { println("${it.messageType}: ${it.text.take(100)}...") } // Log prompt
        println("--- End Prompt ---")

        return try {
            val chatResponse = chatClient.prompt(prompt).call().chatResponse()
            val assistantMessage: AssistantMessage? = chatResponse?.result?.output
            chatMemory.add(conversationId, listOf(userMessage, assistantMessage))
            println("Added to memory for $conversationId: User query, Assistant response")
            assistantMessage?.text
        } catch (e: Exception) {
            handleError(e) // Use existing error handler
        }
    }

    fun extractActorCharacterInfo(userQueryAboutMovieRoles: String): ActorCharacterInfo? {
        // 1. Create the Output Converter for our target data class
        // Use the non-deprecated BeanOutputConverter
        val outputConverter = BeanOutputConverter(ActorCharacterInfo::class.java) // <--- Use BeanOutputConverter

        // 2. Create the PromptTemplate string.
        //    The '{format}' placeholder is where the converter inserts its instructions.
        val templateString = """
            For the movie mentioned in the following query:
            "{user_query}"
            Extract the movie title and a list of main characters with the actors who played them.
            If the movie title isn't clearly mentioned, infer it if possible or state it's unclear.
            Respond ONLY with the JSON object described below, nothing else before or after the JSON:
            {format}
        """.trimIndent()

        println("--- Template String ---")
        println(templateString)
        println("--- Template String ---")

        val promptTemplate = PromptTemplate(templateString)

        // 3. Apply variables (user query) and the format instructions from the converter
        // The '.format' method still provides the instructions for the LLM prompt
        val prompt = promptTemplate.create(mapOf(
            "user_query" to userQueryAboutMovieRoles,
            "format" to outputConverter.format // Get formatting instructions
        ))

        println("--- Generated Prompt for Structure Extraction ---")
        println(prompt.instructions)
        println("--- End Prompt ---")

        return try {
            // 4. Call the ChatClient
            val chatResponse = chatClient.prompt(prompt).call().chatResponse()
            val rawResponse = chatResponse?.result?.output?.text
            println("--- Raw LLM Response ---")
            println(rawResponse)
            println("--- End Raw Response ---")


            // 5. Convert the response content using the OutputConverter
            // Use the '.convert()' method instead of '.parse()'
            val actorInfo = rawResponse?.let { outputConverter.convert(it) } // <--- Use .convert()
            println("Converted Actor/Character Info: $actorInfo")
            actorInfo

        } catch (e: Exception) {
            // Handle potential conversion errors (LLM didn't return valid format) or API errors
            println("ERROR extracting/converting actor/character info: ${e.message}")
            e.printStackTrace() // Print stack trace for debugging
            null
        }
    }

    private fun handleError(e: Exception): String {
        println("Error calling AI service: ${e.message}")
        return "Sorry, there was an error processing your request."
    }
}