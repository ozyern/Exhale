/*
 * Exhale Project Original (2026)
 * ozyern (github.com/ozyern)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.ozyern.exhale.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity

@Immutable
@Entity(
    tableName = "playCount",
    primaryKeys = ["song", "year", "month"]
)
class PlayCountEntity(
    val song: String, // song id
    val year: Int = -1,
    val month: Int = -1,
    val count: Int = -1,
)
