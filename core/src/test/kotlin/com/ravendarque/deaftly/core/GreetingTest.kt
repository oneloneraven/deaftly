package com.ravendarque.deaftly.core

import kotlin.test.Test
import kotlin.test.assertEquals

class GreetingTest {
    @Test
    fun greetsByName() {
        assertEquals("Hello from Deaftly", greeting("Deaftly"))
    }
}
