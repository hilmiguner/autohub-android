package com.autohub.android.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator
import com.autohub.android.BuildConfig
import com.autohub.android.spike.SpikeState

class AutoHubCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return if (BuildConfig.DEBUG) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(this)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }
    }

    override fun onCreateSession(sessionInfo: SessionInfo): Session = AutoHubSession()
}

private class AutoHubSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = PhaseZeroScreen(carContext)
}

private class PhaseZeroScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val testAction = Action.Builder()
            .setTitle("Test input")
            .setOnClickListener {
                SpikeState.registerCarTap()
                invalidate()
            }
            .build()

        val statusRow = Row.Builder()
            .setTitle("Android Auto host connected")
            .addText("Car test actions received: ${SpikeState.carTapCount()}")
            .build()

        val pane = Pane.Builder()
            .addRow(statusRow)
            .addAction(testAction)
            .build()

        val header = Header.Builder()
            .setStartHeaderAction(Action.APP_ICON)
            .setTitle("AutoHub · Phase 0")
            .build()

        return PaneTemplate.Builder(pane)
            .setHeader(header)
            .build()
    }
}
