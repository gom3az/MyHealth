package com.gomaa.healthy.presentation.ui.dailysteps

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DailyStepsScreen(
    onNavigateBack: () -> Unit = {}, viewModel: DailyStepsViewModel = hiltViewModel()
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.handleIntent(DailyStepsIntent.LoadData)
    }

    when (state) {
        is DailyStepsState.Error -> {

        }

        is DailyStepsState.Loaded -> {

        }

        DailyStepsState.Loading -> {

        }
    }
}