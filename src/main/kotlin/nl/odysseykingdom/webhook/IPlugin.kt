package nl.odysseykingdom.webhook

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import revxrsal.commands.Lamp
import java.util.logging.Logger

@OptIn(DelicateCoroutinesApi::class)
interface IPlugin {
    val commandManager: Lamp<*>
    val webhooks: MutableList<Webhook>
    val customLogger: Logger

    companion object {
        val GSON = Gson()
        val OK_HTTP = OkHttpClient()
    }

    fun reloadWebhooks()

    fun sendToWebhook(webhook: Webhook, payload: String?) = GlobalScope.launch {
        if (requiresMessage(webhook) && (payload == null || payload.isBlank())) {
            customLogger.warning("Webhook ${webhook.name} requires a message but none was provided")
            throw IllegalArgumentException("A message is required for this webhook template")
        }

        async {
            try {
                val jsonObject = JsonObject()
                val actualPayload = payload ?: ""

                // Check if the webhook URL is from Discord
                val isDiscordWebhook = webhook.url.contains("discord.com") || webhook.url.contains("discordapp.com")

                if (isDiscordWebhook) {
                    // For Discord webhooks, create an embed
                    val embedsArray = JsonArray()
                    val embed = JsonObject()

                    // Set embed properties
                    if (webhook.structuredEmbedTemplate != null) {
                        // Use the structured embed template if available
                        webhook.structuredEmbedTemplate.title?.let {
                            embed.addProperty("title", it)
                        }

                        webhook.structuredEmbedTemplate.description?.let {
                            embed.addProperty("description", it.replace("%message%", actualPayload))
                        } ?: embed.addProperty("description", actualPayload)

                        webhook.structuredEmbedTemplate.color?.let {
                            embed.addProperty("color", it)
                        } ?: embed.addProperty("color", 3447003) // Default Discord blue color
                    } else if (webhook.embedTemplate != null) {
                        // Use the string embed template if available
                        embed.addProperty("description", webhook.embedTemplate.replace("%message%", actualPayload))
                        embed.addProperty("color", 3447003) // Discord blue color
                    } else {
                        // Fall back to the default behavior
                        embed.addProperty("description", actualPayload)
                        embed.addProperty("color", 3447003) // Discord blue color
                    }

                    embedsArray.add(embed)
                    jsonObject.add("embeds", embedsArray)
                } else {
                    // For other webhooks, use the standard content field
                    if (webhook.messageTemplate != null) {
                        // Use the message template if available
                        jsonObject.addProperty("content", webhook.messageTemplate.replace("%message%", actualPayload))
                    } else {
                        // Fall back to the default behavior
                        jsonObject.addProperty("content", actualPayload)
                    }
                }

                val json = GSON.toJson(jsonObject)
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = json.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(webhook.url)
                    .post(requestBody)
                    .build()

                OK_HTTP.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        customLogger.warning("Failed to send webhook message to ${webhook.name}: ${response.code}")
                    } else {
                        customLogger.info("Successfully sent webhook message to ${webhook.name}")
                    }
                }
            } catch (e: Exception) {
                customLogger.severe("Error sending webhook message to ${webhook.name}: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Checks if the webhook template requires a message.
     *
     * @param webhook The webhook to check
     * @return true if the webhook template contains %message%, false otherwise
     */
    fun requiresMessage(webhook: Webhook): Boolean {
        // Check if any template contains %message%
        return (webhook.messageTemplate?.contains("%message%") == true) ||
                (webhook.embedTemplate?.contains("%message%") == true) ||
                (webhook.structuredEmbedTemplate?.description?.contains("%message%") == true)
    }
}