import org.apache.unomi.api.actions.Action
import org.apache.unomi.api.actions.ActionExecutor
import org.apache.unomi.api.Event
import org.apache.unomi.groovy.actions.annotations.Action as GAction
import org.apache.unomi.groovy.actions.annotations.Parameter
import org.apache.unomi.api.services.EventService

import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.Instant

@GAction(
    id = "notifyOnPageViewMilestoneAction",
    actionExecutor = "groovy:notifyOnPageViewMilestoneAction",
    parameters = [
        @Parameter(id = "webhookUrl", type = "string", multivalued = false),
        @Parameter(id = "threshold", type = "integer", multivalued = false)
    ]
)
def execute() {
    try {
        def webhookUrl = action.parameterValues.get("webhookUrl") ?: "http://host.docker.internal:3001/unomi-hook"
        def threshold = (action.parameterValues.get("threshold") ?: 10) as int

        def profileObj = event.profile
        def rawCount = profileObj?.properties?.pageViewCount
        if (rawCount == null) {
            return EventService.NO_CHANGE
        }
        int count = (rawCount as Number).intValue()

        if (count <= 0 || count % threshold != 0) {
            return EventService.NO_CHANGE
        }

        def payload = '{"count":' + count +
            ',"profileId":"' + profileObj.itemId +
            '","timestamp":"' + Instant.now().toString() + '"}'

        Thread.start {
            HttpURLConnection conn = null
            try {
                conn = (HttpURLConnection) new URL(webhookUrl).openConnection()
                conn.setRequestMethod("POST")
                conn.setDoOutput(true)
                conn.setConnectTimeout(2000)
                conn.setReadTimeout(2000)
                conn.setRequestProperty("Content-Type", "application/json")
                conn.getOutputStream().withCloseable { os ->
                    os.write(payload.getBytes(StandardCharsets.UTF_8))
                }
                conn.getResponseCode()
            } catch (Throwable t) {
                logger?.warn("notifyOnPageViewMilestone webhook failed: " + t.getMessage())
            } finally {
                try { conn?.disconnect() } catch (Throwable ignored) {}
            }
        }
    } catch (Throwable t) {
        try { logger?.warn("notifyOnPageViewMilestone error: " + t.getMessage()) } catch (Throwable ignored) {}
    }
    return EventService.NO_CHANGE
}
