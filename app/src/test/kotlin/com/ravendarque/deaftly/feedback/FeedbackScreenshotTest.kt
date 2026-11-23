package com.ravendarque.deaftly.feedback

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h1200dp")
class FeedbackScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val actions = FeedbackActions({}, {}, {})

    private fun capture(
        state: FeedbackUiState,
        dark: Boolean = false,
    ) {
        composeRule.setContent { AppTheme(darkTheme = dark) { FeedbackContent(state, "0.54.1", actions) } }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }

    private val issue = FeedbackUiState(FeedbackKind.Issue, message = "Show went blank after I rotated the phone.")

    private val quoted =
        FeedbackUiState(
            FeedbackKind.Feedback,
            message = "Face to face made ordering at the bakery so much easier.",
            email = "keisha@example.com",
            quoteConsent = true,
            credit = Credit.Initials,
            name = "Keisha Grant",
        )

    @Test
    fun issueLightTheme() = capture(issue)

    @Test
    fun issueDarkTheme() = capture(issue, dark = true)

    @Test
    @Config(fontScale = 2f)
    fun issueLargeFontScale() = capture(issue)

    @Test
    fun feedbackQuotedLightTheme() = capture(quoted)

    @Test
    fun feedbackQuotedDarkTheme() = capture(quoted, dark = true)

    @Test
    @Config(fontScale = 2f)
    fun feedbackQuotedLargeFontScale() = capture(quoted)

    @Test
    fun problemsLightTheme() =
        capture(
            FeedbackUiState(
                FeedbackKind.Issue,
                email = "keisha at example",
                problems = setOf(FeedbackProblem.MessageEmpty, FeedbackProblem.EmailInvalid),
            ),
        )

    @Test
    fun failedLightTheme() = capture(issue.copy(failure = SendResult.Unreachable))

    @Test
    fun sentLightTheme() = capture(quoted.copy(sent = true))
}
