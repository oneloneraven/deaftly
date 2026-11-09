package com.ravendarque.deaftly.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

const val TARGET_PACKAGE = "com.ravendarque.deaftly"

private const val WAIT_MILLIS = 5_000L
private const val TEMPLATE_TITLE = "Explain I'm deaf"
private const val POLL_MILLIS = 100L
private const val GET_STARTED_LABEL = "Get started"
private const val CONFIRMATION_WAIT_MILLIS = 1_500L
private const val IMMERSIVE_CONFIRMATION = "Got it"
private const val PLACEHOLDER_LABEL = "how to reply"
private const val CHOICE_LABEL = "type on my phone"
private const val SHOW_LABEL = "Show"
private const val CLOSE_SHOW_DESCRIPTION = "Close show mode"
private const val START_CHAT_LABEL = "Start chat"
private const val EMPTY_CHAT_LABEL = "No messages yet"

private fun MacrobenchmarkScope.waitForText(text: String) {
    check(device.wait(Until.hasObject(By.text(text)), WAIT_MILLIS)) { "Never saw \"$text\"" }
}

fun MacrobenchmarkScope.waitForHome() {
    val deadline = System.currentTimeMillis() + WAIT_MILLIS
    while (System.currentTimeMillis() < deadline && !device.hasObject(By.text(START_CHAT_LABEL))) {
        device.findObject(By.text(GET_STARTED_LABEL))?.click()
        Thread.sleep(POLL_MILLIS)
    }
    waitForText(START_CHAT_LABEL)
}

fun MacrobenchmarkScope.resetAppData() {
    device.executeShellCommand("pm clear $TARGET_PACKAGE")
}

fun MacrobenchmarkScope.openHome() {
    pressHome()
    startActivityAndWait()
    waitForHome()
}

fun MacrobenchmarkScope.openTemplateAndShow() {
    device.findObject(By.text(TEMPLATE_TITLE)).click()
    waitForText(SHOW_LABEL)
    device.findObject(By.text(PLACEHOLDER_LABEL)).click()
    waitForText(CHOICE_LABEL)
    device.findObject(By.text(CHOICE_LABEL)).click()
    device.wait(Until.hasObject(By.text(SHOW_LABEL).enabled(true)), WAIT_MILLIS)
    device.findObject(By.text(SHOW_LABEL)).click()
    device.wait(Until.findObject(By.text(IMMERSIVE_CONFIRMATION)), CONFIRMATION_WAIT_MILLIS)?.click()
    check(device.wait(Until.hasObject(By.desc(CLOSE_SHOW_DESCRIPTION)), WAIT_MILLIS)) { "Show mode never opened" }
    device.findObject(By.desc(CLOSE_SHOW_DESCRIPTION)).click()
    device.wait(Until.hasObject(By.text(SHOW_LABEL)), WAIT_MILLIS)
    device.pressBack()
    waitForText(START_CHAT_LABEL)
}

fun MacrobenchmarkScope.openChat() {
    device.findObject(By.text(START_CHAT_LABEL)).click()
    waitForText(EMPTY_CHAT_LABEL)
}
