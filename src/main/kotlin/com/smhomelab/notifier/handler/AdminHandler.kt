package com.smhomelab.notifier.handler

import com.smhomelab.notifier.bot.BotCommands
import com.smhomelab.notifier.bot.secureCommand
import com.smhomelab.notifier.service.AuthorizationService
import com.smhomelab.notifier.service.BotUserService
import io.github.dehuckakpyt.telegrambot.annotation.HandlerComponent
import io.github.dehuckakpyt.telegrambot.handler.BotHandler

@HandlerComponent
class AdminHandler(
    private val auth: AuthorizationService,
    private val botUserService: BotUserService,
) : BotHandler({

    secureCommand(BotCommands.LIST_USERS, auth) {
        val users = botUserService.getAllUsers()

        if (users.isEmpty()) {
            sendMessage("Пользователей нет")
            return@secureCommand
        }

        val text = users.mapIndexed { i, user ->
            val username = user.username?.let { "@$it" } ?: "—"
            val name = listOfNotNull(user.firstName, user.lastName)
                .joinToString(" ")
                .ifEmpty { "—" }
            "${i + 1}. $username ($name) — ${user.role}"
        }.joinToString("\n")

        sendMessage("Пользователи:\n$text")
    }
})
