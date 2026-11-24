package com.oneloneraven.deaftly.feedback

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

const val ENGAGE_SUBMISSIONS = "https://engage-api.oneloneraven.com/v1/submissions"
private const val PRODUCT = "deaftly"
private const val SOURCE = "app"
private const val TIMEOUT_MILLIS = 15_000
private const val CREATED = 201
private const val BAD_REQUEST = 400
private const val TOO_LARGE = 413
private const val RATE_LIMITED = 429
private val AppVersion = Regex("""\d+\.\d+\.\d+""")
private val json = Json { explicitNulls = false }

enum class FeedbackKind(
    val wire: String,
) {
    Issue("issue"),
    Feedback("feedback"),
}

enum class Credit(
    val wire: String,
) {
    Anonymous("anonymous"),
    Initials("initials"),
    Name("name"),
}

data class FeedbackSubmission(
    val kind: FeedbackKind,
    val message: String,
    val contactEmail: String? = null,
    val quoteConsent: Boolean = false,
    val credit: Credit = Credit.Anonymous,
    val contactName: String? = null,
)

enum class SendResult { Sent, Unreachable, RateLimited, Rejected, Failed }

fun interface FeedbackSender {
    suspend fun send(submission: FeedbackSubmission): SendResult
}

@Serializable
internal data class SubmissionBody(
    val product: String,
    val kind: String,
    val source: String,
    val message: String,
    val contactEmail: String? = null,
    val contactName: String? = null,
    val quoteConsent: Boolean? = null,
    val credit: String? = null,
    val appVersion: String? = null,
)

internal fun FeedbackSubmission.body(appVersion: String): String {
    val feedback = kind == FeedbackKind.Feedback
    return json.encodeToString(
        SubmissionBody.serializer(),
        SubmissionBody(
            product = PRODUCT,
            kind = kind.wire,
            source = SOURCE,
            message = message.trim(),
            contactEmail = contactEmail?.trim()?.ifEmpty { null },
            contactName = contactName?.trim()?.ifEmpty { null }.takeIf { feedback && quoteConsent },
            quoteConsent = quoteConsent.takeIf { feedback },
            credit = credit.wire.takeIf { feedback && quoteConsent },
            appVersion = appVersion.takeIf { AppVersion.matches(it) },
        ),
    )
}

class HttpFeedbackSender(
    private val endpoint: URL,
    private val appVersion: String,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : FeedbackSender {
    override suspend fun send(submission: FeedbackSubmission): SendResult =
        withContext(io) {
            try {
                post(submission.body(appVersion))
            } catch (_: IOException) {
                SendResult.Unreachable
            }
        }

    private fun post(body: String): SendResult {
        val connection = endpoint.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { it.write(body.toByteArray()) }
            when (connection.responseCode) {
                CREATED -> SendResult.Sent
                BAD_REQUEST, TOO_LARGE -> SendResult.Rejected
                RATE_LIMITED -> SendResult.RateLimited
                else -> SendResult.Failed
            }
        } finally {
            connection.disconnect()
        }
    }
}
