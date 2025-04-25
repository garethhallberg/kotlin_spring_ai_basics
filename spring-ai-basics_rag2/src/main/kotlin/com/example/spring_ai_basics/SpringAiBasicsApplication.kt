package com.example.spring_ai_basics

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
	// Add this exclude attribute:
	exclude = [
		DataSourceAutoConfiguration::class,
		DataSourceTransactionManagerAutoConfiguration::class
	]
)

class SpringAiBasicsApplication

fun main(args: Array<String>) {
	runApplication<SpringAiBasicsApplication>(*args)
}
