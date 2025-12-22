package com.smhomelab.notifier.api

enum class PushoverSound(
    val value: String,
) {
    PUSHOVER("pushover"),
    BIKE("bike"),
    BUGLE("bugle"),
    CASH_REGISTER("cashregister"),
    CLASSICAL("classical"),
    COSMIC("cosmic"),
    FALLING("falling"),
    GAMELAN("gamelan"),
    INCOMING("incoming"),
    INTERMISSION("intermission"),
    MAGIC("magic"),
    MECHANICAL("mechanical"),
    PIANO_BAR("pianobar"),
    SIREN("siren"),
    SPACE_ALARM("spacealarm"),
    TUG_BOAT("tugboat"),
    ALIEN("alien"),
    CLIMB("climb"),
    PERSISTENT("persistent"),
    ECHO("echo"),
    UP_DOWN("updown"),
    VIBRATE("vibrate"),
    NONE("none"),
    ;

    companion object {
        private val valueMap = entries.associateBy { it.value }

        fun fromValue(value: String): PushoverSound? = valueMap[value.lowercase()]
    }
}
