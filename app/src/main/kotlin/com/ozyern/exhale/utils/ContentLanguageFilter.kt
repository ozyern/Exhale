/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.utils

import com.ozyern.exhale.innertube.models.AlbumItem
import com.ozyern.exhale.innertube.models.ArtistItem
import com.ozyern.exhale.innertube.models.PlaylistItem
import com.ozyern.exhale.innertube.models.SongItem
import com.ozyern.exhale.innertube.models.YTItem
import com.ozyern.exhale.innertube.pages.ChartsPage
import com.ozyern.exhale.innertube.pages.ExplorePage
import com.ozyern.exhale.innertube.pages.HomePage

/**
 * Strict content-language filtering for the Home/recommendation feeds.
 *
 * The Innertube `hl`/`gl` locale (already injected in App.kt from [com.ozyern.exhale.constants.ContentLanguageKey])
 * only localizes UI strings — YouTube Music still mixes regional-language playlists/songs into
 * the feed. This filter closes that gap: once the user picks language(s) in onboarding or
 * Settings → Content, anything whose title is written in a script that does not belong to those
 * languages is aggressively dropped (e.g. English-only users never see Devanagari/Tamil/Telugu
 * "Hindi Top 50"-style shelves).
 *
 * Script detection is cheap (one pass over the title's code points, no allocation beyond
 * counters) and runs on the IO dispatcher inside the ViewModel loads.
 */
object ContentLanguageFilter {

    /** Unicode script buckets we can tell apart cheaply via code-point ranges. */
    private enum class Script { LATIN, DEVANAGARI, BENGALI, GURMUKHI, GUJARATI, TAMIL, TELUGU, KANNADA, MALAYALAM, SINHALA, ARABIC, CYRILLIC, GREEK, HEBREW, THAI, CJK, HANGUL, KANA, OTHER }

    /** language code → scripts that language is legitimately written in. */
    private fun scriptsFor(lang: String): Set<Script> = when (lang.lowercase().substringBefore("-")) {
        "hi", "mr", "ne", "sa" -> setOf(Script.DEVANAGARI, Script.LATIN)
        "bn", "as" -> setOf(Script.BENGALI, Script.LATIN)
        "pa" -> setOf(Script.GURMUKHI, Script.LATIN)
        "gu" -> setOf(Script.GUJARATI, Script.LATIN)
        "ta" -> setOf(Script.TAMIL, Script.LATIN)
        "te" -> setOf(Script.TELUGU, Script.LATIN)
        "kn" -> setOf(Script.KANNADA, Script.LATIN)
        "ml" -> setOf(Script.MALAYALAM, Script.LATIN)
        "si" -> setOf(Script.SINHALA, Script.LATIN)
        "ar", "fa", "ur" -> setOf(Script.ARABIC, Script.LATIN)
        "ru", "uk", "be", "bg", "sr", "mk" -> setOf(Script.CYRILLIC, Script.LATIN)
        "el" -> setOf(Script.GREEK, Script.LATIN)
        "iw", "he" -> setOf(Script.HEBREW, Script.LATIN)
        "th" -> setOf(Script.THAI, Script.LATIN)
        "zh" -> setOf(Script.CJK, Script.LATIN)
        "ja" -> setOf(Script.KANA, Script.CJK, Script.LATIN)
        "ko" -> setOf(Script.HANGUL, Script.LATIN)
        // Latin-script languages (en, es, fr, de, pt, it, id, tr, vi, …).
        else -> setOf(Script.LATIN)
    }

    private fun scriptOf(codePoint: Int): Script = when (codePoint) {
        in 0x0041..0x024F, in 0x1E00..0x1EFF -> Script.LATIN
        in 0x0900..0x097F -> Script.DEVANAGARI
        in 0x0980..0x09FF -> Script.BENGALI
        in 0x0A00..0x0A7F -> Script.GURMUKHI
        in 0x0A80..0x0AFF -> Script.GUJARATI
        in 0x0B80..0x0BFF -> Script.TAMIL
        in 0x0C00..0x0C7F -> Script.TELUGU
        in 0x0C80..0x0CFF -> Script.KANNADA
        in 0x0D00..0x0D7F -> Script.MALAYALAM
        in 0x0D80..0x0DFF -> Script.SINHALA
        in 0x0600..0x06FF, in 0x0750..0x077F -> Script.ARABIC
        in 0x0400..0x04FF -> Script.CYRILLIC
        in 0x0370..0x03FF -> Script.GREEK
        in 0x0590..0x05FF -> Script.HEBREW
        in 0x0E00..0x0E7F -> Script.THAI
        in 0x4E00..0x9FFF, in 0x3400..0x4DBF -> Script.CJK
        in 0xAC00..0xD7AF -> Script.HANGUL
        in 0x3040..0x30FF -> Script.KANA
        else -> Script.OTHER // digits, punctuation, emoji, whitespace — ignored
    }

    /**
     * Regional-market keywords per language. The script check alone can't catch shelves like
     * "India's biggest hits" — the title is English (Latin script) but the CONTENT is a
     * geo-localized regional category leaked in by Innertube's IP-based `gl`. When the user's
     * language set does NOT include a language, any title mentioning that language's regional
     * markers is aggressively dropped.
     */
    private val regionalMarkers: Map<String, List<String>> = mapOf(
        "hi" to listOf("hindi", "bollywood", "india", "indian", "desi", "filmi"),
        "pa" to listOf("punjabi", "bhangra"),
        "ta" to listOf("tamil", "kollywood"),
        "te" to listOf("telugu", "tollywood"),
        "bn" to listOf("bengali", "bangla"),
        "mr" to listOf("marathi"),
        "gu" to listOf("gujarati"),
        "kn" to listOf("kannada"),
        "ml" to listOf("malayalam", "mollywood"),
        "bho" to listOf("bhojpuri"),
        "ur" to listOf("urdu", "pakistani", "pakistan"),
        "ar" to listOf("arabic", "khaleeji"),
        "es" to listOf("latino", "reggaeton", "música mexicana"),
        "pt" to listOf("sertanejo", "funk brasileiro", "brasil hits"),
        "ko" to listOf("k-pop", "kpop", "korean"),
        "ja" to listOf("j-pop", "jpop", "japanese"),
        "zh" to listOf("c-pop", "cpop", "mandopop", "cantopop"),
        "tr" to listOf("turkish", "türkçe"),
        "th" to listOf("thai", "t-pop"),
        "vi" to listOf("v-pop", "vpop", "vietnamese"),
        "id" to listOf("dangdut", "indonesian"),
        "ru" to listOf("russian", "russkaya"),
    )

    /**
     * Distinctive words that mark a title as being in a language even when it is spelled in the
     * Latin alphabet.
     *
     * This is the hole the script check cannot cover, and it is the common case rather than an
     * edge one: the Hindi and Punjabi songs that dominate an Indian chart shelf are titled
     * "Kesariya", "Tum Hi Ho", "Laung Laachi" — Latin letters throughout, by artists whose names
     * are also Latin. Every one of them scores 100% Latin and sails past a script test.
     *
     * Chosen for distinctiveness, not coverage. Only words that would be bizarre in an English
     * title are listed, and they are matched as **whole words** — the substring test used for
     * region names would fire "dil" inside "dilemma". A title with no listed word still passes,
     * so this narrows the leak rather than closing it: a one-word title that is simply a name
     * remains unclassifiable from the client, and nothing here pretends otherwise.
     */
    private val romanizedMarkers: Map<String, List<String>> = mapOf(
        "hi" to listOf(
            "ishq", "pyaar", "pyar", "mohabbat", "dil", "dilbar", "deewana", "deewani", "bewafa",
            "zindagi", "sanam", "saathiya", "jaanam", "tere", "tera", "teri", "mera", "meri",
            "mere", "tum", "tumhe", "tujhe", "mujhe", "humko", "tumko", "yaad", "raat", "chaand",
            "sapna", "aashiqui", "kabhi", "dhadkan", "jaan", "aankhen", "aankhon", "baarish",
            "chaleya", "kesariya", "judaai", "intezaar", "khuda", "maahi", "mahiya", "piya",
            "sajna", "sajni", "bulleya", "naina", "saiyaan", "banna", "rangisari",
        ),
        "pa" to listOf(
            "yaara", "kudi", "munda", "sohna", "sohni", "laung", "gallan", "nachde", "jatt",
            "jatti", "pind", "gabru", "chann", "ranjha", "heer", "satrangi", "laachi", "sardaar",
        ),
        "ur" to listOf("aashiq", "dilruba", "bekhayali", "qarar", "tanha", "wafa"),
    )

    /** Splits on anything that is not a letter or digit, so markers match as whole words. */
    private val wordBoundary = Regex("[^\\p{L}\\p{N}]+")

    /**
     * True when [text] contains a whole word that belongs to a language the user did not choose.
     */
    private fun mentionsForeignLanguageWord(text: String, languages: Set<String>): Boolean {
        val words = text.lowercase().split(wordBoundary).filterTo(HashSet()) { it.isNotEmpty() }
        if (words.isEmpty()) return false
        for ((lang, markers) in romanizedMarkers) {
            if (lang in languages) continue
            if (markers.any { it in words }) return true
        }
        return false
    }

    /**
     * True when [text] names a regional market whose language is NOT in the user's allowed
     * set — e.g. "India's biggest hits" for an English-only user.
     */
    private fun mentionsForeignRegion(text: String, languages: Set<String>): Boolean {
        val lower = text.lowercase()
        for ((lang, markers) in regionalMarkers) {
            if (lang in languages) continue
            if (markers.any { it in lower }) return true
        }
        return false
    }

    /**
     * True when [text]'s letters are (majority-)written in a script belonging to one of the
     * [languages]. Titles with no detectable letters (pure symbols/numbers) always pass.
     */
    fun textMatches(text: String, languages: Set<String>): Boolean {
        if (languages.isEmpty()) return true
        // Aggressive geo-leak guard: even a perfectly-English title is dropped when it
        // advertises a regional market the user did not opt into.
        if (mentionsForeignRegion(text, languages)) return false
        // Romanized titles are Latin-script by definition, so this has to run before the script
        // test rather than as a tiebreak inside it.
        if (mentionsForeignLanguageWord(text, languages)) return false
        val allowed = languages.flatMapTo(HashSet()) { scriptsFor(it) }

        var matched = 0
        var lettered = 0
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val script = scriptOf(cp)
            if (script != Script.OTHER) {
                lettered++
                if (script in allowed) matched++
            }
            i += Character.charCount(cp)
        }
        if (lettered == 0) return true
        // Strict: >70% of the title's letters must belong to an allowed script.
        return matched * 10 >= lettered * 7
    }

    private fun itemMatches(item: YTItem, languages: Set<String>): Boolean {
        // Title check first — cheapest and catches most leaks.
        if (!textMatches(item.title, languages)) return false
        // Then every text surface the item carries: a "Trending" (English-titled) shelf full
        // of Hindi songs still leaks unless the artist/album/author names are checked too.
        return when (item) {
            is SongItem ->
                item.artists.all { textMatches(it.name, languages) } &&
                        (item.album?.name?.let { textMatches(it, languages) } != false)

            is AlbumItem ->
                item.artists.orEmpty().all { textMatches(it.name, languages) }

            is PlaylistItem ->
                (item.author?.name?.let { textMatches(it, languages) } != false) &&
                        (item.description?.let { textMatches(it, languages) } != false)

            is ArtistItem -> true // name == title, already checked
            else -> true
        }
    }

    /** Filters a list of feed items down to the preferred languages. */
    fun <T : YTItem> filterItems(items: List<T>, languages: Set<String>): List<T> =
        if (languages.isEmpty()) items else items.filter { itemMatches(it, languages) }

    /**
     * Filters a whole home page: section titles AND their items must match; sections that end
     * up empty are dropped entirely so no orphan headers remain.
     */
    fun filterHomeSections(
        sections: List<HomePage.Section>,
        languages: Set<String>,
    ): List<HomePage.Section> {
        if (languages.isEmpty()) return sections
        return sections.mapNotNull { section ->
            if (!textMatches(section.title, languages)) return@mapNotNull null
            val kept = filterItems(section.items, languages)
            if (kept.isEmpty()) null else section.copy(items = kept)
        }
    }

    /**
     * Filters the Charts page ("Trending", "Top songs", community/top playlists…): the shelf
     * titles come back in English even when the CONTENT is entirely geo-localized (IP-region
     * charts), so every individual item is language-checked and empty shelves are dropped.
     */
    fun filterChartSections(
        sections: List<ChartsPage.ChartSection>,
        languages: Set<String>,
    ): List<ChartsPage.ChartSection> {
        if (languages.isEmpty()) return sections
        return sections.mapNotNull { section ->
            if (!textMatches(section.title, languages)) return@mapNotNull null
            val kept = filterItems(section.items, languages)
            if (kept.isEmpty()) null else section.copy(items = kept)
        }
    }

    /** Filters the Explore page (new releases + mood/genre buckets) the same way. */
    fun filterExplorePage(page: ExplorePage, languages: Set<String>): ExplorePage {
        if (languages.isEmpty()) return page
        return page.copy(
            newReleaseAlbums = filterItems(page.newReleaseAlbums, languages),
            moodAndGenres = page.moodAndGenres.filter { textMatches(it.title, languages) },
        )
    }

    /**
     * Resolves the user's preferred language set from the DataStore values: the onboarding CSV
     * ([com.ozyern.exhale.constants.PreferredAudioLanguagesKey]) plus the content language
     * ([com.ozyern.exhale.constants.ContentLanguageKey]). Empty result = filtering disabled.
     */
    fun resolveLanguages(preferredCsv: String?, contentLanguage: String?): Set<String> {
        val langs = HashSet<String>()
        preferredCsv?.split(",")?.forEach { raw ->
            val code = raw.trim().lowercase()
            if (code.isNotEmpty()) langs += code
        }
        contentLanguage?.trim()?.lowercase()
            ?.takeIf { it.isNotEmpty() && it != "system" && it != "system_default" }
            ?.let { langs += it }
        return langs
    }
}
