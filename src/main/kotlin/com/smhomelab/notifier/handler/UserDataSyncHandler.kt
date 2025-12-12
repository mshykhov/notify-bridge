package com.smhomelab.notifier.handler

import com.smhomelab.notifier.service.BotUserService
import io.github.dehuckakpyt.telegrambot.annotation.HandlerComponent
import io.github.dehuckakpyt.telegrambot.handler.BotUpdateHandler

@HandlerComponent
class UserDataSyncHandler(
    private val botUserService: BotUserService
) : BotUpdateHandler({

    message {
        botUserService.syncUserData(from.id, from.username, from.firstName, from.lastName)
    }

    callbackQuery {
        botUserService.syncUserData(from.id, from.username, from.firstName, from.lastName)
    }
})
