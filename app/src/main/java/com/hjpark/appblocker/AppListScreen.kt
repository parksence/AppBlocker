package com.hjpark.appblocker

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    modifier: Modifier = Modifier,
    viewModel: AppListViewModel = viewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val pm = remember { context.packageManager }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("앱 차단") })
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("앱 이름으로 검색") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
            )
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.errorMessage ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = state.filteredRows,
                            key = { it.packageName },
                            contentType = { "app_row" },
                        ) { row ->
                            AppRow(
                                label = row.label,
                                packageName = row.packageName,
                                isBlocked = row.isBlocked,
                                strictPowerBlockEnabled = state.strictPowerBlockEnabled,
                                packageManager = pm,
                                onBlockedChange = { checked ->
                                    viewModel.onToggleBlocked(
                                        row.packageName,
                                        checked,
                                        row.isBlocked,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private val grayscaleIconFilter: ColorFilter =
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
private fun AppRow(
    label: String,
    packageName: String,
    isBlocked: Boolean,
    strictPowerBlockEnabled: Boolean,
    packageManager: android.content.pm.PackageManager,
    onBlockedChange: (Boolean) -> Unit,
) {
    val iconBitmap = remember(packageName) {
        appIconDrawable(packageManager, packageName)?.toBitmap(128, 128)?.asImageBitmap()
    }
    val rowAlpha = if (isBlocked) 1f else 0.5f
    val switchLocked = strictPowerBlockEnabled && isBlocked

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                colorFilter = if (isBlocked) null else grayscaleIconFilter,
            )
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isBlocked) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isBlocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (switchLocked) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "강력 차단으로 해제 불가",
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(20.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }
        Switch(
            checked = isBlocked,
            onCheckedChange = onBlockedChange,
            enabled = !switchLocked,
        )
    }
}

private fun appIconDrawable(pm: android.content.pm.PackageManager, packageName: String): Drawable? {
    return try {
        pm.getApplicationIcon(packageName)
    } catch (_: Exception) {
        null
    }
}
