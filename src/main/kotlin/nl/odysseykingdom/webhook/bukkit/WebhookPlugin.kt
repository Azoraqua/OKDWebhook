package nl.odysseykingdom.webhook.bukkit

import nl.odysseykingdom.webhook.*
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

    override fun onEnable() {
        super.saveDefaultConfig()
        this.reloadWebhooks()

        commandManager.register(WebhookCommand(this))
    }

    override fun onDisable() {
        commandManager.unregisterAllCommands()
    }

    override fun reloadWebhooks() {
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
}