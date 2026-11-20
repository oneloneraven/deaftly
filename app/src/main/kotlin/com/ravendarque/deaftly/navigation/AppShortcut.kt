package com.ravendarque.deaftly.navigation

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.ravendarque.deaftly.MainActivity
import com.ravendarque.deaftly.R

private const val EXTRA_SHORTCUT = "com.ravendarque.deaftly.extra.SHORTCUT"

enum class AppShortcut(
    val id: String,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    val route: Route,
) {
    StartTyping("start-typing", R.string.hub_start_typing, R.drawable.ic_shortcut_start_typing, Route.TypeShow),
    FaceToFace("face-to-face", R.string.hub_face_to_face, R.drawable.ic_shortcut_face_to_face, Route.FaceToFace),
    SideBySide("side-by-side", R.string.hub_side_by_side, R.drawable.ic_shortcut_side_by_side, Route.Chat()),
    Listen("listen", R.string.hub_listen, R.drawable.ic_shortcut_listen, Route.Listen),
    ;

    fun intent(context: Context): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_SHORTCUT, id)

    private fun info(context: Context): ShortcutInfoCompat =
        ShortcutInfoCompat
            .Builder(context, id)
            .setShortLabel(context.getString(label))
            .setRank(ordinal)
            .setIcon(IconCompat.createWithResource(context, icon))
            .setIntent(intent(context))
            .build()

    fun requestPin(context: Context) {
        ShortcutManagerCompat.requestPinShortcut(context, info(context), null)
    }

    companion object {
        fun forRoute(route: Route): AppShortcut? = entries.firstOrNull { it.route == route }

        fun from(intent: Intent?): AppShortcut? = intent?.getStringExtra(EXTRA_SHORTCUT)?.let { id -> entries.firstOrNull { it.id == id } }

        fun publish(context: Context) {
            ShortcutManagerCompat.setDynamicShortcuts(context, entries.map { it.info(context) })
        }
    }
}
