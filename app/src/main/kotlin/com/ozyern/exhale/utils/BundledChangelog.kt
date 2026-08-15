/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.utils

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ozyern.exhale.BuildConfig
import com.ozyern.exhale.R

/** One line of "what you get", as an icon and a pair of strings. */
data class ReleaseHighlight(
    @param:DrawableRes val icon: Int,
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
)

/**
 * The release notes for the build you are running, compiled in.
 *
 * Every *other* version's notes come off GitHub — [Updater.checkForUpdate] reads the release body
 * from `api.github.com/repos/ozyern/Exhale/releases`, so 1.0.103 and everything after it will show
 * whatever is written on its release page with no app change needed. This object exists for the
 * one version that cannot work that way: the build in the user's hand, whose notes have to be
 * readable offline, before any network call, and on day one when the repository may have no
 * release entry at all.
 *
 * [isCurrentBuild] is the guard. It is deliberately an exact match rather than a floor — once the
 * user is on 1.0.103, these 1.0.102 notes are stale, and stale notes presented as current are
 * worse than none. At that point the section simply disappears and the changelog screen (which is
 * fed by GitHub) is the only source, which is correct.
 */
object BundledChangelog {

    /** The version these notes describe. */
    const val VERSION: String = "1.0.102"

    /** True when the running build is the one [highlights] was written for. */
    val isCurrentBuild: Boolean
        get() = Updater.isSameVersion(BuildConfig.VERSION_NAME, VERSION)

    val highlights: List<ReleaseHighlight> = listOf(
        ReleaseHighlight(
            icon = R.drawable.auto_awesome,
            title = R.string.feature_glass_ui_title,
            description = R.string.feature_glass_ui_desc,
        ),
        ReleaseHighlight(
            icon = R.drawable.palette,
            title = R.string.feature_theming_title,
            description = R.string.feature_theming_desc,
        ),
        ReleaseHighlight(
            icon = R.drawable.lyrics,
            title = R.string.feature_lyrics_title,
            description = R.string.feature_lyrics_desc,
        ),
        ReleaseHighlight(
            icon = R.drawable.offline,
            title = R.string.feature_offline_title,
            description = R.string.feature_offline_desc,
        ),
        ReleaseHighlight(
            icon = R.drawable.equalizer,
            title = R.string.feature_equalizer_title,
            description = R.string.feature_equalizer_desc,
        ),
        ReleaseHighlight(
            icon = R.drawable.stats,
            title = R.string.feature_sound_chem_title,
            description = R.string.feature_sound_chem_desc,
        ),
        ReleaseHighlight(
            icon = R.drawable.integration,
            title = R.string.feature_scrobbling_title,
            description = R.string.feature_scrobbling_desc,
        ),
        ReleaseHighlight(
            icon = R.drawable.discord,
            title = R.string.feature_discord_title,
            description = R.string.feature_discord_desc,
        ),
        ReleaseHighlight(
            icon = R.drawable.group,
            title = R.string.feature_together_title,
            description = R.string.feature_together_desc,
        ),
        ReleaseHighlight(
            icon = R.drawable.android_auto,
            title = R.string.feature_android_auto_title,
            description = R.string.feature_android_auto_desc,
        ),
        ReleaseHighlight(
            icon = R.drawable.bedtime,
            title = R.string.feature_aod_title,
            description = R.string.feature_aod_desc,
        ),
        ReleaseHighlight(
            icon = R.drawable.backup,
            title = R.string.feature_backup_title,
            description = R.string.feature_backup_desc,
        ),
    )
}
