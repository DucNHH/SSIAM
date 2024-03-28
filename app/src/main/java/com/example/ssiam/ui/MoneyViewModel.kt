package com.example.ssiam.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ssiam.MyChannel
import com.example.ssiam.MySSIAMApp
import com.example.ssiam.worker.MoneyUpdateWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MoneyViewModel(private val app: MySSIAMApp) : ViewModel() {
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MySSIAMApp
                MoneyViewModel(app)
            }
        }
    }

    init {
        val myChannel = MyChannel(app)
        app.workManagerRepo.getUpdatedMoney()
        viewModelScope.launch {
            MoneyUpdateWorker.outputData.collect { money ->
                if (_uiState.value.money != money) {
                    myChannel.notify("Money updated: $money")
                    updateMoneyState(money)
                }
            }
        }
    }

    private val _uiState = MutableStateFlow(MoneyUiState())
    val uiState: StateFlow<MoneyUiState> = app.userPreferenceRepo.lastMoney.map { money -> MoneyUiState(money) }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = MoneyUiState())

    private suspend fun updateMoneyState(money: Long) {
        app.userPreferenceRepo.saveLastMoney(money)
        _uiState.update { curState -> curState.copy(money = money) }
    }
}