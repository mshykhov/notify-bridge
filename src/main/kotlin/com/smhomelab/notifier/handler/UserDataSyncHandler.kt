package com.smhomelab.notifier.handler

import com.smhomelab.notifier.service.UserService
import io.github.dehuckakpyt.telegrambot.annotation.HandlerComponent
import io.github.dehuckakpyt.telegrambot.handler.BotUpdateHandler
import io.github.dehuckakpyt.telegrambot.model.telegram.User

@HandlerComponent
class UserDataSyncHandler(
    private val userService: UserService
) : BotUpdateHandler({

    fun sync(from: User) {
        userService.syncUserData(from.id, from.username, from.firstName, from.lastName)
    }

    message { from?.let { sync(it) } }
    editedMessage { from?.let { sync(it) } }
    callbackQuery { sync(from) }
    inlineQuery { sync(from) }
    chosenInlineResult { sync(from) }
    myChatMember { sync(from) }
    chatMember { sync(from) }
    chatJoinRequest { sync(from) }
})
