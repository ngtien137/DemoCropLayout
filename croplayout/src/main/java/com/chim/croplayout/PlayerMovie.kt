package com.chim.croplayout

import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import java.lang.Exception

class PlayerMovie {
    var path: String = ""
    var width = 0
    var height = 0
    var rotation = 0f

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                rotation = format.getFloat(MediaFormat.KEY_ROTATION)?:0f
            }catch (e:NullPointerException){

            }
        }
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

    override fun toString(): String {
        return "PlayerMovie(path='$path', width=$width, height=$height, rotation=$rotation)"
    }


}