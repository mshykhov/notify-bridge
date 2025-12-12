package com.smhomelab.notifier.handler

import com.smhomelab.notifier.service.BotUserService
import io.github.dehuckakpyt.telegrambot.annotation.HandlerComponent
import io.github.dehuckakpyt.telegrambot.handler.BotUpdateHandler
import io.github.dehuckakpyt.telegrambot.model.telegram.User

@HandlerComponent
class UserDataSyncHandler(
    private val botUserService: BotUserService
) : BotUpdateHandler({

    fun sync(from: User) {
        botUserService.syncUserData(from.id, from.username, from.firstName, from.lastName)
    }

    message { sync(from) }
    editedMessage { sync(from) }
    callbackQuery { sync(from) }
    inlineQuery { sync(from) }
    chosenInlineResult { sync(from) }
    myChatMember { sync(from) }
    chatMember { sync(from) }
    chatJoinRequest { sync(from) }
})
