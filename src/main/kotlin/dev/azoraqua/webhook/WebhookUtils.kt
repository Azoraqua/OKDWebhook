package dev.azoraqua.webhook

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.yaml.snakeyaml.Yaml
import java.io.FileInputStream

/**
 * Utility class for webhook operations.
 */
@OptIn(DelicateCoroutinesApi::class)
object WebhookUtils {
    private val GSON = Gson()
    private val OK_HTTP = OkHttpClient()
    private val YAML = Yaml()

    /**
     * Reloads webhooks from the config file.
     *
     * @param plugin The plugin instance
     */
    @Suppress("UNCHECKED_CAST")
    fun reloadWebhooks(plugin: IPlugin) {
        // Clear existing webhooks before loading new ones
        plugin.webhooks.clear()

        try {
            FileInputStream(plugin.configFile).use { inputStream ->
                val config = YAML.load<Map<String, Any>>(inputStream)
                val webhooksSection = config["webhooks"] as? Map<String, Any> ?: return

                webhooksSection.forEach { (webhookKey, webhookValue) ->
                    (webhookValue as? Map<*, *>)?.let { webhookMap ->
                        val url = webhookMap["url"] as? String ?: return@forEach
                        val name = webhookMap["name"] as? String ?: webhookKey
                        val messageTemplate = webhookMap["messageTemplate"] as? String
                        val embedTemplate = webhookMap["embedTemplate"] as? String

                        // Check for structured embed template
                        val structuredEmbedTemplate = (webhookMap["embedTemplate"] as? Map<String, Any>)?.let { embedSection ->
                            val title = embedSection["title"] as? String
                            val description = embedSection["description"] as? String
                            val color = if (embedSection.containsKey("color")) embedSection["color"] as? Int else null

                            EmbedTemplate(title, description, color)
                        }

                        plugin.webhooks.add(Webhook(name, url, messageTemplate, embedTemplate, structuredEmbedTemplate))
                        plugin.customLogger.info("Loaded webhook '$name' with URL '$url'")
                    }
                }
            }
        } catch (e: Exception) {
            plugin.customLogger.severe("Failed to load webhooks from config.yml: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Sends a payload to a webhook.
     *
     * @param plugin The plugin instance
     * @param webhook The webhook to send the payload to
     * @param payload The payload to send
     * @return A Job that can be used to track the completion of the operation
     */
    fun sendToWebhook(plugin: IPlugin, webhook: Webhook, payload: String?) = CoroutineScope(Dispatchers.IO).launch {
        if (requiresMessage(webhook) && payload.isNullOrBlank()) {
            plugin.customLogger.warning("Webhook ${webhook.name} requires a message but none was provided")
            throw IllegalArgumentException("A message is required for this webhook template")
        }

        try {
            val jsonObject = JsonObject()
            val actualPayload = payload.orEmpty()

            // Check if the webhook URL is from Discord
            val isDiscordWebhook = webhook.url.contains("discord.com") || webhook.url.contains("discordapp.com")

            if (isDiscordWebhook) {
                // For Discord webhooks, create an embed
                val embedsArray = JsonArray()
                val embed = JsonObject()

                // Set embed properties
                webhook.structuredEmbedTemplate?.let { template ->
                    // Use the structured embed template if available
                    template.title?.let {
                        embed.addProperty("title", it)
                    }

                    template.description?.let {
                        embed.addProperty("description", it.replace("%message%", actualPayload))
                    } ?: embed.addProperty("description", actualPayload)

                    template.color?.let {
                        embed.addProperty("color", it)
                    } ?: embed.addProperty("color", 3447003) // Default Discord blue color
                } ?: webhook.embedTemplate?.let {
                    // Use the string embed template if available
                    embed.addProperty("description", it.replace("%message%", actualPayload))
                    embed.addProperty("color", 3447003) // Discord blue color
                } ?: run {
                    // Fall back to the default behavior
                    embed.addProperty("description", actualPayload)
                    embed.addProperty("color", 3447003) // Discord blue color
                }

                embedsArray.add(embed)
                jsonObject.add("embeds", embedsArray)
            } else {
                // For other webhooks, use the standard content field
                webhook.messageTemplate?.let {
                    // Use the message template if available
                    jsonObject.addProperty("content", it.replace("%message%", actualPayload))
                } ?: jsonObject.addProperty("content", actualPayload)
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
                    plugin.customLogger.warning("Failed to send webhook message to ${webhook.name}: ${response.code}")
                } else {
                    plugin.customLogger.info("Successfully sent webhook message to ${webhook.name}")
                }
            }
        } catch (e: Exception) {
            plugin.customLogger.severe("Error sending webhook message to ${webhook.name}: ${e.message}")
            e.printStackTrace()
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
        return webhook.messageTemplate?.contains("%message%") == true ||
               webhook.embedTemplate?.contains("%message%") == true ||
               webhook.structuredEmbedTemplate?.description?.contains("%message%") == true
    }
}