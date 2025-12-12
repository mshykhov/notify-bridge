package com.smhomelab.notifier

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class NotifierApplication

fun main(args: Array<String>) {
	runApplication<NotifierApplication>(*args)
}
