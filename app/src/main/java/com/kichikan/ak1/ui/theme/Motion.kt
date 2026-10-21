package com.kichikan.ak1.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Shared motion tokens.
 *
 * One place for every duration/easing/spring so the whole app moves with the same
 * rhythm. Values stay short (140-420 ms): this is a tool used in a class, not a
 * showreel, and long animations make attendance-taking feel slow.
 */
object Motion {
    /** Taps, ripples, colour changes. */
    const val Fast = 140

    /** Standard enter/exit, fade-through. */
    const val Medium = 260

    /** Screen transitions and larger layout moves. */
    const val Slow = 420

    /** Enter: fast start, gentle settle. */
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Emphasised: used for page-level movement. */
    val Emphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Decelerate: counters and progress values. */
    val Decelerate = CubicBezierEasing(0f, 0f, 0f, 1f)

    /** Press feedback: quick and slightly bouncy, never sluggish. */
    val Press = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessHigh)

    /** Selection feedback (nav icons): visible bounce without wobble. */
    val Selection = spring<Float>(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
}
