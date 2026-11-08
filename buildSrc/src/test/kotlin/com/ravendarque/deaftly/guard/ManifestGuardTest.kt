package com.ravendarque.deaftly.guard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManifestGuardTest {
    private val namespace = "com.ravendarque.deaftly"

    private fun manifest(
        permissions: String = "",
        extraComponents: String = "",
    ) = """
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            $permissions
            <application>
                <activity android:name="$namespace.MainActivity" android:exported="true" />
                <activity android:name="androidx.compose.ui.tooling.PreviewActivity" android:exported="false" />
                $extraComponents
            </application>
        </manifest>
        """.trimIndent()

    @Test
    fun `accepts only the main activity exported and no permissions`() {
        assertEquals(emptyList(), manifestViolations(manifest(), namespace))
    }

    @Test
    fun `allows CAMERA but does not require it`() {
        val xml = manifest(permissions = """<uses-permission android:name="android.permission.CAMERA" />""")
        assertEquals(emptyList(), manifestViolations(xml, namespace))
    }

    @Test
    fun `allows the signature permission AndroidX adds in the app namespace`() {
        val xml =
            manifest(
                permissions =
                    """<uses-permission android:name="$namespace.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" />""",
            )
        assertEquals(emptyList(), manifestViolations(xml, namespace))
    }

    @Test
    fun `names INTERNET, network state and an extra exported component`() {
        val xml =
            manifest(
                permissions =
                    """
                    <uses-permission android:name="android.permission.INTERNET" />
                    <uses-permission-sdk-23 android:name="android.permission.ACCESS_NETWORK_STATE" />
                    <uses-permission android:name="android.permission.CAMERA" />
                    """.trimIndent(),
                extraComponents = """<receiver android:name="com.example.Leaky" android:exported="true" />""",
            )
        val violations = manifestViolations(xml, namespace)
        assertEquals(3, violations.size)
        assertTrue(violations.any { "android.permission.INTERNET" in it })
        assertTrue(violations.any { "android.permission.ACCESS_NETWORK_STATE" in it })
        assertTrue(violations.any { "com.example.Leaky" in it })
    }

    @Test
    fun `ignores components that are not exported`() {
        val xml = manifest(extraComponents = """<service android:name="com.example.Quiet" />""")
        assertEquals(emptyList(), manifestViolations(xml, namespace))
    }
}
