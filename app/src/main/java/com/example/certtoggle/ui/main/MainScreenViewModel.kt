package com.example.certtoggle.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.certtoggle.data.CertInfo
import com.example.certtoggle.data.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(private val dataRepository: DataRepository) : ViewModel() {
  private val isToggling = MutableStateFlow(false)

  init {
    viewModelScope.launch {
      dataRepository.refresh()
    }
  }

  val isRooted: Boolean = dataRepository.isRooted

  val uiState: StateFlow<MainScreenUiState> =
    combine(dataRepository.certificates, isToggling) { certs, toggling ->
      MainScreenUiState.Success(
        certificates = certs,
        isToggling = toggling
      )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState.Loading)

  fun toggleAll(disable: Boolean) {
    viewModelScope.launch {
      isToggling.value = true
      dataRepository.toggleAll(disable)
      isToggling.value = false
    }
  }

  fun refresh() {
    viewModelScope.launch {
      dataRepository.refresh()
    }
  }
}

sealed interface MainScreenUiState {
  object Loading : MainScreenUiState
  data class Success(
    val certificates: List<CertInfo>,
    val isToggling: Boolean
  ) : MainScreenUiState
}
