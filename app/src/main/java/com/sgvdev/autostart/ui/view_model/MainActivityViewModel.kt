package com.sgvdev.autostart.ui.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgvdev.autostart.domain.use_case.GetAdUseCase
import com.sgvdev.autostart.models.AdRequest
import com.sgvdev.autostart.models.AdResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val getAdUseCase: GetAdUseCase
) : ViewModel() {

//    private val _ad: MutableStateFlow<AdResponse?> = MutableStateFlow(null)
//    val ad = _ad.asStateFlow()

    private val _ad: MutableLiveData<AdResponse> = MutableLiveData()
    val ad: LiveData<AdResponse>
        get() = _ad

    fun getAd(token: AdRequest) {
        viewModelScope.launch {
            while (true) {
                getAdUseCase.execute(token) {
                    it.fold(
                        onSuccess = {
                            _ad.value = it
                        },
                        onFailure = {}
                    )
                }
                delay(60000L)
            }
        }
    }
}