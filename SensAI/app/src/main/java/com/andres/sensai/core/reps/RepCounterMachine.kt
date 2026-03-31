package com.andres.sensai.core.reps

import kotlin.math.abs

class RepCounterMachine(
    private val upThreshold: Float = 0.75f,
    private val downThreshold: Float = 0.25f,
    private val stableMs: Long = 200L,
    private val stableDelta: Float = 0.03f,
    private val cooldownMs: Long = 250L,
    private val emaAlpha: Float = 0.25f
) {
    enum class State { WAIT_UP, GOING_DOWN, WAIT_DOWN, GOING_UP }

    data class Output(
        val state: State,
        val reps: Int,
        val scoreRaw: Float,
        val scoreSmooth: Float,
        val poseOk: Boolean,
        val debug: String
    )

    private var state: State = State.WAIT_UP
    private var reps: Int = 0

    private var smooth: Float = 1f
    private var prevSmooth: Float = 1f

    private var stableStartMs: Long? = null
    private var lastCountMs: Long = 0L

    fun reset() {
        state = State.WAIT_UP
        reps = 0
        smooth = 1f
        prevSmooth = 1f
        stableStartMs = null
        lastCountMs = 0L
    }

    fun update(scoreRaw: Float, nowMs: Long, poseOk: Boolean): Output {
        val raw = scoreRaw.coerceIn(0f, 1f)

        if (!poseOk) {
            // congelamos la máquina
            return Output(state, reps, raw, smooth, false, "poseOk=false → freeze")
        }

        prevSmooth = smooth
        smooth = emaAlpha * raw + (1f - emaAlpha) * smooth

        val delta = abs(smooth - prevSmooth)
        val stableNow = delta <= stableDelta

        if (stableNow) {
            if (stableStartMs == null) stableStartMs = nowMs
        } else {
            stableStartMs = null
        }

        val stableFor = stableStartMs?.let { nowMs - it } ?: 0L
        val stableEnough = stableFor >= stableMs

        val inCooldown = (nowMs - lastCountMs) < cooldownMs

        when (state) {
            State.WAIT_UP -> {
                if (smooth >= upThreshold && stableEnough) {
                    state = State.GOING_DOWN
                    stableStartMs = null
                }
            }
            State.GOING_DOWN -> {
                if (smooth <= downThreshold) {
                    state = State.WAIT_DOWN
                    stableStartMs = null
                }
            }
            State.WAIT_DOWN -> {
                if (smooth <= downThreshold && stableEnough) {
                    state = State.GOING_UP
                    stableStartMs = null
                }
            }
            State.GOING_UP -> {
                if (!inCooldown && smooth >= upThreshold && stableEnough) {
                    reps += 1
                    lastCountMs = nowMs
                    state = State.GOING_DOWN // reps continuas
                    stableStartMs = null
                }
            }
        }

        return Output(
            state = state,
            reps = reps,
            scoreRaw = raw,
            scoreSmooth = smooth,
            poseOk = true,
            debug = "Δ=$delta stableFor=${stableFor}ms cooldown=$inCooldown"
        )
    }
}