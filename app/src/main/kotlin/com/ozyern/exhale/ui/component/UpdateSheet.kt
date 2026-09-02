/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.ozyern.exhale.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.mikepenz.markdown.m3.Markdown
import com.ozyern.exhale.R

/**
 * The "a new version is out" sheet that greets you on launch, shaped like iOS Software Update.
 *
 * ### What it replaces
 *
 * A centred headline, the version rendered as an `OutlinedButton` with `onClick = {}` — a control
 * that looks pressable, invites a press and does nothing — a wall of release markdown, and a
 * single button that threw you out to a browser to download an APK by hand. There was no decline
 * action at all, on a sheet that reappears every launch.
 *
 * ### The shape iOS uses, and why each part earns its place
 *
 *  * **Identity as a row, not a stack.** Icon on the leading edge, name and version beside it.
 *    Centring a mark over a headline is a *splash* composition; it says "look at this app". A row
 *    is a record — "this is the thing being updated, and this is which version" — read in one
 *    saccade, and it leaves the vertical space for the thing you actually came to read.
 *  * **Both version numbers, in one quiet line.** Where you are going and where you are. That is
 *    the entire question a person has in front of an update prompt.
 *  * **The notes, labelled and scrolling in their own box.** Release markdown under a bare
 *    headline is a wall; a rule and a "What's New" label make it a section you can choose to
 *    read, and keeping the scroll inside the box means the buttons never walk off the bottom.
 *  * **A filled capsule and a plain text link, stacked.** The primary is unmissable, the
 *    alternative is one word in the same accent — present, unpunished, obviously not the default.
 *    A pair of side-by-side outlined buttons, which is where this was heading, gives two options
 *    equal weight and makes a person stop and choose between them.
 *  * **A footnote in the grey.** iOS always says what the button is about to do. Here that
 *    matters more than usual, because "Update" no longer means "leave for a browser" — it means
 *    the app fetches the build itself, in the background, and you can keep listening.
 */
@Composable
fun ColumnScope.NewVersionSheet(
    latestVersion: String,
    currentVersion: String,
    notes: String?,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
) {
    Spacer(Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(15.dp))
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(15.dp),
                ),
        )

        Spacer(Modifier.size(14.dp))

        Column {
            Text(
                text = stringResource(R.string.app_name) + " " + latestVersion,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    // Display type set at a body default reads loose; the fitting a face wants
                    // falls as the size rises.
                    letterSpacing = (-0.015).em,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.update_installed_version) + " " + currentVersion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(20.dp))

    Text(
        text = stringResource(R.string.update_whats_new),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(8.dp))

    HorizontalDivider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        thickness = 1.dp,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // fill = false so a two-line changelog makes a short sheet rather than stretching one
            // paragraph down the screen.
            .weight(1f, fill = false)
            .padding(vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        if (!notes.isNullOrBlank()) {
            Markdown(
                content = notes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
            )
        } else {
            Text(
                text = stringResource(R.string.release_notes_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Button(
            onClick = onUpdate,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            Text(
                text = stringResource(R.string.update_now),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }

        TextButton(
            onClick = onLater,
            shape = CircleShape,
            // The accent, not the muted grey. A greyed-out alternative reads as *disabled* and
            // people stop seeing it; iOS keeps the second choice in the same blue as the first
            // and lets weight and position carry the hierarchy instead of colour.
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
        ) {
            Text(text = stringResource(R.string.update_not_now))
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text = stringResource(R.string.update_background_footnote),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}
