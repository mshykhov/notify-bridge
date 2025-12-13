package com.smhomelab.notifier.config

import io.github.dehuckakpyt.telegrambot.annotation.EnableTelegramBot
import io.github.dehuckakpyt.telegrambot.config.TelegramBotConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableTelegramBot
class TelegramBotConfiguration {
    @Bean
    fun telegramBotConfig(): TelegramBotConfig = TelegramBotConfig().apply {
        receiving {
            exceptionHandler = { CustomExceptionHandler(telegramBot, receiving.messageTemplate, templater) }
        }
    }
}
