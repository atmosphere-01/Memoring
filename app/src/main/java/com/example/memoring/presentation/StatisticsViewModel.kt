package com.example.memoring.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoring.domain.LearningStatisticsUseCase
import com.example.memoring.domain.model.LearningStatisticsUiModel
import kotlinx.coroutines.flow.*

class StatisticsViewModel(userId: Int, useCase: LearningStatisticsUseCase) : ViewModel() {
    val statistics: StateFlow<LearningStatisticsUiModel> = useCase.observe(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearningStatisticsUiModel())
}
