package nl.odysseykingdom.webhook

import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.node.ExecutionContext

class WebhookSuggestionProvider(val plugin: WebhookPlugin) : SuggestionProvider<BukkitCommandActor> {
    override fun getSuggestions(context: ExecutionContext<BukkitCommandActor>): Collection<String> {
        return plugin.webhooks.map { it.name }
    }

}