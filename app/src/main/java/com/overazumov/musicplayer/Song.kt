package com.overazumov.musicplayer

data class Song(
    val path: String,
    val title: String
)

val internalSongs = listOf(
    Song(
        "android.resource://com.overazumov.musicplayer/${R.raw.mareux_the_perfect_girl}",
        "Mareux - The Perfect Girl"
    ),
    Song(
        "android.resource://com.overazumov.musicplayer/${R.raw.twisted_worth_nothing}",
        "TWISTED - Worth Nothing"
    ),
    Song(
        "android.resource://com.overazumov.musicplayer/${R.raw.pixies_where_is_my_mind}",
        "Pixies - Where is My Mind"
    )
)

