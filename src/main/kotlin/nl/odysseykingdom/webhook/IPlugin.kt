package nl.odysseykingdom.webhook

import kotlinx.coroutines.Job
import revxrsal.commands.Lamp
import java.io.File
import java.util.logging.Logger

interface IPlugin {
    val commandManager: Lamp<*>
    val webhooks: MutableList<Webhook>
    val customLogger: Logger
    val configFile: File

    /**
     * Reloads webhooks from the config file.
     */
    fun reloadWebhooks()

    /**
     * Sends a payload to a webhook.
     *
     * @param webhook The webhook to send the payload to
     * @param payload The payload to send
     * @return A Job that can be used to track the completion of the operation
     */
    fun sendToWebhook(webhook: Webhook, payload: String?): Job
}
