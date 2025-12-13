package com.smhomelab.notifier.bot

import io.github.dehuckakpyt.telegrambot.model.type.LinkPreviewOptions

object ParseMode {
    const val HTML = "HTML"
    const val MARKDOWN = "Markdown"
    const val MARKDOWN_V2 = "MarkdownV2"
}

object LinkPreview {
    val DISABLED = LinkPreviewOptions(isDisabled = true)
}
