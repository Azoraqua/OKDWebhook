package nl.odysseykingdom.webhook.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.coroutines.Job
import nl.odysseykingdom.webhook.*
import revxrsal.commands.Lamp
import revxrsal.commands.velocity.VelocityLamp
import revxrsal.commands.velocity.actor.VelocityCommandActor
import java.io.File
import java.io.FileOutputStream
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
    override val configFile: File by lazy { dataDirectory.resolve("config.yml").toFile() }

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
            javaClass.classLoader.getResourceAsStream("config.yml")?.use { inputStream ->
                FileOutputStream(configFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                    customLogger.info("Default config.yml has been saved.")
                }
            } ?: customLogger.warning("Could not find default config.yml in resources.")
        } catch (e: Exception) {
            customLogger.severe("Failed to save default config.yml: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun reloadWebhooks() {
        WebhookUtils.reloadWebhooks(this)
    }

    override fun sendToWebhook(webhook: Webhook, payload: String?): Job {
        return WebhookUtils.sendToWebhook(this, webhook, payload)
    }
}
