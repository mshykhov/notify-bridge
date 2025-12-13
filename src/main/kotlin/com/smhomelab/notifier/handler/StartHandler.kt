package com.smhomelab.notifier.handler

import com.smhomelab.notifier.bot.BotCommands
import com.smhomelab.notifier.bot.ParseMode
import com.smhomelab.notifier.bot.secureCommand
import com.smhomelab.notifier.service.AuthorizationService
import com.smhomelab.notifier.service.InvitationService
import com.smhomelab.notifier.service.SettingsService
import com.smhomelab.notifier.service.UserService
import io.github.dehuckakpyt.telegrambot.annotation.HandlerComponent
import io.github.dehuckakpyt.telegrambot.handler.BotHandler

@HandlerComponent
class StartHandler(
    private val auth: AuthorizationService,
    private val userService: UserService,
    private val invitationService: InvitationService,
    private val settingsService: SettingsService,
) : BotHandler({

        command(BotCommands.START.slashCommand) {
            val telegramId = from.id
            val username = from.username

            if (userService.existsByTelegramId(telegramId)) {
                userService.cleanupOrphanedInvitation(username)
                val pushoverHint = if (!settingsService.isPushoverConfigured(telegramId)) {
                    "\n\n💡 Настрой Pushover для получения уведомлений: <i>${BotCommands.SETTINGS.slashCommand}</i>"
                } else {
                    ""
                }
                sendMessage(
                    "Используй ${BotCommands.HELP.slashCommand} для списка команд.$pushoverHint",
                    parseMode = ParseMode.HTML,
                )
                return@command
            }

            if (username != null) {
                val invitation = invitationService.findByUsername(username)
                if (invitation != null) {
                    userService.activateFromInvitation(invitation, telegramId, from.firstName, from.lastName)
                    sendMessage(
                        "Твой аккаунт активирован.\n\n" +
                            "💡 Настрой Pushover для получения уведомлений: <i>${BotCommands.SETTINGS.slashCommand}</i>\n\n" +
                            "Используй ${BotCommands.HELP.slashCommand} для списка команд.",
                        parseMode = ParseMode.HTML,
                    )
                    return@command
                }
            }
        }

        secureCommand(BotCommands.HELP, auth) {
            val user = auth.getUser(from.id)
            val role = user?.role

            val commands = BotCommands
                .forRole(role)
                .joinToString("\n") { "${it.slashCommand} - ${it.description}" }

            sendMessage("Доступные команды:\n$commands")
        }
    })
