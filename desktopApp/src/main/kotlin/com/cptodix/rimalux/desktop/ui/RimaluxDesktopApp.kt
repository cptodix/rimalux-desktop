package com.cptodix.rimalux.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cptodix.rimalux.desktop.controller.StudioController
import com.cptodix.rimalux.desktop.controller.StudioUiState
import com.cptodix.rimalux.desktop.ui.components.StatusSnackbar
import com.cptodix.rimalux.desktop.ui.options.OptionsModal
import com.cptodix.rimalux.desktop.ui.options.OptionsPage
import com.cptodix.rimalux.desktop.ui.screens.StudioScreen
import kotlinx.coroutines.delay

@Composable
fun RimaluxDesktopApp(
    state: StudioUiState,
    controller: StudioController,
    optionsOpen: Boolean,
    optionsPage: OptionsPage,
    onOptionsOpenChange: (Boolean) -> Unit,
    onOptionsPageChange: (OptionsPage) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state.statusMessage) {
        if (state.statusMessage != null) {
            delay(3_200L)
            controller.dismissStatus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        StudioScreen(
            state = state,
            controller = controller,
            onOpenOptions = {
                onOptionsOpenChange(true)
                onOptionsPageChange(OptionsPage.NONE)
            },
            modifier = Modifier.fillMaxSize()
        )

        if (optionsOpen) {
            OptionsModal(
                state = state,
                controller = controller,
                page = optionsPage,
                onPageChange = onOptionsPageChange,
                onDismiss = {
                    onOptionsOpenChange(false)
                    onOptionsPageChange(OptionsPage.NONE)
                }
            )
        }

        StatusSnackbar(
            message = state.statusMessage,
            onDismiss = controller::dismissStatus,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 150.dp)
        )
    }
}
