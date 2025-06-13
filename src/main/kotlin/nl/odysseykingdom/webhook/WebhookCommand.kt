package nl.odysseykingdom.webhook

import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

@Command("webhook", "wh")
class WebhookCommand(val plugin: IPlugin) {

    @CommandPermission("webhook.list")
    @Subcommand("list", "l")
    fun onList(actor: BukkitCommandActor) {
        actor.reply("Webhooks: ${plugin.webhooks.joinToString(", ")}")
    }

    @CommandPermission("webhook.reload")
    @Subcommand("reload", "r")
    fun onReload(actor: BukkitCommandActor) {
        plugin.reloadWebhooks()
        actor.reply("Reloaded webhooks.")
    }

    @CommandPermission("webhook.send")
    @Subcommand("send", "s")
    fun onSend(
        actor: BukkitCommandActor,
        webhook: Webhook,
        @Optional payload: String?
    ) {
        try {
            if (plugin.requiresMessage(webhook) && (payload == null || payload.isBlank())) {
                actor.reply("§cError: This webhook template requires a message. Please provide one.")
                return
            }

            actor.reply("Sending payload to webhook ${webhook.name}...")
            plugin.sendToWebhook(webhook, payload)
        } catch (e: IllegalArgumentException) {
            actor.reply("§cError: ${e.message}")
        } catch (e: Exception) {
            actor.reply("§cAn error occurred while sending the webhook: ${e.message}")
            e.printStackTrace()
        }
    }
}
