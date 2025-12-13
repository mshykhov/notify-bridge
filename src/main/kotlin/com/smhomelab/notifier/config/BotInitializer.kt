package com.smhomelab.notifier.config

import com.smhomelab.notifier.service.BotCommandMenuService
import com.smhomelab.notifier.service.UserService
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Configuration

@Configuration
class BotInitializer(
    private val botCommandMenuService: BotCommandMenuService,
    private val userService: UserService,
) {
    @PostConstruct
    fun init() = runBlocking {
        userService.ensureMasterAdminExists()
        botCommandMenuService.initializeAllCommands()
    }
}
