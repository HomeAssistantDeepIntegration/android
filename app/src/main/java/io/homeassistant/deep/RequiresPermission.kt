package io.homeassistant.deep

import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequiresPermission(
    permission: String,
    content: @Composable (isGranted: Boolean, requestPermission: () -> Unit) -> Unit
) {
    val permissionState = rememberPermissionState(
        permission
    )

    content(permissionState.status.isGranted) { permissionState.launchPermissionRequest() }
}