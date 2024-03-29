package com.example.ssiam.ui

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo
import com.example.ssiam.APK_NAME
import com.example.ssiam.AppUpdater
import com.example.ssiam.BASE_UPDATE_URL
import com.example.ssiam.repository.WorkManagerRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class UpdaterViewModel @Inject constructor(
    private val appUpdater: AppUpdater,
    private val workManagerRepo: WorkManagerRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdaterUiState())
    val uiState: StateFlow<UpdaterUiState> = _uiState.asStateFlow()

    init {
        appUpdater.deleteRedundantApk()
        observeWorkInfo()
    }

    private fun observeWorkInfo() {
        lateinit var observer: Observer<WorkInfo>
        val workInfoLiveData = workManagerRepo.checkForUpdate()
        observer = Observer { workInfo ->
            if (workInfo.state.isFinished) {
                val outputData = workInfo.outputData
                val isUpdateAvailable = outputData.getBoolean("isUpdateAvailable", false)
                val latestVersion = outputData.getString("latestVersion")
                if (isUpdateAvailable) {
                    _uiState.update { curState -> curState.copy(isUpdateAvailable = true, version = latestVersion!!) }
                    workInfoLiveData.removeObserver(observer)
                }
            }
        }
        workInfoLiveData.observeForever(observer)
    }

    fun ignoreUpdate() {
        _uiState.update { curState -> curState.copy(isUpdateAvailable = false) }
    }

    fun startUpdate() {
        _uiState.update { curState -> curState.copy(isUpdateAvailable = false) }
        val apkUrl = "$BASE_UPDATE_URL/${uiState.value.version}/$APK_NAME"
        appUpdater.startDownload(apkUrl)
    }
}

data class UpdaterUiState(
    val isUpdateAvailable: Boolean = false,
    val version: String = ""
)