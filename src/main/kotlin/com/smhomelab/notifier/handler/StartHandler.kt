package com.smhomelab.notifier.handler

import com.smhomelab.notifier.bot.BotCommands
import io.github.dehuckakpyt.telegrambot.handler.BotHandler
import io.github.dehuckakpyt.telegrambot.annotation.HandlerComponent

@HandlerComponent
class StartHandler : BotHandler({

    command(BotCommands.START.slashCommand) {
        sendMessage("Привет! Я notifier бот. Используй /help для списка команд.")
    }

    command(BotCommands.HELP.slashCommand) {
        val commandsList = BotCommands.entries.joinToString("\n") {
            "${it.slashCommand} - ${it.description}"
        }
        sendMessage("Доступные команды:\n$commandsList")
    }
})
