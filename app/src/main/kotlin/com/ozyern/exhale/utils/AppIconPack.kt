/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ozyern.exhale.constants.AppIconPackKey
import com.ozyern.exhale.R
import timber.log.Timber

/**
 * The launcher icons a user can pick between — and, with them, the mark the app wears about the
 * place.
 *
 * ### How the launcher part works at all
 *
 * A component's `android:icon` is fixed at install time — there is no API to change the icon of an
 * installed app. What *can* change is which component the launcher lists, so an app-icon picker is
 * really an alias picker: the manifest declares one `<activity-alias>` per icon, all pointing at
 * MainActivity, and exactly one is enabled. Flipping which one is enabled swaps the icon.
 *
 * ### Why the pack also carries in-app art
 *
 * Because otherwise the choice stops at the home screen. Someone who picks the dark mark and then
 * opens the app is met by the yellow one in the app bar and, next cold start, by the yellow one
 * filling the splash — which reads as the setting not having worked. [logoRes] and [splashLogoRes]
 * are the same mark in the two places the app shows itself off, so the pick carries all the way
 * through.
 *
 * ### The costs, which are real
 *
 * On most launchers the icon visibly vanishes and reappears when the alias flips, and any
 * home-screen shortcut pinned to the outgoing alias can be left pointing at a component that no
 * longer exists. Nothing here can prevent either — they are properties of the technique — which is
 * why the picker warns before it does it.
 *
 * [applyTo] enables the incoming alias *before* disabling the others. Disabling first leaves a
 * window in which the package has no launcher component at all, and some launchers respond to that
 * by dropping the app from the drawer and not putting it back.
 */
enum class AppIconPack(
    /** Appended to the package name to name the manifest alias. */
    val aliasSuffix: String,
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    /** Adaptive-icon layers, for drawing a preview of this pack inside the app. */
    @DrawableRes val backgroundRes: Int,
    @DrawableRes val foregroundRes: Int,
    /** The disc in the app bar. */
    @DrawableRes val logoRes: Int,
    /** The mark the boot splash opens on. */
    @DrawableRes val splashLogoRes: Int,
) {
    /** The default mark: the black "E))" on a gold tile. */
    DEFAULT(
        aliasSuffix = ".MainActivityDefault",
        labelRes = R.string.app_icon_default,
        descriptionRes = R.string.app_icon_default_desc,
        backgroundRes = R.mipmap.ic_launcher_background,
        foregroundRes = R.mipmap.ic_launcher_foreground,
        logoRes = R.drawable.logo,
        splashLogoRes = R.drawable.splash_logo,
    ),

    /** The dark mark: the gold "E))" lit against near-black (assets/icon_dark.png). */
    GOLD(
        aliasSuffix = ".MainActivityGold",
        labelRes = R.string.app_icon_gold,
        descriptionRes = R.string.app_icon_gold_desc,
        backgroundRes = R.mipmap.ic_launcher_gold_background,
        foregroundRes = R.mipmap.ic_launcher_gold_foreground,
        logoRes = R.drawable.logo_gold,
        splashLogoRes = R.drawable.splash_logo_gold,
    );

    fun componentOf(context: Context): ComponentName =
        ComponentName(context.packageName, context.packageName + aliasSuffix)

    companion object {
        val DEFAULT_PACK = DEFAULT

        fun fromName(name: String?): AppIconPack =
            entries.firstOrNull { it.name == name } ?: DEFAULT_PACK

        /**
         * Which pack the system is actually showing.
         *
         * Read from PackageManager rather than from the stored preference, because the two can
         * disagree: a reinstall resets every component to its manifest default while the
         * preference survives in DataStore, and the picker showing a selection the launcher is not
         * honouring is worse than no picker.
         *
         * `COMPONENT_ENABLED_STATE_DEFAULT` means "whatever the manifest said", which is enabled
         * for the default pack and disabled for everything else — so it only counts as a match for
         * the default.
         *
         * This is also the read used by the app bar and the boot splash, which is why it is a
         * plain synchronous call: both need an answer on the frame they are first drawn, and
         * neither can afford to show the wrong mark and then swap it.
         */
        fun current(context: Context): AppIconPack {
            val pm = context.packageManager
            return entries.firstOrNull { pack ->
                when (runCatching { pm.getComponentEnabledSetting(pack.componentOf(context)) }.getOrNull()) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> pack == DEFAULT_PACK
                    else -> false
                }
            } ?: DEFAULT_PACK
        }

        /**
         * Make [pack] the launcher icon.
         *
         * `DONT_KILL_APP` keeps the process alive through the change — without it the system tears
         * the app down mid-tap, which from the user's side is the app crashing when they picked an
         * icon.
         */
        fun applyTo(context: Context, pack: AppIconPack) {
            val pm = context.packageManager

            runCatching {
                // Incoming first. See the class comment: a moment with no enabled launcher
                // component is a moment some launchers use to forget the app exists.
                pm.setComponentEnabledSetting(
                    pack.componentOf(context),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP,
                )

                entries.filter { it != pack }.forEach { other ->
                    pm.setComponentEnabledSetting(
                        other.componentOf(context),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP,
                    )
                }
            }.onFailure {
                Timber.e(it, "Could not switch app icon to ${pack.name}")
            }
        }
    }
}

/**
 * The pack the app is currently wearing, as Compose state.
 *
 * The value itself comes from PackageManager, which is the only thing that actually knows. The
 * preference is read purely as a change signal: DataStore is what notices the moment someone picks
 * a different pack on the settings page, so re-reading the system whenever it changes is what makes
 * the app bar's logo swap under you instead of on next launch.
 *
 * The first composition is already correct — the system read does not wait for DataStore — so
 * nothing shows the wrong mark and then corrects itself.
 */
@Composable
fun rememberAppIconPack(): AppIconPack {
    val context = LocalContext.current
    val (storedPack) = rememberPreference(AppIconPackKey, defaultValue = AppIconPack.DEFAULT.name)
    return remember(context, storedPack) { AppIconPack.current(context) }
}
