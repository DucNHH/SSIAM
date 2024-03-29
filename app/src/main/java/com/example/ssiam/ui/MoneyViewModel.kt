package com.example.ssiam.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ssiam.repository.UserPreferenceRepo
import com.example.ssiam.repository.WorkManagerRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MoneyViewModel @Inject constructor(
    workManagerRepo: WorkManagerRepo,
    userPreferenceRepo: UserPreferenceRepo
) : ViewModel() {

    init {
        workManagerRepo.getUpdatedMoney()
    }

    val uiState: StateFlow<MoneyUiState> = userPreferenceRepo.lastMoney.map { money -> MoneyUiState(money) }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = MoneyUiState())
}

data class MoneyUiState(
    val money: Long = 0L
)