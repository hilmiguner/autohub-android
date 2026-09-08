package com.autohub.android.spike

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SpikeStateTest {
    @Before
    fun setUp() {
        SpikeState.reset()
    }

    @After
    fun tearDown() {
        SpikeState.reset()
    }

    @Test
    fun registerCarTap_incrementsCounter() {
        assertEquals(1, SpikeState.registerCarTap())
        assertEquals(2, SpikeState.registerCarTap())
        assertEquals(2, SpikeState.carTapCount())
    }
}
