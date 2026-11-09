package com.ravendarque.deaftly.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.AppTheme
import com.ravendarque.deaftly.ui.theme.Deaftly
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val SCREENSHOT_DIR = "src/test/screenshots"
private const val PACKAGE = "com.ravendarque.deaftly.ui.components"

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h1600dp")
abstract class ComponentScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val testName = TestName()

    protected abstract val content: @Composable () -> Unit

    private fun capture(dark: Boolean) {
        composeRule.setContent {
            AppTheme(darkTheme = dark) {
                Surface(color = Deaftly.colors.ground) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
                }
            }
        }
        composeRule.onRoot().captureRoboImage(
            "$SCREENSHOT_DIR/$PACKAGE.${this::class.simpleName}.${testName.methodName}.png",
        )
    }

    @Test
    fun lightTheme() = capture(dark = false)

    @Test
    fun darkTheme() = capture(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun largeFontScale() = capture(dark = false)
}

class ButtonsScreenshotTest : ComponentScreenshotTest() {
    override val content: @Composable () -> Unit = {
        PrimaryButton("Show", onClick = {}, modifier = Modifier.fillMaxWidth(), icon = DeaftlyIcons.Send)
        SecondaryButton("Start chat", onClick = {}, modifier = Modifier.fillMaxWidth(), icon = DeaftlyIcons.Chat)
        InkButton("Type & show", onClick = {}, modifier = Modifier.fillMaxWidth(), icon = DeaftlyIcons.Type)
        PrimaryButton("Disabled", onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeaftlyIconButton(DeaftlyIcons.Back, "Back", onClick = {})
            DeaftlyIconButton(DeaftlyIcons.Close, "Close", onClick = {})
            DeaftlyIconButton(DeaftlyIcons.Plus, "New template", onClick = {})
            DeaftlyIconButton(DeaftlyIcons.Edit, "Edit template", onClick = {})
            DeaftlyIconButton(DeaftlyIcons.Search, "Search", onClick = {})
            DeaftlyIconButton(DeaftlyIcons.Settings, "Settings", onClick = {})
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeaftlyIconButton(DeaftlyIcons.Scan, "Scan", onClick = {})
            DeaftlyIconButton(DeaftlyIcons.Chat, "Chat", onClick = {})
            DeaftlyIconButton(DeaftlyIcons.Check, "Done", onClick = {})
            DeaftlyIconButton(DeaftlyIcons.Torch, "Torch", onClick = {})
            DeaftlyIconButton(DeaftlyIcons.Send, "Send", onClick = {})
        }
    }
}

class PlaceholderChipScreenshotTest : ComponentScreenshotTest() {
    override val content: @Composable () -> Unit = {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PlaceholderChip("how to reply", onClick = {}, state = ChipState.Empty)
            PlaceholderChip("Sam", onClick = {}, state = ChipState.Filled)
            PlaceholderChip("how to reply", onClick = {}, state = ChipState.Focused)
            PlaceholderChip("Sam", onClick = {}, state = ChipState.Filled, onRemove = {})
            AddMoreChip("add more", onClick = {})
            PlaceholderChip("menu item", onClick = {}, state = ChipState.Empty, tone = ChipTone.PersonB)
            PlaceholderChip("Pasta arrabbiata", onClick = {}, tone = ChipTone.PersonB, onRemove = {})
            PlaceholderChip("menu item", onClick = {}, state = ChipState.Focused, tone = ChipTone.PersonB)
            AddMoreChip("add more", onClick = {}, tone = ChipTone.PersonB)
        }
    }
}

class BottomSheetScreenshotTest : ComponentScreenshotTest() {
    override val content: @Composable () -> Unit = {
        Surface(color = Deaftly.colors.surface, shape = Deaftly.shapes.sheet) {
            Column {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    SheetHandle()
                }
                SheetBody("How should they reply?") {
                    SecondaryButton("type on my phone", onClick = {}, modifier = Modifier.fillMaxWidth())
                    SecondaryButton("write it down", onClick = {}, modifier = Modifier.fillMaxWidth())
                    PrimaryButton("Show", onClick = {}, modifier = Modifier.fillMaxWidth(), icon = DeaftlyIcons.Send)
                }
            }
        }
    }
}

class CardsScreenshotTest : ComponentScreenshotTest() {
    override val content: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PinnedTile(
                title = "Check in",
                preview = "Hi! I'm deaf. I'd like to check in…",
                icon = DeaftlyIcons.Check,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            PinnedTile(
                title = "Order food",
                preview = "Could I have … please?",
                icon = DeaftlyIcons.Scan,
                onClick = {},
                modifier = Modifier.weight(1f),
                tone = TileTone.PersonB,
            )
        }
        ListCard(
            title = "Explain I'm deaf",
            preview =
                listOf(
                    PreviewPart.Plain("I'm deaf. I can't hear you. Please "),
                    PreviewPart.Placeholder("how to reply"),
                ),
            onClick = {},
        )
        ListCard(
            title = "At the doctor's",
            preview = listOf(PreviewPart.Plain("I'm deaf. Please write down what the doctor says.")),
            onClick = {},
        )
    }
}

class HubCardsScreenshotTest : ComponentScreenshotTest() {
    override val content: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HubCard("Start typing", DeaftlyIcons.Type, onClick = {}, modifier = Modifier.weight(1f))
            HubCard("Use a template", DeaftlyIcons.Template, onClick = {}, modifier = Modifier.weight(1f))
        }
    }
}

class BarsScreenshotTest : ComponentScreenshotTest() {
    override val content: @Composable () -> Unit = {
        DeaftlyTopBar(
            title = "Order food",
            onNavigate = {},
            trailing = { DeaftlyIconButton(DeaftlyIcons.Edit, "Edit template", onClick = {}) },
        )
        DeaftlyTopBar(
            title = "Edit template",
            onNavigate = {},
            navigationIcon = DeaftlyIcons.Close,
            navigationLabel = "Cancel",
            trailing = { InkButton("Save", onClick = {}) },
        )
        BottomDock {
            SecondaryButton("Home", onClick = {}, modifier = Modifier.fillMaxWidth(), icon = DeaftlyIcons.Home)
        }
    }
}
