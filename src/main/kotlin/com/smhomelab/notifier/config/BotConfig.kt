package com.smhomelab.notifier.config

import com.smhomelab.notifier.bot.CustomExceptionHandler
import com.smhomelab.notifier.service.BotCommandMenuService
import com.smhomelab.notifier.service.UserService
import io.github.dehuckakpyt.telegrambot.annotation.EnableTelegramBot
import io.github.dehuckakpyt.telegrambot.config.TelegramBotConfig
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableTelegramBot
class BotConfig(
    private val botCommandMenuService: BotCommandMenuService,
    private val userService: UserService,
) {
    @PostConstruct
    fun init() = runBlocking {
        userService.ensureMasterAdminExists()
        botCommandMenuService.initializeAllCommands()
    }

    @Bean
    fun telegramBotConfig(): TelegramBotConfig = TelegramBotConfig().apply {
        receiving {
            exceptionHandler = { CustomExceptionHandler(telegramBot, receiving.messageTemplate, templater) }
        }
    }
}
