package id.nkz.nokontzzzmanager.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.nkz.nokontzzzmanager.data.database.BenchmarkEntity
import id.nkz.nokontzzzmanager.data.repository.BenchmarkRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BenchmarkDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val benchmarkRepository: BenchmarkRepository
) : ViewModel() {

    private val benchmarkId: Long = checkNotNull(savedStateHandle["benchmarkId"])

    val benchmark: StateFlow<BenchmarkEntity?> = benchmarkRepository.getBenchmarkById(benchmarkId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
