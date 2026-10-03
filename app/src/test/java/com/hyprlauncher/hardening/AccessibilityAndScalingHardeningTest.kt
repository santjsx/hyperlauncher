package com.hyprlauncher.hardening

import com.hyprlauncher.domain.model.AnimationScale
import com.hyprlauncher.domain.model.ThemeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Phase 12 Hardening: Accessibility, Animation & Scaling Validation (PRD §43, §51, §57).
 */
@RunWith(RobolectricTestRunner::class)
class AccessibilityAndScalingHardeningTest {

    @Test
    fun fontScaleHandlesExtreme200PercentScaling() {
        val standardTheme = ThemeConfig(fontScale = 1.0f)
        assertEquals(1.0f, standardTheme.fontScale, 0.01f)

        // 200% font scaling (PRD §51: Font scaling 200% -> UI remains usable)
        val scaledTheme = ThemeConfig(fontScale = 2.0f)
        assertEquals(2.0f, scaledTheme.fontScale, 0.01f)

        // Serialization and deserialization preservation
        val json = scaledTheme.toJson()
        val parsed = ThemeConfig.fromJson(json)
        assertEquals(2.0f, parsed.fontScale, 0.01f)
    }

    @Test
    fun animationScalesSupportAccessibilityReducedMotion() {
        // Full motion
        val full = AnimationScale.FULL
        assertEquals(1.0f, full.multiplier, 0.01f)

        // Reduced motion (PRD Section 29 Accessibility)
        val reduced = AnimationScale.REDUCED
        assertEquals(0.5f, reduced.multiplier, 0.01f)

        // Minimal motion
        val minimal = AnimationScale.MINIMAL
        assertEquals(0.25f, minimal.multiplier, 0.01f)

        // Disabled motion (Zero-duration transitions for motion sensitivity)
        val disabled = AnimationScale.DISABLED
        assertEquals(0.0f, disabled.multiplier, 0.01f)
    }

    @Test
    fun highContrastAndMonospacePreferencesPreserved() {
        val theme = ThemeConfig(
            useMonospaceAll = true,
            borderWidthDp = 2.0f
        )
        assertTrue(theme.useMonospaceAll)
        assertEquals(2.0f, theme.borderWidthDp, 0.01f)

        val json = theme.toJson()
        val parsed = ThemeConfig.fromJson(json)
        assertTrue(parsed.useMonospaceAll)
        assertEquals(2.0f, parsed.borderWidthDp, 0.01f)
    }
}
