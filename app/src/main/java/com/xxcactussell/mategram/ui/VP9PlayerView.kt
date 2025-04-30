package com.xxcactussell.mategram.ui

import android.view.Surface

class Vp9Player(videoPath: String, surface: Surface) {
    private var nativeHandle: Long

    init {
        nativeHandle = nativeCreate(videoPath, surface)
    }

    fun start() {
        nativeStart(nativeHandle)
    }

    fun stop() {
        nativeStop(nativeHandle)
    }

    fun destroy() {
        nativeDestroy(nativeHandle)
        nativeHandle = 0
    }

    private external fun nativeCreate(videoPath: String, surface: Surface): Long
    private external fun nativeStart(nativeHandle: Long)
    private external fun nativeStop(nativeHandle: Long)
    private external fun nativeDestroy(nativeHandle: Long)

    companion object {
        init {
            System.loadLibrary("vp9player") // Убедитесь, что загружаете ту библиотеку, содержащую ваши JNI функции.
        }
    }
}