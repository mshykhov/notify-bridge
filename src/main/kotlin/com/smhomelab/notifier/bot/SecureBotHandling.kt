package com.smhomelab.notifier.bot

import com.smhomelab.notifier.service.AuthorizationService
import io.github.dehuckakpyt.telegrambot.container.CallbackContainer
import io.github.dehuckakpyt.telegrambot.container.message.CommandContainer
import io.github.dehuckakpyt.telegrambot.container.message.TextMessageContainer
import io.github.dehuckakpyt.telegrambot.handling.BotHandling

fun BotHandling.secureCommand(
    cmd: BotCommands,
    auth: AuthorizationService,
    next: String? = null,
    action: suspend CommandContainer.() -> Unit
) {
    command(cmd.slashCommand, next) {
        if (!auth.isAuthorized(from.id, cmd.requiredRole)) return@command
        action()
    }
}

fun BotHandling.secureCallback(
    cb: BotCallbacks,
    auth: AuthorizationService,
    next: String? = null,
    action: suspend CallbackContainer.() -> Unit
) {
    callback(cb.callback, next) {
        if (!auth.isAuthorized(from.id, cb.requiredRole)) return@callback
        action()
    }
}

fun BotHandling.secureStep(
    step: BotSteps,
    auth: AuthorizationService,
    next: String? = null,
    action: suspend TextMessageContainer.() -> Unit
) {
    step(step.step, next) {
        if (!auth.isAuthorized(from.id, step.requiredRole)) return@step
        action()
    }
}
