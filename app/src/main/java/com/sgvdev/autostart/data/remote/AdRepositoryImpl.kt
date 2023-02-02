package com.sgvdev.autostart.data.remote

import com.sgvdev.autostart.core.Result
import com.sgvdev.autostart.models.AdRequest
import com.sgvdev.autostart.models.AdResponse
import javax.inject.Inject

class AdRepositoryImpl @Inject constructor(
    private val adRemoteDataSource: AdRemoteDataSource
) : AdRepository {

    override suspend fun getAds(token: AdRequest): Result<AdResponse> {
        return adRemoteDataSource.getAds(token)
    }
}