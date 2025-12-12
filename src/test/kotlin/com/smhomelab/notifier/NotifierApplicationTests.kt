package com.smhomelab.notifier

import com.smhomelab.notifier.config.TestcontainersConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestcontainersConfig::class)
class NotifierApplicationTests {

	@Test
	fun contextLoads() {
	}
}

