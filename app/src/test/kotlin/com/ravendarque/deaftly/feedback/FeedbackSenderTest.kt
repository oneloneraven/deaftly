package com.ravendarque.deaftly.feedback

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URL
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FeedbackSenderTest {
    private val received = mutableListOf<Pair<String, String>>()
    private var status = 201
    private val server =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/v1/submissions") { exchange ->
                received += exchange.requestHeaders.getFirst("Content-Type") to exchange.requestBody.readBytes().decodeToString()
                val reply = """{"id":"x"}""".toByteArray()
                exchange.sendResponseHeaders(status, reply.size.toLong())
                exchange.responseBody.use { it.write(reply) }
            }
            start()
        }

    @AfterTest
    fun stop() = server.stop(0)

    private fun sender(version: String = "0.54.1") =
        HttpFeedbackSender(URL("http://127.0.0.1:${server.address.port}/v1/submissions"), version, Dispatchers.IO)

    private fun send(
        submission: FeedbackSubmission,
        version: String = "0.54.1",
    ) = runBlocking { sender(version).send(submission) }

    private fun body(): JsonObject = Json.parseToJsonElement(received.single().second).jsonObject

    @Test
    fun anIssueSendsOnlyTheIssueFieldsAsJson() {
        val result =
            send(FeedbackSubmission(FeedbackKind.Issue, "  Show crashed  ", contactEmail = " sam@example.com ", quoteConsent = true))

        assertEquals(SendResult.Sent, result)
        assertEquals("application/json; charset=utf-8", received.single().first)
        assertEquals(
            mapOf(
                "product" to "deaftly",
                "kind" to "issue",
                "source" to "app",
                "message" to "Show crashed",
                "contactEmail" to "sam@example.com",
                "appVersion" to "0.54.1",
            ),
            body().mapValues { it.value.jsonPrimitive.content },
        )
    }

    @Test
    fun feedbackWithConsentSendsTheCreditAndName() {
        send(
            FeedbackSubmission(
                FeedbackKind.Feedback,
                "Love it",
                quoteConsent = true,
                credit = Credit.Initials,
                contactName = "Keisha Grant",
            ),
        )

        val body = body()
        assertEquals("feedback", body.getValue("kind").jsonPrimitive.content)
        assertEquals("true", body.getValue("quoteConsent").jsonPrimitive.content)
        assertEquals("initials", body.getValue("credit").jsonPrimitive.content)
        assertEquals("Keisha Grant", body.getValue("contactName").jsonPrimitive.content)
        assertFalse("contactEmail" in body)
    }

    @Test
    fun feedbackWithoutConsentSendsNoCreditOrName() {
        send(FeedbackSubmission(FeedbackKind.Feedback, "Love it", credit = Credit.Name, contactName = "Keisha Grant"))

        val body = body()
        assertEquals("false", body.getValue("quoteConsent").jsonPrimitive.content)
        assertFalse("credit" in body)
        assertFalse("contactName" in body)
    }

    @Test
    fun aVersionThatIsNotThreeNumbersIsLeftOut() {
        send(FeedbackSubmission(FeedbackKind.Issue, "Hello"), version = "0.54.1-debug")

        assertFalse("appVersion" in body())
    }

    @Test
    fun responsesMapToOutcomes() {
        mapOf(
            400 to SendResult.Rejected,
            413 to SendResult.Rejected,
            429 to SendResult.RateLimited,
            500 to SendResult.Failed,
        ).forEach { (code, expected) ->
            status = code
            assertEquals(expected, send(FeedbackSubmission(FeedbackKind.Issue, "Hello")), "HTTP $code")
        }
    }

    @Test
    fun anUnreachableServerIsReportedAsUnreachable() {
        val closedPort = ServerSocket(0).use { it.localPort }
        val result =
            runBlocking {
                HttpFeedbackSender(URL("http://127.0.0.1:$closedPort/v1/submissions"), "0.54.1", Dispatchers.IO)
                    .send(FeedbackSubmission(FeedbackKind.Issue, "Hello"))
            }

        assertEquals(SendResult.Unreachable, result)
    }
}
