package com.hjpark.appblocker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 포커스/차단류 앱에서 흔한 패턴: 필수 권한을 한 화면에서 모두 만족할 때까지 본 기능(목록)으로 넘기지 않음.
 * 설정에서 허용 후 앱으로 돌아오면 [Lifecycle.Event.ON_RESUME]에서 상태를 다시 읽어 자동 반영.
 */
@Composable
fun SetupPermissionsScreen(
    usageGranted: Boolean,
    overlayGranted: Boolean,
    onOpenUsageSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val requiredCount = 2
    val doneCount = listOf(usageGranted, overlayGranted).count { it }
    val progress = doneCount.toFloat() / requiredCount

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = "시작하기 전에",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "아래 두 가지를 모두 허용해야 앱 목록과 차단 설정을 쓸 수 있어요. 설정을 연 뒤 뒤로 돌아오면 자동으로 확인됩니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
        )
        Text(
            text = "$doneCount / $requiredCount 완료",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        SetupPermissionCard(
            title = "1. 사용량 접근",
            description = "설치된 앱 목록과 전면 앱 감지에 필요합니다. 「설정 열기」는 가능한 기기에서 AppBlocker만의 화면으로 바로 열립니다. 목록만 열리면 AppBlocker를 찾아 허용을 켜 주세요.",
            granted = usageGranted,
            onOpenSettings = onOpenUsageSettings,
        )
        SetupPermissionCard(
            title = "2. 다른 앱 위에 표시",
            description = "최신 안드로이드에서는 백그라운드에서 잠금 화면(액티비티) 시작이 막히는 경우가 많아, 다른 앱 위에 잠금 UI를 띄우려면 이 권한이 필요합니다.",
            granted = overlayGranted,
            onOpenSettings = onOpenOverlaySettings,
        )
    }
}

@Composable
private fun SetupPermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    onOpenSettings: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (granted) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "허용됨",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            } else {
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                        )
                        Text(
                            text = "설정 열기",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
