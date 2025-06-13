package nl.odysseykingdom.webhook

import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.node.ExecutionContext
import revxrsal.commands.parameter.ParameterType
import revxrsal.commands.stream.MutableStringStream

class WebhookParameter(val plugin: WebhookPlugin) : ParameterType<BukkitCommandActor, Webhook> {

    override fun parse(
        input: MutableStringStream,
        context: ExecutionContext<BukkitCommandActor>
    ): Webhook? {
        return plugin.webhooks.find { it.name.equals(input.readString(), true) }
    }
}