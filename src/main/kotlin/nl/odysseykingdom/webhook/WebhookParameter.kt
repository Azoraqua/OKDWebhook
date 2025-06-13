package nl.odysseykingdom.webhook

import revxrsal.commands.command.CommandActor
import revxrsal.commands.node.ExecutionContext
import revxrsal.commands.parameter.ParameterType
import revxrsal.commands.stream.MutableStringStream

class WebhookParameter<A : CommandActor>(val plugin: IPlugin) : ParameterType<A, Webhook> {
    override fun parse(
        input: MutableStringStream,
        context: ExecutionContext<A>
    ): Webhook? {
        return plugin.webhooks.find { it.name.equals(input.readString(), ignoreCase = true) }
    }
}
