package com.chim.democroplayout.player

import android.media.MediaExtractor
import android.media.MediaFormat

class PlayerMovie {
    var path: String = ""
    var width = 0
    var height = 0

    constructor(path: String) {
        this.path = path
        getVideoSize(path)
    }

    private fun getVideoSize(path: String) {
        val extractor = MediaExtractor()
        extractor.setDataSource(path)

        val trackIndex: Int = selectTrack(extractor)

        if (trackIndex < 0) {
            throw RuntimeException("No video track found in $path")
        }
        extractor.selectTrack(trackIndex)

        val format = extractor.getTrackFormat(trackIndex)
        width = format.getInteger(MediaFormat.KEY_WIDTH)
        height = format.getInteger(MediaFormat.KEY_HEIGHT)
    }

    private fun selectTrack(extractor: MediaExtractor): Int {
        // Select the first video track we find, ignore the rest.
        val numTracks = extractor.trackCount
        for (i in 0 until numTracks) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith("video/")) {
                return i
            }
        }
        return -1
    }
}