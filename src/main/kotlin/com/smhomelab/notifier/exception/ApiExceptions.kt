package com.smhomelab.notifier.exception

class ChannelDisabledException(
    val channel: String,
    override val message: String = "Channel '$channel' is disabled",
) : RuntimeException(message)

class ConfigurationMissingException(
    override val message: String,
) : RuntimeException(message)

class SendFailedException(
    val channel: String,
    override val message: String,
) : RuntimeException(message)
