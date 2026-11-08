package com.ravendarque.deaftly.guard

import org.w3c.dom.Element
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
private const val CAMERA = "android.permission.CAMERA"
private const val APP_RECEIVER_PERMISSION = ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
private const val MAIN_ACTIVITY = ".MainActivity"
private val PERMISSION_TAGS = setOf("uses-permission", "uses-permission-sdk-23")
private val COMPONENT_TAGS = setOf("activity", "activity-alias", "service", "receiver", "provider")

fun manifestViolations(
    manifestXml: String,
    namespace: String,
): List<String> {
    val factory =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
    val root = factory.newDocumentBuilder().parse(manifestXml.byteInputStream()).documentElement
    val allowedPermissions = setOf(CAMERA, "$namespace$APP_RECEIVER_PERMISSION")
    val mainActivity = "$namespace$MAIN_ACTIVITY"
    val violations = mutableListOf<String>()
    val elements = root.getElementsByTagName("*")
    for (i in 0 until elements.length) {
        val element = elements.item(i) as Element
        val name = element.getAttributeNS(ANDROID_NS, "name")
        when (element.tagName) {
            in PERMISSION_TAGS -> {
                if (name !in allowedPermissions) violations += "Permission not allowed: $name (<${element.tagName}>)"
            }

            in COMPONENT_TAGS -> {
                if (element.getAttributeNS(ANDROID_NS, "exported") == "true" && name != mainActivity) {
                    violations += "Exported component not allowed: $name (<${element.tagName}>)"
                }
            }
        }
    }
    return violations
}
