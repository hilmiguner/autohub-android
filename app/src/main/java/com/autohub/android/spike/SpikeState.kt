package com.autohub.android.spike

import java.util.concurrent.atomic.AtomicInteger

/**
 * Minimal in-process state used only by the Phase 0 technical spike.
 *
 * The car host increments this value when it receives a button action. The phone
 * shell can read it to confirm that both entry points are running in the same app
 * process. This will be replaced by proper application state in later phases.
 */
object SpikeState {
    private val carTapCount = AtomicInteger(0)

    fun registerCarTap(): Int = carTapCount.incrementAndGet()

    fun carTapCount(): Int = carTapCount.get()

    fun reset() {
        carTapCount.set(0)
    }
}
