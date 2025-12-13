package com.smhomelab.notifier.service

import com.smhomelab.notifier.persistence.model.UserRole
import org.springframework.stereotype.Service

@Service
class AdminService(
    private val botUserService: BotUserService,
    private val invitationService: InvitationService
) {
    fun getUsersListText(): String {
        val users = botUserService.getAllUsers()
        val invitations = invitationService.getAll()

        val usersText = if (users.isEmpty()) {
            "Нет зарегистрированных пользователей"
        } else {
            users.mapIndexed { i, user ->
                val username = user.username?.let { "@$it" } ?: "—"
                val name = listOfNotNull(user.firstName, user.lastName)
                    .joinToString(" ").ifEmpty { "—" }
                "${i + 1}. $username ($name) — ${user.role}"
            }.joinToString("\n")
        }

        val invitationsText = if (invitations.isEmpty()) {
            ""
        } else {
            "\n\nПриглашения:\n" + invitations.mapIndexed { i, inv ->
                "${i + 1}. @${inv.username} — ${inv.role}"
            }.joinToString("\n")
        }

        return "Пользователи:\n$usersText$invitationsText"
    }

    fun validateTelegramId(input: String): ValidationResult<Long> {
        val telegramId = input.trim().toLongOrNull()
            ?: return ValidationResult.Invalid("Неверный формат ID. Введи число:")

        if (botUserService.existsByTelegramId(telegramId)) {
            return ValidationResult.Invalid("Пользователь с таким ID уже существует")
        }

        return ValidationResult.Valid(telegramId)
    }

    fun validateUsername(input: String): ValidationResult<String> {
        val username = input.trim().removePrefix("@").lowercase()

        if (username.isEmpty()) {
            return ValidationResult.Invalid("Username не может быть пустым. Введи ещё раз:")
        }

        if (botUserService.existsByUsername(username) || invitationService.existsByUsername(username)) {
            return ValidationResult.Invalid("Пользователь или приглашение с таким username уже существует")
        }

        return ValidationResult.Valid(username)
    }

    suspend fun addUserById(telegramId: Long, role: UserRole): String {
        botUserService.createUser(telegramId, role)
        return "Пользователь с ID $telegramId добавлен с ролью $role"
    }

    suspend fun addUserByUsername(username: String, role: UserRole, createdBy: Long): String {
        invitationService.create(username, role, createdBy)
        return "Приглашение для @$username создано с ролью $role.\nПользователь получит доступ после /start"
    }

    suspend fun addUser(data: String, role: UserRole, createdBy: Long): String {
        return when {
            data.startsWith("id:") -> {
                val telegramId = data.removePrefix("id:").toLong()
                addUserById(telegramId, role)
            }
            data.startsWith("username:") -> {
                val username = data.removePrefix("username:")
                addUserByUsername(username, role, createdBy)
            }
            else -> "Ошибка: неверный формат данных"
        }
    }

    suspend fun removeUser(telegramId: Long): String {
        botUserService.deleteUser(telegramId)
        return "Пользователь удалён"
    }

    fun removeInvitation(username: String): String {
        invitationService.delete(username)
        return "Приглашение для @$username удалено"
    }

    fun hasUsersOrInvitations(): Boolean {
        return botUserService.getAllUsers().isNotEmpty() || invitationService.getAll().isNotEmpty()
    }
}

sealed class ValidationResult<out T> {
    data class Valid<T>(val value: T) : ValidationResult<T>()
    data class Invalid(val error: String) : ValidationResult<Nothing>()
}
