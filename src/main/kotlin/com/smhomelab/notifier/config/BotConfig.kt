package com.smhomelab.notifier.config

import com.smhomelab.notifier.service.BotCommandMenuService
import com.smhomelab.notifier.service.BotUserService
import io.github.dehuckakpyt.telegrambot.annotation.EnableTelegramBot
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Configuration

@Configuration
@EnableTelegramBot
class BotConfig(
    private val botCommandMenuService: BotCommandMenuService,
    private val botUserService: BotUserService
) {
    @PostConstruct
    fun init() = runBlocking {
        botUserService.ensureMasterAdminExists()
        botCommandMenuService.initializeAllCommands()
    }
}
