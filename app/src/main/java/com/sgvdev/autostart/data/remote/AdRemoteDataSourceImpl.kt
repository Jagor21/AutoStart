package com.sgvdev.autostart.data.remote

import com.sgvdev.autostart.VlifteApi
import com.sgvdev.autostart.base.BaseNetworkDataSource
import com.sgvdev.autostart.core.Result
import com.sgvdev.autostart.models.AdRequest
import javax.inject.Inject
import com.sgvdev.autostart.models.AdResponse
import com.sgvdev.autostart.utils.bodyOrError

class AdRemoteDataSourceImpl @Inject constructor (private val vlifteApi: VlifteApi) : BaseNetworkDataSource(), AdRemoteDataSource {

    override suspend fun getAds(token: AdRequest): Result<AdResponse> {
        return execute { vlifteApi.getAdsData(token) }
    }
}