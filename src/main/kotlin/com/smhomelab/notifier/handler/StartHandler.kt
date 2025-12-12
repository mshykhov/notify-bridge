package com.smhomelab.notifier.handler

import com.smhomelab.notifier.bot.BotCommands
import com.smhomelab.notifier.bot.secureCommand
import com.smhomelab.notifier.service.AuthorizationService
import com.smhomelab.notifier.service.BotUserService
import com.smhomelab.notifier.service.InvitationService
import io.github.dehuckakpyt.telegrambot.annotation.HandlerComponent
import io.github.dehuckakpyt.telegrambot.handler.BotHandler

@HandlerComponent
class StartHandler(
    private val auth: AuthorizationService,
    private val botUserService: BotUserService,
    private val invitationService: InvitationService,
) : BotHandler({

    command(BotCommands.START.slashCommand) {
        val telegramId = from.id
        val username = from.username

        if (auth.isMasterAdmin(telegramId)) {
            sendMessage("Используй /help для списка команд.")
            return@command
        }

        if (botUserService.existsByTelegramId(telegramId)) {
            botUserService.updateUserInfo(telegramId, username, from.firstName, from.lastName)
            botUserService.cleanupOrphanedInvitation(username)
            sendMessage("Используй /help для списка команд.")
            return@command
        }

        if (username != null) {
            val invitation = invitationService.findByUsername(username)
            if (invitation != null) {
                botUserService.activateFromInvitation(invitation, telegramId, from.firstName, from.lastName)
                sendMessage("Твой аккаунт активирован. Используй /help для списка команд.")
                return@command
            }
        }

        sendMessage("У тебя нет доступа к боту. Обратись к администратору.")
    }

    secureCommand(BotCommands.HELP, auth) {
        val user = auth.getUser(from.id)
        val role = user?.role

        val commands = BotCommands.forRole(role)
            .joinToString("\n") { "${it.slashCommand} - ${it.description}" }

        sendMessage("Доступные команды:\n$commands")
    }
})
