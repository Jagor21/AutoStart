package com.sgvdev.autostart.domain.use_case

import com.sgvdev.autostart.base.BaseUseCase
import com.sgvdev.autostart.data.remote.AdRepository
import com.sgvdev.autostart.models.AdResponse
import javax.inject.Inject
import com.sgvdev.autostart.core.Result
import com.sgvdev.autostart.models.AdRequest

class GetAdUseCase @Inject constructor(
    private val adRepository: AdRepository
) : BaseUseCase<AdRequest, AdResponse>() {
    override suspend fun run(param: AdRequest): Result<AdResponse> {
        return adRepository.getAds(param)
    }
}