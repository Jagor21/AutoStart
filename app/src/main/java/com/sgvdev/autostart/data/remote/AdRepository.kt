package com.sgvdev.autostart.data.remote

import com.sgvdev.autostart.core.Result
import com.sgvdev.autostart.models.AdRequest
import com.sgvdev.autostart.models.AdResponse

interface AdRepository {

    suspend fun getAds(token: AdRequest): Result<AdResponse>
}