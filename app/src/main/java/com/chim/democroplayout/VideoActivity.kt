package com.chim.democroplayout

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chim.democroplayout.player.AppPlayer
import com.chim.croplayout.PlayerMovie
import kotlinx.android.synthetic.main.activity_video.*
import java.io.File


class VideoActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_VIDEO = 100
        const val REQUEST_PERMISSION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
    }

    private val appPlayer by lazy {
        AppPlayer().apply {
            //playerView = this@VideoActivity.playerView
        }
    }

    fun onLoadVideo(v: View) {
        loadVideo()
    }

    fun rotate(v: View) {
        movieTextureView.rotation += 90
    }

    private lateinit var movie: PlayerMovie
    fun applyCrop(view: View) {
//        cropLayoutVideo.applyCropToPlayerView(
//            playerView,
//            playerView.videoSurfaceView as TextureView
//        )
        cropLayoutVideo.appCropToTextureView(movieTextureView)
    }

    var currentVideoPath: String? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_VIDEO -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.let {
                        currentVideoPath = getPath(it.data!!)
                        if (currentVideoPath != null) {
                            if (File(currentVideoPath!!).exists()) {
                                movie = PlayerMovie(
                                    currentVideoPath!!
                                )
                                appPlayer.init(currentVideoPath!!)
                                appPlayer.setTextureView(movieTextureView)
                                movieTextureView.wrapVideo(movie.width, movie.height)
                            } else
                                Toast.makeText(
                                    this,
                                    "File is not exists",
                                    Toast.LENGTH_SHORT
                                ).show()
                        } else {
                            Toast.makeText(this, "Can't find path by uri", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }
    }

    fun loadVideo() {
        doRequestPermission(
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, REQUEST_VIDEO)
            }, {

            }
        )

    }

    @SuppressLint("Recycle")
    fun getPath(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        return if (cursor != null) {
            val column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(column_index)
        } else
            null
    }


    private var onAllow: (() -> Unit)? = null
    private var onDenied: (() -> Unit)? = null
    fun checkPermission(permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions.forEach {
                if (checkSelfPermission(it) ==
                    PackageManager.PERMISSION_DENIED
                ) {
                    return false
                }
            }
        }
        return true
    }

    private fun doRequestPermission(
        permissions: Array<String>,
        onAllow: () -> Unit = {}, onDenied: () -> Unit = {}
    ) {
        this.onAllow = onAllow
        this.onDenied = onDenied
        if (checkPermission(permissions)) {
            onAllow()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, REQUEST_PERMISSION)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (checkPermission(permissions)) {
            onAllow?.invoke()
        } else {
            onDenied?.invoke()
        }
    }

    override fun onDestroy() {
        appPlayer.release()
        super.onDestroy()
    }

}