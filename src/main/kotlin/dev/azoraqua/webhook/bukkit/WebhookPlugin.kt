package dev.azoraqua.webhook.bukkit

import dev.azoraqua.webhook.*
import kotlinx.coroutines.Job
import org.bukkit.plugin.java.JavaPlugin
import revxrsal.commands.Lamp
import revxrsal.commands.bukkit.BukkitLamp
import revxrsal.commands.bukkit.actor.BukkitCommandActor

class WebhookPlugin : JavaPlugin(), IPlugin {
    override val commandManager: Lamp<BukkitCommandActor> by lazy {
        BukkitLamp.builder(this)
            .parameterTypes {
                it.addParameterType(
                    Webhook::class.java,
                    WebhookParameter<BukkitCommandActor>(this)
                )
            }
            .suggestionProviders {
                it.addProvider(
                    Webhook::class.java,
                    WebhookSuggestionProvider<BukkitCommandActor>(this)
                )
            }
            .build()
    }
    override val webhooks: MutableList<Webhook> = mutableListOf()
    override val customLogger = super.logger
    override val configFile = dataFolder.resolve("config.yml")

    override fun onEnable() {
        super.saveDefaultConfig()
        reloadWebhooks()

        commandManager.register(WebhookCommand(this))
    }

    override fun onDisable() {
        commandManager.unregisterAllCommands()
    }

    override fun reloadWebhooks() {
        WebhookUtils.reloadWebhooks(this)
    }

    override fun sendToWebhook(webhook: Webhook, payload: String?): Job {
        return WebhookUtils.sendToWebhook(this, webhook, payload)
    }
}
