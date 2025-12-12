package com.smhomelab.notifier.handler

import com.smhomelab.notifier.bot.BotCommands
import io.github.dehuckakpyt.telegrambot.handler.BotHandler
import io.github.dehuckakpyt.telegrambot.annotation.HandlerComponent
import io.github.dehuckakpyt.telegrambot.factory.keyboard.inlineKeyboard

@HandlerComponent
class SettingsHandler : BotHandler({

    command(BotCommands.SETTINGS.slashCommand) {
        sendMessage(
            "Выбери что настроить:",
            replyMarkup = inlineKeyboard(
                callbackButton("🔔 Уведомления", "settings:notifications"),
                callbackButton("⏰ Время тишины", "settings:quiet_hours"),
                callbackButton("❌ Отмена", "settings:cancel")
            )
        )
    }

    callback("settings:notifications") {
        sendMessage(
            "Какие уведомления включить?",
            replyMarkup = inlineKeyboard(
                callbackButton("✅ Все", "notify:all"),
                callbackButton("⚠️ Только важные", "notify:important"),
                callbackButton("🔕 Выключить", "notify:off")
            )
        )
    }

    callback("notify:all") {
        sendMessage("✅ Все уведомления включены")
    }

    callback("notify:important") {
        sendMessage("⚠️ Только важные уведомления включены")
    }

    callback("notify:off") {
        sendMessage("🔕 Уведомления выключены")
    }

    callback("settings:quiet_hours", next = "get_quiet_start") {
        sendMessage("Введи время начала тишины (например: 22:00):")
    }

    step("get_quiet_start", next = "get_quiet_end") {
        val startTime = text
        sendMessage("Время начала: $startTime\nТеперь введи время окончания (например: 08:00):")
        transfer(startTime)
    }

    step("get_quiet_end") {
        val startTime = transferred<String>()
        val endTime = text
        sendMessage("⏰ Время тишины установлено: $startTime - $endTime")
    }

    callback("settings:cancel") {
        sendMessage("Настройки закрыты")
    }
})
