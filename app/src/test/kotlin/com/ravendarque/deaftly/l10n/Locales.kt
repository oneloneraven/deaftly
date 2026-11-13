package com.ravendarque.deaftly.l10n

import android.content.res.Resources
import org.robolectric.RuntimeEnvironment

fun resourcesFor(qualifiers: String): Resources {
    RuntimeEnvironment.setQualifiers(qualifiers)
    return RuntimeEnvironment.getApplication().resources
}
