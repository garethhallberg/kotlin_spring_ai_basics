package com.example.spring_ai_basics

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ai") // Base path for our AI endpoints
class AiController(private val simpleAiService: SimpleAiService) {

    // Endpoint to test the simple response
    @GetMapping("/simple")
    fun simpleChat(@RequestParam(defaultValue = "Tell me a joke") prompt: String): String? {
        return simpleAiService.getSimpleChatResponse(prompt)
    }

    // Endpoint to see the more detailed response structure (just for illustration)
    @GetMapping("/details")
    fun detailedChat(@RequestParam(defaultValue = "Why is the sky blue?") prompt: String): Any? {
        val response = simpleAiService.getChatResponseWithDetails(prompt)
        // Returning the whole response object might be complex depending on serialization
        // For now, let's just return the content or an error message
        return response?.result?.output?.text ?: "Error retrieving detailed response."

    }
}