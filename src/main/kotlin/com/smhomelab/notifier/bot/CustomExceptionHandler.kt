package com.smhomelab.notifier.bot

import io.github.dehuckakpyt.telegrambot.TelegramBot
import io.github.dehuckakpyt.telegrambot.exception.api.TelegramBotApiException
import io.github.dehuckakpyt.telegrambot.exception.handler.ExceptionHandlerImpl
import io.github.dehuckakpyt.telegrambot.model.telegram.CallbackQuery
import io.github.dehuckakpyt.telegrambot.model.telegram.Chat
import io.github.dehuckakpyt.telegrambot.template.MessageTemplate
import io.github.dehuckakpyt.telegrambot.template.Templater
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class CustomExceptionHandler(
    bot: TelegramBot,
    template: MessageTemplate,
    templater: Templater,
) : ExceptionHandlerImpl(bot, template, templater) {

    override suspend fun caught(chat: Chat, ex: Throwable) {
        if (isMessageNotModified(ex)) {
            logger.debug { "Message not modified (content unchanged), ignoring" }
            return
        }
        super.caught(chat, ex)
    }

    override suspend fun executeCallback(callback: CallbackQuery, block: suspend () -> Unit) {
        try {
            block()
        } catch (ex: Throwable) {
            if (isMessageNotModified(ex)) {
                logger.debug { "Message not modified (content unchanged), ignoring" }
                return
            }
            caught(callback.message!!.chat, ex)
        }
    }

    private fun isMessageNotModified(ex: Throwable): Boolean =
        ex is TelegramBotApiException && "message is not modified" in ex.message.orEmpty()
}
