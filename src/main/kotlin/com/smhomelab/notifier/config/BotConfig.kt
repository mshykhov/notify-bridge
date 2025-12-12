package com.smhomelab.notifier.config

import com.smhomelab.notifier.bot.BotCommands
import io.github.dehuckakpyt.telegrambot.TelegramBot
import io.github.dehuckakpyt.telegrambot.annotation.EnableTelegramBot
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

@Configuration
@EnableTelegramBot
class BotConfig(
    private val telegramBot: TelegramBot
) {
    private val log = LoggerFactory.getLogger(BotConfig::class.java)

    @PostConstruct
    fun registerCommands() = runBlocking {
        telegramBot.setMyCommands(BotCommands.all())
        log.info("Bot commands registered: {}", BotCommands.entries.map { it.command })
    }
}
