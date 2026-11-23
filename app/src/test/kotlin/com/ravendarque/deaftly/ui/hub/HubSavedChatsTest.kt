package com.ravendarque.deaftly.ui.hub

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h1600dp")
class HubSavedChatsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val navigated = mutableListOf<Route>()

    private fun show(
        hasSavedChats: Boolean,
        savedChatsLoaded: Boolean = true,
    ) {
        composeRule.setContent {
            AppTheme { HubScreen(onNavigate = { navigated += it }, hasSavedChats = hasSavedChats, savedChatsLoaded = savedChatsLoaded) }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun savedChatsCardShowsOnlyWhenThereAreSavedChats() {
        show(hasSavedChats = false)

        composeRule.onNodeWithText("Saved chats").assertDoesNotExist()
    }

    @Test
    fun theSavedChatsCardOpensTheList() {
        show(hasSavedChats = true)

        composeRule.onNodeWithText("Saved chats").performScrollTo().performClick()

        assertEquals(listOf<Route>(Route.SavedChats), navigated)
    }

    @Test
    fun theCardsWaitUntilSavedChatsHaveBeenRead() {
        show(hasSavedChats = false, savedChatsLoaded = false)

        composeRule.onNodeWithText("Start typing").assertDoesNotExist()
    }
}
