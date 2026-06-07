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
  private val isRefreshing = MutableStateFlow(false)

  init {
    viewModelScope.launch {
      dataRepository.refresh()
    }
  }

  val isRooted: Boolean = dataRepository.isRooted

  val uiState: StateFlow<MainScreenUiState> =
    combine(dataRepository.certificates, isToggling, isRefreshing) { certs, toggling, refreshing ->
      MainScreenUiState.Success(
        certificates = certs,
        isToggling = toggling,
        isRefreshing = refreshing
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
      isRefreshing.value = true
      dataRepository.refresh()
      isRefreshing.value = false
    }
  }
}

sealed interface MainScreenUiState {
  object Loading : MainScreenUiState
  data class Success(
    val certificates: List<CertInfo>,
    val isToggling: Boolean,
    val isRefreshing: Boolean = false
  ) : MainScreenUiState
}
