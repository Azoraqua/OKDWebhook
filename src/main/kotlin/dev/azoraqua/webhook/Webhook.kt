package dev.azoraqua.webhook

data class EmbedTemplate(
    val title: String? = null,
    val description: String? = null,
    val color: Int = 0x0000FF
)

typealias MessageTemplate = String

data class Webhook(
    val name: String, 
    val url: String,
    val messageTemplate: MessageTemplate? = null,
    val embedTemplate: EmbedTemplate? = null
)
