package com.ravendarque.deaftly.feedback

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val MESSAGE_MAX = 5_000
const val EMAIL_MAX = 254
const val NAME_MAX = 100
private val Email = Regex("""[^@\s]+@[^@\s]+\.[^@\s]+""")
private const val MESSAGE_KEY = "feedback_message"
private const val EMAIL_KEY = "feedback_email"
private const val CONSENT_KEY = "feedback_consent"
private const val CREDIT_KEY = "feedback_credit"
private const val NAME_KEY = "feedback_name"
private const val SENT_KEY = "feedback_sent"

enum class FeedbackProblem { MessageEmpty, MessageTooLong, EmailInvalid }

data class FeedbackUiState(
    val kind: FeedbackKind,
    val message: String = "",
    val email: String = "",
    val quoteConsent: Boolean = false,
    val credit: Credit = Credit.Anonymous,
    val name: String = "",
    val problems: Set<FeedbackProblem> = emptySet(),
    val sending: Boolean = false,
    val failure: SendResult? = null,
    val sent: Boolean = false,
) {
    val sentWithEmail: Boolean get() = sent && email.isNotBlank()
}

class FeedbackViewModel(
    kind: FeedbackKind,
    private val sender: FeedbackSender,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutable =
        MutableStateFlow(
            FeedbackUiState(
                kind = kind,
                message = savedState[MESSAGE_KEY] ?: "",
                email = savedState[EMAIL_KEY] ?: "",
                quoteConsent = savedState[CONSENT_KEY] ?: false,
                credit = savedState.get<String>(CREDIT_KEY)?.let(Credit::valueOf) ?: Credit.Anonymous,
                name = savedState[NAME_KEY] ?: "",
                sent = savedState[SENT_KEY] ?: false,
            ),
        )

    val state: StateFlow<FeedbackUiState> = mutable.asStateFlow()

    fun setMessage(value: String) = edit(MESSAGE_KEY, value) { copy(message = value, problems = problems - MESSAGE_PROBLEMS) }

    fun setEmail(value: String) =
        edit(EMAIL_KEY, value.take(EMAIL_MAX)) {
            copy(
                email = value.take(EMAIL_MAX),
                problems =
                    problems - FeedbackProblem.EmailInvalid,
            )
        }

    fun setQuoteConsent(value: Boolean) = edit(CONSENT_KEY, value) { copy(quoteConsent = value) }

    fun setCredit(value: Credit) = edit(CREDIT_KEY, value.name) { copy(credit = value) }

    fun setName(value: String) = edit(NAME_KEY, value.take(NAME_MAX)) { copy(name = value.take(NAME_MAX)) }

    fun send() {
        val current = mutable.value
        if (current.sending || current.sent) return
        val problems = problemsIn(current)
        if (problems.isNotEmpty()) {
            mutable.update { it.copy(problems = problems, failure = null) }
            return
        }
        mutable.update { it.copy(sending = true, failure = null) }
        viewModelScope.launch {
            val result = sender.send(current.toSubmission())
            if (result == SendResult.Sent) {
                savedState[SENT_KEY] = true
                mutable.update { it.copy(sending = false, sent = true) }
            } else {
                mutable.update { it.copy(sending = false, failure = result) }
            }
        }
    }

    private fun <T> edit(
        key: String,
        saved: T,
        change: FeedbackUiState.() -> FeedbackUiState,
    ) {
        savedState[key] = saved
        mutable.update { it.change().copy(failure = null) }
    }

    private fun problemsIn(state: FeedbackUiState): Set<FeedbackProblem> =
        buildSet {
            val length = state.message.trim().let { it.codePointCount(0, it.length) }
            if (length == 0) add(FeedbackProblem.MessageEmpty)
            if (length > MESSAGE_MAX) add(FeedbackProblem.MessageTooLong)
            if (state.email.isNotBlank() && !Email.matches(state.email.trim())) add(FeedbackProblem.EmailInvalid)
        }

    private fun FeedbackUiState.toSubmission() =
        FeedbackSubmission(
            kind = kind,
            message = message,
            contactEmail = email,
            quoteConsent = quoteConsent,
            credit = credit,
            contactName = name,
        )

    private companion object {
        val MESSAGE_PROBLEMS = setOf(FeedbackProblem.MessageEmpty, FeedbackProblem.MessageTooLong)
    }
}
