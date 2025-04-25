package com.example.spring_ai_basics.controller

import com.example.spring_ai_basics.dto.ChatRequest
import com.example.spring_ai_basics.service.SimpleAiService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/ai") // Base path for our AI endpoints
class AiController(private val simpleAiService: SimpleAiService) {

    @GetMapping("/simple")
    fun simpleChat(@RequestParam(defaultValue = "Tell me a joke") prompt: String): String? {
        return simpleAiService.getSimpleChatResponse(prompt)
    }

    // New endpoint using system prompt
    @GetMapping("/persona")
    fun personaChat(
        @RequestParam(defaultValue = "You are a helpful assistant who speaks who speaks like a proud Yorkshireman") system: String,
        @RequestParam(defaultValue = "What is the weather like today?") prompt: String
    ): String? {
        return simpleAiService.getResponseWithSystemPrompt(system, prompt)
    }

    @GetMapping("/details")
    fun detailedChat(@RequestParam(defaultValue = "Why is the sky blue?") prompt: String): Any? {
        val response = simpleAiService.getChatResponseWithDetails(prompt)
        return response?.result?.output?.text ?: "Error retrieving detailed response."
    }

    /**
     * Endpoint for stateful chat interaction.
     * conversationId is passed as a path variable.
     * The user prompt is sent in the request body.
     */
    @PostMapping("/chat/{conversationId}") // Use POST and path variable
    fun statefulChat(
        @PathVariable conversationId: String, // Get ID from URL path
        @RequestBody request: ChatRequest // Get prompt from JSON body
    ): ResponseEntity<String> {
        val response = simpleAiService.getChatbotResponse(conversationId, request.prompt)
        return if (response != null) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.internalServerError().body("Error processing chat request.")
        }
    }
}