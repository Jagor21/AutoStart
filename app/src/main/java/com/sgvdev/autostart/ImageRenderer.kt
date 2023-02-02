package com.sgvdev.autostart

import com.google.android.exoplayer2.BaseRenderer
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.C.TRACK_TYPE_IMAGE
import com.google.android.exoplayer2.Format

class ImageRenderer : BaseRenderer(TRACK_TYPE_IMAGE) {
    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun render(positionUs: Long, elapsedRealtimeUs: Long) {
        TODO("Not yet implemented")
    }

    override fun isReady(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEnded(): Boolean {
        TODO("Not yet implemented")
    }

    override fun supportsFormat(format: Format): Int {
        TODO("Not yet implemented")
    }
}