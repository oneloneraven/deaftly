package com.oneloneraven.deaftly.storage

import android.util.Log

private const val TAG = "TemplateStore"
private const val KEPT_CHATS_TAG = "KeptChats"

fun describeFailure(failure: Throwable): String = generateSequence(failure) { it.cause }.joinToString(" <- ") { it.javaClass.simpleName }

fun logCorruption(failure: Throwable) {
    Log.w(TAG, "Template store unreadable, restored built-in templates: ${describeFailure(failure)}")
}

fun logKeptChatSaveFailure(failure: Throwable) {
    Log.w(KEPT_CHATS_TAG, "Kept chat not saved: ${describeFailure(failure)}")
}
