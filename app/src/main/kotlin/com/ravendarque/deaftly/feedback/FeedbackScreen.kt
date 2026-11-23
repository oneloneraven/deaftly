package com.ravendarque.deaftly.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.components.DeaftlyTopBar
import com.ravendarque.deaftly.ui.components.PrimaryButton
import com.ravendarque.deaftly.ui.settings.CheckRow
import com.ravendarque.deaftly.ui.settings.ChoiceRow
import com.ravendarque.deaftly.ui.theme.Deaftly

private const val MESSAGE_LINES = 6

sealed interface FeedbackEdit {
    data class Message(
        val value: String,
    ) : FeedbackEdit

    data class Email(
        val value: String,
    ) : FeedbackEdit

    data class QuoteConsent(
        val value: Boolean,
    ) : FeedbackEdit

    data class CreditChoice(
        val value: Credit,
    ) : FeedbackEdit

    data class Name(
        val value: String,
    ) : FeedbackEdit
}

class FeedbackActions(
    val onEdit: (FeedbackEdit) -> Unit,
    val onSend: () -> Unit,
    val onDone: () -> Unit,
)

@Composable
fun FeedbackScreen(
    viewModel: FeedbackViewModel,
    appVersion: String,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FeedbackContent(
        state = state,
        appVersion = appVersion,
        actions =
            FeedbackActions(
                onEdit = { edit ->
                    when (edit) {
                        is FeedbackEdit.Message -> viewModel.setMessage(edit.value)
                        is FeedbackEdit.Email -> viewModel.setEmail(edit.value)
                        is FeedbackEdit.QuoteConsent -> viewModel.setQuoteConsent(edit.value)
                        is FeedbackEdit.CreditChoice -> viewModel.setCredit(edit.value)
                        is FeedbackEdit.Name -> viewModel.setName(edit.value)
                    }
                },
                onSend = viewModel::send,
                onDone = onDone,
            ),
    )
}

@Composable
fun FeedbackContent(
    state: FeedbackUiState,
    appVersion: String,
    actions: FeedbackActions,
) {
    val title = stringResource(if (state.kind == FeedbackKind.Issue) R.string.feedback_issue_title else R.string.feedback_feedback_title)
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding().imePadding()) {
            DeaftlyTopBar(title = title, onNavigate = actions.onDone)
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Deaftly.spacing.space20, vertical = Deaftly.spacing.space8),
                verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space16),
            ) {
                if (state.sent) Sent(state, actions.onDone) else Form(state, appVersion, actions)
            }
        }
    }
}

@Composable
private fun Form(
    state: FeedbackUiState,
    appVersion: String,
    actions: FeedbackActions,
) {
    val intro = if (state.kind == FeedbackKind.Issue) R.string.feedback_issue_intro else R.string.feedback_feedback_intro
    Paragraph(stringResource(intro))
    Paragraph(stringResource(R.string.feedback_privacy), muted = true)
    Field(
        label = stringResource(R.string.feedback_message),
        value = state.message,
        onChange = { actions.onEdit(FeedbackEdit.Message(it)) },
        supporting = stringResource(R.string.feedback_message_hint),
        error = messageError(state.problems),
        singleLine = false,
        keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
    Field(
        label = stringResource(R.string.feedback_email),
        value = state.email,
        onChange = { actions.onEdit(FeedbackEdit.Email(it)) },
        supporting = stringResource(R.string.feedback_email_hint),
        error = stringResource(R.string.feedback_email_invalid).takeIf { FeedbackProblem.EmailInvalid in state.problems },
        keyboard = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )
    if (state.kind == FeedbackKind.Feedback) QuoteChoices(state, actions)
    Paragraph(stringResource(R.string.feedback_version, appVersion), muted = true)
    state.failure?.let { failure ->
        Text(
            text = stringResource(failure.messageRes()),
            style = MaterialTheme.typography.bodyLarge,
            color = Deaftly.colors.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    PrimaryButton(
        text = stringResource(if (state.sending) R.string.feedback_sending else R.string.feedback_send),
        onClick = actions.onSend,
        enabled = !state.sending,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun QuoteChoices(
    state: FeedbackUiState,
    actions: FeedbackActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space10)) {
        Text(
            text = stringResource(R.string.feedback_quote_heading),
            style = MaterialTheme.typography.titleSmall,
            color = Deaftly.colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        Paragraph(stringResource(R.string.feedback_quote_hint), muted = true)
        CheckRow(state.quoteConsent, stringResource(R.string.feedback_quote_consent)) { actions.onEdit(FeedbackEdit.QuoteConsent(it)) }
    }
    if (state.quoteConsent) {
        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space10)) {
            Credit.entries.forEach { credit ->
                ChoiceRow(state.credit == credit, stringResource(credit.labelRes()), hint = null, onClick = {
                    actions.onEdit(FeedbackEdit.CreditChoice(credit))
                })
            }
        }
        if (state.credit != Credit.Anonymous) {
            Field(
                label = stringResource(R.string.feedback_name),
                value = state.name,
                onChange = { actions.onEdit(FeedbackEdit.Name(it)) },
                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            )
        }
    }
}

@Composable
private fun Sent(
    state: FeedbackUiState,
    onDone: () -> Unit,
) {
    Text(
        text = stringResource(R.string.feedback_sent_title),
        style = MaterialTheme.typography.headlineSmall,
        color = Deaftly.colors.ink,
        modifier =
            Modifier.semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
            },
    )
    Paragraph(stringResource(if (state.sentWithEmail) R.string.feedback_sent_reply else R.string.feedback_sent_no_reply))
    PrimaryButton(text = stringResource(R.string.feedback_done), onClick = onDone, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun Paragraph(
    text: String,
    muted: Boolean = false,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (muted) Deaftly.colors.inkSecondary else Deaftly.colors.ink,
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    supporting: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
) {
    val c = Deaftly.colors
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().semantics { if (error != null) error(error) },
        label = { Text(label) },
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        isError = error != null,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else MESSAGE_LINES,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = Deaftly.shapes.control,
        keyboardOptions = keyboard,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = c.ink,
                unfocusedTextColor = c.ink,
                focusedContainerColor = c.surface,
                unfocusedContainerColor = c.surface,
                errorContainerColor = c.surface,
                focusedBorderColor = c.accent,
                unfocusedBorderColor = c.outline,
                focusedLabelColor = c.accentStrong,
                unfocusedLabelColor = c.inkMuted,
                focusedSupportingTextColor = c.inkSecondary,
                unfocusedSupportingTextColor = c.inkSecondary,
                errorBorderColor = c.error,
                errorLabelColor = c.error,
                errorSupportingTextColor = c.error,
                cursorColor = c.accent,
            ),
    )
}

@Composable
private fun messageError(problems: Set<FeedbackProblem>): String? =
    when {
        FeedbackProblem.MessageEmpty in problems -> stringResource(R.string.feedback_message_empty)
        FeedbackProblem.MessageTooLong in problems -> stringResource(R.string.feedback_message_too_long)
        else -> null
    }

private fun SendResult.messageRes(): Int =
    when (this) {
        SendResult.Unreachable -> R.string.feedback_unreachable
        SendResult.RateLimited -> R.string.feedback_rate_limited
        SendResult.Rejected -> R.string.feedback_rejected
        SendResult.Sent, SendResult.Failed -> R.string.feedback_failed
    }

private fun Credit.labelRes(): Int =
    when (this) {
        Credit.Anonymous -> R.string.feedback_credit_anonymous
        Credit.Initials -> R.string.feedback_credit_initials
        Credit.Name -> R.string.feedback_credit_name
    }
