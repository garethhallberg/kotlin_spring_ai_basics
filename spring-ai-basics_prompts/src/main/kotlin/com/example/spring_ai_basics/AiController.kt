package com.example.spring_ai_basics

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
}