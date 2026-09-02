package com.ozyern.exhale.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixtures live in the 1.x line on purpose.
 *
 * [Updater.findLatestRelease] discards anything outside the running app's major version, so a
 * release numbered 13.0.0 is invisible to a 1.x build by design — that filter is what stops the
 * old Exhale 3.x history from presenting itself as an update. These tests used to be written
 * against 12.x/13.x tags, which meant every fixture was filtered out before the code under test
 * could rank anything, and both ranking tests failed no matter what the ranking did.
 */
class UpdaterSemVerTest {
    @Test
    fun findLatestRelease_picksHighestStableSemverEvenIfNotFirst() {
        val releases =
            listOf(
                ReleaseInfo(
                    tagName = "v1.4.7",
                    name = "1.4.7",
                    body = null,
                    publishedAt = "2026-01-01T00:00:00Z",
                    htmlUrl = "https://example.com/1.4.7",
                ),
                ReleaseInfo(
                    tagName = "v1.10.0",
                    name = "1.10.0",
                    body = null,
                    publishedAt = "2026-02-01T00:00:00Z",
                    htmlUrl = "https://example.com/1.10.0",
                ),
            )

        val latest = Updater.findLatestRelease(releases)
        assertNotNull(latest)
        // 1.10.0, not 1.4.7: ranked as semver, so 10 beats 4. Sorted as text it would lose.
        assertEquals("v1.10.0", latest?.tagName)
    }

    @Test
    fun findLatestRelease_ignoresPrereleaseWhenStableExists() {
        val releases =
            listOf(
                ReleaseInfo(
                    tagName = "v1.5.0-beta.1",
                    name = "1.5.0-beta.1",
                    body = null,
                    publishedAt = "2026-02-01T00:00:00Z",
                    htmlUrl = "https://example.com/1.5.0-beta.1",
                ),
                ReleaseInfo(
                    tagName = "v1.4.7",
                    name = "1.4.7",
                    body = null,
                    publishedAt = "2026-01-01T00:00:00Z",
                    htmlUrl = "https://example.com/1.4.7",
                ),
            )

        val latest = Updater.findLatestRelease(releases)
        assertNotNull(latest)
        assertEquals("v1.4.7", latest?.tagName)
    }

    @Test
    fun findLatestRelease_ignoresReleasesOutsideTheAppsMajorLine() {
        val releases =
            listOf(
                ReleaseInfo(
                    tagName = "v13.0.0",
                    name = "13.0.0",
                    body = null,
                    publishedAt = "2026-02-01T00:00:00Z",
                    htmlUrl = "https://example.com/13.0.0",
                ),
            )

        // The running app is 1.x. A 13.0.0 tag is foreign lineage, not an update, and offering it
        // would hand the user an APK that cannot install over what they have.
        assertNull(Updater.findLatestRelease(releases))
    }

    @Test
    fun isSameVersion_matchesSemverRegardlessOfPrefixOrText() {
        assertTrue(Updater.isSameVersion("v13.0.0", "13.0.0"))
        assertTrue(Updater.isSameVersion("Exhale 13.0.0", "13.0.0"))
        assertFalse(Updater.isSameVersion("13.0.1", "13.0.0"))
    }
}
