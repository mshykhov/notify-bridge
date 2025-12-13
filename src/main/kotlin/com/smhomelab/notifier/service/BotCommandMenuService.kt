package com.smhomelab.notifier.service

import com.smhomelab.notifier.bot.BotCommands
import com.smhomelab.notifier.config.AdminProperties
import com.smhomelab.notifier.persistence.facade.BotUserFacade
import com.smhomelab.notifier.persistence.model.UserRole
import io.github.dehuckakpyt.telegrambot.TelegramBot
import io.github.dehuckakpyt.telegrambot.model.telegram.BotCommandScopeChat
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class BotCommandMenuService(
    private val telegramBot: TelegramBot,
    private val botUserFacade: BotUserFacade,
    private val adminProperties: AdminProperties,
) {
    suspend fun updateCommandsForUser(telegramId: Long, role: UserRole) {
        try {
            telegramBot.setMyCommands(
                commands = BotCommands.forRole(role).map { it.toBotCommand() },
                scope = BotCommandScopeChat(chatId = telegramId.toString()),
            )
            logger.debug { "Updated commands for $telegramId" }
        } catch (e: Exception) {
            logger.warn { "Failed to update commands for $telegramId: ${e.message}" }
        }
    }

    suspend fun resetCommandsForUser(telegramId: Long) {
        try {
            telegramBot.setMyCommands(
                commands = BotCommands.forRole(null).map { it.toBotCommand() },
                scope = BotCommandScopeChat(chatId = telegramId.toString()),
            )
        } catch (e: Exception) {
            logger.debug { "Could not reset commands for $telegramId" }
        }
    }

    suspend fun initializeAllCommands() {
        telegramBot.setMyCommands(
            commands = BotCommands.forRole(null).map { it.toBotCommand() },
        )

        telegramBot.setMyCommands(
            commands = BotCommands.all(),
            scope = BotCommandScopeChat(chatId = adminProperties.masterAdminId.toString()),
        )

        botUserFacade.findAll().forEach { user ->
            updateCommandsForUser(user.telegramId, user.role)
        }

        logger.info { "Bot commands initialized" }
    }
}
