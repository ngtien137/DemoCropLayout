package com.chim.democroplayout

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun imageClick(view: View) {
        Toast.makeText(this, "Click image", Toast.LENGTH_SHORT).show()
    }

    fun applyCrop(view: View) {
        cropLayout.applyCropToImageView(R.id.imgMain)
    }
}