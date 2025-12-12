package com.smhomelab.notifier.config

import com.smhomelab.notifier.service.BotCommandMenuService
import io.github.dehuckakpyt.telegrambot.annotation.EnableTelegramBot
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Configuration

@Configuration
@EnableTelegramBot
class BotConfig(
    private val botCommandMenuService: BotCommandMenuService
) {
    @PostConstruct
    fun registerCommands() = runBlocking {
        botCommandMenuService.initializeAllCommands()
    }
}
