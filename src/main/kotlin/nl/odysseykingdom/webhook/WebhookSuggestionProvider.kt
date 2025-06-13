package nl.odysseykingdom.webhook

import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.command.CommandActor
import revxrsal.commands.node.ExecutionContext

class WebhookSuggestionProvider<A : CommandActor>(val plugin: IPlugin) : SuggestionProvider<A> {
    override fun getSuggestions(context: ExecutionContext<A>): Collection<String> {
        return plugin.webhooks.map { it.name }
    }
}