package com.hjpark.appblocker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hjpark.appblocker.data.AppDatabase
import com.hjpark.appblocker.data.AppRepository
import com.hjpark.appblocker.data.InstalledAppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppRowUi(
    val packageName: String,
    val label: String,
    val isBlocked: Boolean,
)

data class AppListUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val rows: List<AppRowUi> = emptyList(),
    val filteredRows: List<AppRowUi> = emptyList(),
    val errorMessage: String? = null,
)

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(
        application.applicationContext,
        AppDatabase.getInstance(application).appDao(),
    )

    private val searchQuery = MutableStateFlow("")
    private val installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val isLoading = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val rows: StateFlow<List<AppRowUi>> = combine(
        installedApps,
        repository.blockedPackagesFlow,
    ) { apps, blocked ->
        apps.map { info ->
            AppRowUi(
                packageName = info.packageName,
                label = info.label,
                isBlocked = blocked.contains(info.packageName),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<AppListUiState> = combine(
        isLoading,
        searchQuery,
        rows,
        errorMessage,
    ) { loading, query, allRows, err ->
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) {
            allRows
        } else {
            allRows.filter { row ->
                row.label.lowercase().contains(q) ||
                    row.packageName.lowercase().contains(q)
            }
        }
        AppListUiState(
            isLoading = loading,
            searchQuery = query,
            rows = allRows,
            filteredRows = filtered,
            errorMessage = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppListUiState())

    init {
        refreshInstalledApps()
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            isLoading.update { true }
            errorMessage.update { null }
            try {
                val list = repository.loadUserInstalledApps()
                installedApps.update { list }
            } catch (e: Exception) {
                errorMessage.update { e.message ?: "앱 목록을 불러오지 못했습니다." }
            } finally {
                isLoading.update { false }
            }
        }
    }

    fun onSearchQueryChange(value: String) {
        searchQuery.update { value }
    }

    fun onToggleBlocked(packageName: String, blocked: Boolean) {
        viewModelScope.launch {
            repository.setBlocked(packageName, blocked)
        }
    }
}
