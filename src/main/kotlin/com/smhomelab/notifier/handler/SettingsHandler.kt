package com.smhomelab.notifier.handler

import com.smhomelab.notifier.bot.BotCallbacks
import com.smhomelab.notifier.bot.BotCommands
import com.smhomelab.notifier.bot.BotSteps
import com.smhomelab.notifier.bot.secureCallback
import com.smhomelab.notifier.bot.secureCommand
import com.smhomelab.notifier.bot.secureStep
import com.smhomelab.notifier.service.AuthorizationService
import io.github.dehuckakpyt.telegrambot.annotation.HandlerComponent
import io.github.dehuckakpyt.telegrambot.factory.keyboard.inlineKeyboard
import io.github.dehuckakpyt.telegrambot.handler.BotHandler

@HandlerComponent
class SettingsHandler(
    private val auth: AuthorizationService,
) : BotHandler({

    secureCommand(BotCommands.SETTINGS, auth) {
        sendMessage(
            "Выбери что настроить:",
            replyMarkup = inlineKeyboard(
                callbackButton("Уведомления", BotCallbacks.SETTINGS_NOTIFICATIONS.callback),
                callbackButton("Время тишины", BotCallbacks.SETTINGS_QUIET_HOURS.callback),
                callbackButton("Отмена", BotCallbacks.SETTINGS_CANCEL.callback)
            )
        )
    }

    secureCallback(BotCallbacks.SETTINGS_NOTIFICATIONS, auth) {
        sendMessage(
            "Какие уведомления включить?",
            replyMarkup = inlineKeyboard(
                callbackButton("Все", BotCallbacks.NOTIFY_ALL.callback),
                callbackButton("Только важные", BotCallbacks.NOTIFY_IMPORTANT.callback),
                callbackButton("Выключить", BotCallbacks.NOTIFY_OFF.callback)
            )
        )
    }

    secureCallback(BotCallbacks.NOTIFY_ALL, auth) {
        sendMessage("Все уведомления включены")
    }

    secureCallback(BotCallbacks.NOTIFY_IMPORTANT, auth) {
        sendMessage("Только важные уведомления включены")
    }

    secureCallback(BotCallbacks.NOTIFY_OFF, auth) {
        sendMessage("Уведомления выключены")
    }

    secureCallback(BotCallbacks.SETTINGS_QUIET_HOURS, auth, next = BotSteps.GET_QUIET_START.step) {
        sendMessage("Введи время начала тишины (например: 22:00):")
    }

    secureStep(BotSteps.GET_QUIET_START, auth, next = BotSteps.GET_QUIET_END.step) {
        val startTime = text
        sendMessage("Время начала: $startTime\nТеперь введи время окончания (например: 08:00):")
        transfer(startTime)
    }

    secureStep(BotSteps.GET_QUIET_END, auth) {
        val startTime = transferred<String>()
        val endTime = text
        sendMessage("Время тишины установлено: $startTime - $endTime")
    }

    secureCallback(BotCallbacks.SETTINGS_CANCEL, auth) {
        sendMessage("Настройки закрыты")
    }
})
