package com.smhomelab.notifier.model.pushover

enum class PushoverSound(
    val apiValue: String,
    val displayName: String,
) {
    PUSHOVER("pushover", "Pushover"),
    BIKE("bike", "Bike"),
    BUGLE("bugle", "Bugle"),
    CASH_REGISTER("cashregister", "Cash Register"),
    CLASSICAL("classical", "Classical"),
    COSMIC("cosmic", "Cosmic"),
    FALLING("falling", "Falling"),
    GAMELAN("gamelan", "Gamelan"),
    INCOMING("incoming", "Incoming"),
    INTERMISSION("intermission", "Intermission"),
    MAGIC("magic", "Magic"),
    MECHANICAL("mechanical", "Mechanical"),
    PIANO_BAR("pianobar", "Piano Bar"),
    SIREN("siren", "Siren"),
    SPACE_ALARM("spacealarm", "Space Alarm"),
    TUGBOAT("tugboat", "Tugboat"),
    ALIEN("alien", "Alien"),
    CLIMB("climb", "Climb"),
    PERSISTENT("persistent", "Persistent"),
    ECHO("echo", "Echo"),
    UP_DOWN("updown", "Up Down"),
    VIBRATE("vibrate", "Vibrate"),
    NONE("none", "None"),
    ;

    companion object {
        fun fromApiValue(value: String?): PushoverSound? =
            entries.find { it.apiValue == value }
    }
}
