package com.objectmeasure.ar.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    // Nossas dependências (UseCases) entrarão aqui no futuro
) : ViewModel() {
    // O estado da UI (UiState) e a lógica viverão aqui
}