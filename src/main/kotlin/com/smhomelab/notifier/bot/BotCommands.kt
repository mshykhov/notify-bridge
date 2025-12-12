package com.smhomelab.notifier.bot

import io.github.dehuckakpyt.telegrambot.model.telegram.BotCommand

enum class BotCommands(val command: String, val description: String) {
    START("start", "Начать работу"),
    HELP("help", "Показать справку"),
    SETTINGS("settings", "Настройки уведомлений");

    val slashCommand: String get() = "/$command"

    fun toBotCommand() = BotCommand(command, description)

    companion object {
        fun all() = entries.map { it.toBotCommand() }
    }
}
