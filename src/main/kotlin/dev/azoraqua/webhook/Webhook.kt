package dev.azoraqua.webhook

data class EmbedTemplate(
    val title: String? = null,
    val description: String? = null,
    val color: Int? = null
)

data class Webhook(
    val name: String, 
    val url: String,
    val messageTemplate: String? = null,
    val embedTemplate: String? = null,
    val structuredEmbedTemplate: EmbedTemplate? = null
)
