/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.ozyern.exhale.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.ozyern.exhale.R

@Composable
fun NavigationTitle(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    subtitle: String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
            // Generous Apple-style breathing room: lots of air ABOVE each section header,
            // a little below before its row content starts.
            .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 10.dp)
    ) {
        thumbnail?.invoke()

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            label?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Title and chevron as one object, hard against each other.
            //
            // The chevron used to sit at the far trailing edge of the row, which is the Material
            // list idiom: label on the left, affordance on the right, the gap between them
            // meaning "this whole row is tappable". Apple Music does the opposite and it is the
            // single most recognisable thing about its section headers — the disclosure mark
            // follows the last letter of the title, so "Made For You ›" reads as one phrase you
            // press rather than as a heading with a button parked across the screen from it.
            //
            // The title takes `weight(1f, fill = false)`: it shrinks and ellipsises when long, but
            // a short one does not stretch, so the chevron stays glued to the text.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    // Apple-Music header: LARGE, heavy, high-contrast ink with generous air above
                    // each section — the whitespace does the separating, not dividers.
                    style = MaterialTheme.typography.headlineSmall.copy(
                        // Tightened. Letter fitting that is right at body size is visibly airy at
                        // 24sp, and every typographic system that cares tracks large sizes in.
                        // This is most of the answer to why an Apple heading looks set and ours
                        // looked typed.
                        letterSpacing = (-0.02).em,
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )

                if (onClick != null) {
                    // A small grey chevron, not a full-size forward arrow. The disclosure mark is
                    // deliberately quieter than the title it follows — a 24dp filled arrow read
                    // as an action button sitting inside a heading.
                    Icon(
                        painter = painterResource(R.drawable.chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(20.dp),
                    )
                }
            }

            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }
    }
}
