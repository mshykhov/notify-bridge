package com.smhomelab.notifier.bot

import com.smhomelab.notifier.persistence.model.UserRole
import io.github.dehuckakpyt.telegrambot.model.telegram.BotCommand

enum class BotCommands(
    val command: String,
    val description: String,
    val requiredRole: UserRole? = null
) {
    // Public
    START("start", "Начать работу"),

    // User
    HELP("help", "Показать справку", UserRole.USER),
    SETTINGS("settings", "Настройки уведомлений", UserRole.USER),

    // Admin
    LIST_USERS("list_users", "Список пользователей", UserRole.ADMIN),
    ADD_USER("add_user", "Добавить пользователя", UserRole.ADMIN),
    REMOVE_USER("remove_user", "Удалить пользователя", UserRole.ADMIN);

    val slashCommand: String get() = "/$command"

    fun toBotCommand() = BotCommand(command, description)

    companion object {
        fun all() = entries.map { it.toBotCommand() }

        fun forRole(role: UserRole?) = entries.filter { cmd ->
            cmd.requiredRole == null || (role != null && role.hasPermission(cmd.requiredRole))
        }
    }
}
