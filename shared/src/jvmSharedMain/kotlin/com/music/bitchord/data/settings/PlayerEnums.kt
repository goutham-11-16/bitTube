package com.music.bitchord.data.settings

/**
 * Stream bitrate ceiling on the YouTube fallback path — MEDIUM, HIGH and
 * LOSSLESS all mean "whatever the best available Opus format is" there; what
 * actually tells them apart is which other sources are allowed to answer
 * *before* YouTube gets asked. That part is [permits], and the rungs read:
 *
 * - [LOSSLESS] — the user's own addons and JioSaavn both asked.
 * - [HIGH] — the addons skipped, JioSaavn asked.
 * - [MEDIUM] and [LOW] — both skipped; YouTube's own Opus ladder is all there
 *   is, capped at [maxKbps].
 *
 * [hourly] is what the ceiling costs in data over an hour of listening, which
 * is the only part of this a user actually cares about on a metered plan.
 */
enum class AudioQuality(
    val maxKbps: Int,
    val label: String,
    val detail: String,
    val hourly: String,
) {
    LOW(64, "360p · Low Data", "YouTube 360p · ~64 kbps audio", "150 MB/hr"),
    MEDIUM(Int.MAX_VALUE, "720p · HD", "YouTube 720p · ~160 kbps audio", "600 MB/hr"),
    HIGH(Int.MAX_VALUE, "1080p · Full HD", "YouTube 1080p · High bitrate audio", "1.5 GB/hr"),
    LOSSLESS(Int.MAX_VALUE, "4K / 2160p · Ultra HD", "4K / 1440p · Master quality", "3+ GB/hr"),
    ;
}

/** The surface that was last open inside the expanded player. */
enum class LastPlayerScreen {
    MAIN,
    LYRICS,
    QUEUE,
}
