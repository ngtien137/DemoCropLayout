package com.chim.democroplayout

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.AttributeSet
import android.view.TextureView
import com.chim.croplayout.CropLayout
import kotlinx.android.synthetic.main.activity_video.*

class MovieTextureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {

    var mSurfaceTextureReady = false

    init {
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {

    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        mSurfaceTextureReady = false
        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        mSurfaceTextureReady = true
    }

    fun adjustAspectRatio(videoWidth: Int, videoHeight: Int) {
        val aspectRatio = videoHeight.toDouble() / videoWidth
        val newWidth: Int
        val newHeight: Int
        if (height > (width * aspectRatio).toInt()) {
            // limited by narrow width; restrict height
            newWidth = width
            newHeight = (width * aspectRatio).toInt()
        } else {
            // limited by short height; restrict width
            newWidth = (height / aspectRatio).toInt()
            newHeight = height
        }
        val xoff = (width - newWidth) / 2
        val yoff = (height - newHeight) / 2
        val txform = Matrix()
        getTransform(txform)
        txform.setScale(
            newWidth.toFloat() / width,
            newHeight.toFloat() / height
        )
        //txform.postRotate(10);          // just for fun
        txform.postTranslate(xoff.toFloat(), yoff.toFloat())
        setTransform(txform)
    }
}