package com.chim.democroplayout

import android.app.Application

class App : Application() {

    companion object{
        private var app:App?=null
        fun self() = app!!

    }

    override fun onCreate() {
        app = this
        super.onCreate()
    }

}