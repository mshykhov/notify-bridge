package com.smhomelab.notifier

import com.smhomelab.notifier.config.TestcontainersConfig
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@Tag("integration")
@SpringBootTest
@Import(TestcontainersConfig::class)
class NotifierApplicationTests {
    @Test
    fun contextLoads() {
    }
}
