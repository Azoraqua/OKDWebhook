package nl.odysseykingdom.webhook.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import nl.odysseykingdom.webhook.*
import org.yaml.snakeyaml.Yaml
import revxrsal.commands.Lamp
import revxrsal.commands.velocity.VelocityLamp
import revxrsal.commands.velocity.actor.VelocityCommandActor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Path
import java.util.logging.Logger

@Plugin(id = "okdwebhook", name = "OKDWebhook", version = "1.0-SNAPSHOT")
class WebhookPlugin @Inject constructor(
    val server: ProxyServer,
    override val customLogger: Logger,
    @DataDirectory private val dataDirectory: Path
) : IPlugin {
    override val commandManager: Lamp<*> by lazy { 
        VelocityLamp.builder(this, server)
            .parameterTypes {
                it.addParameterType(
                    Webhook::class.java,
                    WebhookParameter<VelocityCommandActor>(this)
                )
            }
            .suggestionProviders {
                it.addProvider(
                    Webhook::class.java,
                    WebhookSuggestionProvider<VelocityCommandActor>(this)
                )
            }
            .build() 
    }
    override val webhooks: MutableList<Webhook> = mutableListOf()
    private val configFile: File by lazy { dataDirectory.resolve("config.yml").toFile() }

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        // Create data directory if it doesn't exist
        if (!dataDirectory.toFile().exists()) {
            dataDirectory.toFile().mkdirs()
        }

        // Save default config if it doesn't exist
        if (!configFile.exists()) {
            saveDefaultConfig()
        }

        // Load webhooks from config
        reloadWebhooks()

        // Register commands
        commandManager.register(WebhookCommand(this))
    }

    /**
     * Saves the default config.yml file to the plugin's data directory
     */
    private fun saveDefaultConfig() {
        try {
            val inputStream = javaClass.classLoader.getResourceAsStream("config.yml")
            if (inputStream != null) {
                val outputStream = FileOutputStream(configFile)
                inputStream.copyTo(outputStream)
                outputStream.close()
                inputStream.close()
                customLogger.info("Default config.yml has been saved.")
            } else {
                customLogger.warning("Could not find default config.yml in resources.")
            }
        } catch (e: Exception) {
            customLogger.severe("Failed to save default config.yml: ${e.message}")
            e.printStackTrace()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun reloadWebhooks() {
        // Clear existing webhooks before loading new ones
        webhooks.clear()

        try {
            val inputStream: InputStream = FileInputStream(configFile)
            val yaml = Yaml()
            val config = yaml.load<Map<String, Any>>(inputStream)

            val webhooksSection = config["webhooks"] as? Map<String, Any> ?: return

            webhooksSection.forEach { (webhookKey, webhookValue) ->
                if (webhookValue is Map<*, *>) {
                    val webhookMap = webhookValue as Map<String, Any>

                    val url = webhookMap["url"] as? String ?: return@forEach
                    val name = webhookMap["name"] as? String ?: webhookKey
                    val messageTemplate = webhookMap["messageTemplate"] as? String
                    val embedTemplate = webhookMap["embedTemplate"] as? String

                    // Check for structured embed template
                    val embedSection = webhookMap["embedTemplate"] as? Map<String, Any>
                    val structuredEmbedTemplate = if (embedSection != null) {
                        val title = embedSection["title"] as? String
                        val description = embedSection["description"] as? String
                        val color = if (embedSection.containsKey("color")) embedSection["color"] as? Int else null

                        EmbedTemplate(title, description, color)
                    } else {
                        null
                    }

                    webhooks.add(Webhook(name, url, messageTemplate, embedTemplate, structuredEmbedTemplate))
                    customLogger.info("Loaded webhook '$name' with URL '$url'")
                }
            }

            inputStream.close()
        } catch (e: Exception) {
            customLogger.severe("Failed to load webhooks from config.yml: ${e.message}")
            e.printStackTrace()
        }
    }
}
