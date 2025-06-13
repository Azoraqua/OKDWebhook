package nl.odysseykingdom.webhook

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.bukkit.plugin.java.JavaPlugin
import revxrsal.commands.bukkit.BukkitLamp

class WebhookPlugin : JavaPlugin() {
    private companion object {
        val GSON = Gson()
        val OK_HTTP = OkHttpClient()
    }

    private val commandManager by lazy {
        BukkitLamp.builder(this)
            .parameterTypes {
                it.addParameterType(
                    Webhook::class.java,
                    WebhookParameter(this)
                )
            }
            .suggestionProviders {
                it.addProvider(
                    Webhook::class.java,
                    WebhookSuggestionProvider(this)
                )
            }
            .build()
    }
    internal val webhooks: MutableList<Webhook> = mutableListOf()

    override fun onEnable() {
        super.saveDefaultConfig()
        this.reloadWebhooks()

        commandManager.register(WebhookCommand(this))
    }

    override fun onDisable() {
        commandManager.unregisterAllCommands()
    }

    fun reloadWebhooks() {
        // Clear existing webhooks before loading new ones
        webhooks.clear()

        val root = config.getConfigurationSection("webhooks")

        root?.getKeys(false)?.forEach { webhook ->
            val url = root.getString("${webhook}.url") ?: return@forEach
            val name = root.getString("${webhook}.name") ?: webhook
            val messageTemplate = root.getString("${webhook}.messageTemplate")
            val embedTemplate = root.getString("${webhook}.embedTemplate")

            // Check for structured embed template
            val embedSection = root.getConfigurationSection("${webhook}.embedTemplate")
            val structuredEmbedTemplate = if (embedSection != null) {
                val title = embedSection.getString("title")
                val description = embedSection.getString("description")
                val color = if (embedSection.contains("color")) embedSection.getInt("color") else null

                EmbedTemplate(title, description, color)
            } else {
                null
            }

            webhooks.add(Webhook(name, url, messageTemplate, embedTemplate, structuredEmbedTemplate))
            logger.info("Loaded webhook '$name' with URL '$url'")
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

    fun sendToWebhook(webhook: Webhook, payload: String?) {
        // Check if the webhook requires a message but none was provided
        if (requiresMessage(webhook) && (payload == null || payload.isBlank())) {
            logger.warning("Webhook ${webhook.name} requires a message but none was provided")
            throw IllegalArgumentException("A message is required for this webhook template")
        }

        // Run the HTTP request in a separate thread to avoid blocking the main server thread
        server.scheduler.runTaskAsynchronously(this, Runnable {
            try {
                val jsonObject = JsonObject()
                val actualPayload = payload ?: ""

                // Check if the webhook URL is from Discord
                val isDiscordWebhook = webhook.url.contains("discord.com") || webhook.url.contains("discordapp.com")

                if (isDiscordWebhook) {
                    // For Discord webhooks, create an embed
                    val embedsArray = com.google.gson.JsonArray()
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
                        logger.warning("Failed to send webhook message to ${webhook.name}: ${response.code}")
                    } else {
                        logger.info("Successfully sent webhook message to ${webhook.name}")
                    }
                }
            } catch (e: Exception) {
                logger.severe("Error sending webhook message to ${webhook.name}: ${e.message}")
                e.printStackTrace()
            }
        })
    }
}
