package com.ravendarque.deaftly

import android.util.Log
import com.ravendarque.deaftly.storage.describeFailure
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private const val TAG = "Deaftly"

private val LogFailure = CoroutineExceptionHandler { _, failure -> Log.e(TAG, "Background work failed: ${describeFailure(failure)}") }

fun appScope(dispatcher: CoroutineDispatcher = Dispatchers.IO): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher + LogFailure)
