package com.ravendarque.deaftly.guard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManifestGuardTest {
    private val namespace = "com.ravendarque.deaftly"
    private val allowlist =
        allowedPermissions(
            """
            # Comments and blank lines are ignored.

            android.permission.CAMERA
            {namespace}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
            """.trimIndent(),
            namespace,
        )

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
        assertEquals(emptyList(), manifestViolations(manifest(), namespace, allowlist))
    }

    @Test
    fun `allows CAMERA but does not require it`() {
        val xml = manifest(permissions = """<uses-permission android:name="android.permission.CAMERA" />""")
        assertEquals(emptyList(), manifestViolations(xml, namespace, allowlist))
    }

    @Test
    fun `allows the signature permission AndroidX adds in the app namespace`() {
        val xml =
            manifest(
                permissions =
                    """<uses-permission android:name="$namespace.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" />""",
            )
        assertEquals(emptyList(), manifestViolations(xml, namespace, allowlist))
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
        val violations = manifestViolations(xml, namespace, allowlist)
        assertEquals(3, violations.size)
        assertTrue(violations.any { "android.permission.INTERNET" in it })
        assertTrue(violations.any { "android.permission.ACCESS_NETWORK_STATE" in it })
        assertTrue(violations.any { "com.example.Leaky" in it })
    }

    @Test
    fun `ignores components that are not exported`() {
        val xml = manifest(extraComponents = """<service android:name="com.example.Quiet" />""")
        assertEquals(emptyList(), manifestViolations(xml, namespace, allowlist))
    }

    @Test
    fun `reads the allowlist, skipping comments and filling in the namespace`() {
        assertEquals(setOf("android.permission.CAMERA", "$namespace.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"), allowlist)
    }

    @Test
    fun `allows INTERNET once it's on the allowlist`() {
        val xml = manifest(permissions = """<uses-permission android:name="android.permission.INTERNET" />""")
        val withInternet = allowlist + "android.permission.INTERNET"
        assertEquals(emptyList(), manifestViolations(xml, namespace, withInternet))
    }

    @Test
    fun `says where to add a permission`() {
        val xml = manifest(permissions = """<uses-permission android:name="android.permission.RECORD_AUDIO" />""")
        val violation = manifestViolations(xml, namespace, allowlist).single()
        assertTrue("android.permission.RECORD_AUDIO" in violation)
        assertTrue("config/permissions-allowlist.txt" in violation)
    }
}
