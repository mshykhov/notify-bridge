package com.smhomelab.notifier.service

import com.smhomelab.notifier.bot.BotCommands
import com.smhomelab.notifier.config.AdminProperties
import com.smhomelab.notifier.model.UserRole
import com.smhomelab.notifier.repository.BotUserRepository
import io.github.dehuckakpyt.telegrambot.TelegramBot
import io.github.dehuckakpyt.telegrambot.model.telegram.BotCommandScopeChat
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BotCommandMenuService(
    private val telegramBot: TelegramBot,
    private val botUserRepository: BotUserRepository,
    private val adminProperties: AdminProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun updateCommandsForUser(telegramId: Long, role: UserRole) {
        try {
            telegramBot.setMyCommands(
                commands = BotCommands.forRole(role).map { it.toBotCommand() },
                scope = BotCommandScopeChat(chatId = telegramId.toString())
            )
            log.debug("Updated commands for $telegramId")
        } catch (e: Exception) {
            log.warn("Failed to update commands for $telegramId: ${e.message}")
        }
    }

    suspend fun resetCommandsForUser(telegramId: Long) {
        try {
            telegramBot.setMyCommands(
                commands = BotCommands.forRole(null).map { it.toBotCommand() },
                scope = BotCommandScopeChat(chatId = telegramId.toString())
            )
        } catch (e: Exception) {
            log.debug("Could not reset commands for $telegramId")
        }
    }

    suspend fun initializeAllCommands() {
        // Global (public)
        telegramBot.setMyCommands(
            commands = BotCommands.forRole(null).map { it.toBotCommand() }
        )

        // Master admin - all commands
        telegramBot.setMyCommands(
            commands = BotCommands.all(),
            scope = BotCommandScopeChat(chatId = adminProperties.masterAdminId.toString())
        )

        // Each registered user
        botUserRepository.findAll().forEach { user ->
            updateCommandsForUser(user.telegramId, user.role)
        }

        log.info("Bot commands initialized")
    }
}
