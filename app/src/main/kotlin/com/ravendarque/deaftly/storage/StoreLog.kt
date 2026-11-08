package com.ravendarque.deaftly.storage

import android.util.Log

private const val TAG = "TemplateStore"

fun describeFailure(failure: Throwable): String = generateSequence(failure) { it.cause }.joinToString(" <- ") { it.javaClass.simpleName }

fun logCorruption(failure: Throwable) {
    Log.w(TAG, "Template store unreadable, restored built-in templates: ${describeFailure(failure)}")
}
