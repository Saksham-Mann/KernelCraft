package com.kernelcraft.feature.detail

import com.kernelcraft.feature.detail.ui.BatteryThermalMath
import com.kernelcraft.feature.detail.ui.CameraLensModule
import com.kernelcraft.feature.detail.ui.CameraOpticsMath
import com.kernelcraft.feature.detail.ui.ChargeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for camera optics ray-trace mathematics and battery thermal simulations.
 */
class ComponentOpticsAndThermalTest {

    // --- Camera Optics Tests ---

    @Test
    fun `entrance pupil diameter matches focal length divided by f-number`() {
        val pupilPrimary = CameraOpticsMath.computeEntrancePupilMm(
            focalLengthMm = CameraLensModule.PRIMARY_OIS.focalLengthMm,
            fNumber = CameraLensModule.PRIMARY_OIS.apertureFNumber
        )
        // 24mm / 1.8 = 13.33mm
        assertEquals(13.33f, pupilPrimary, 0.05f)

        val pupilPeriscope = CameraOpticsMath.computeEntrancePupilMm(
            focalLengthMm = CameraLensModule.TELEPHOTO_PERISCOPE.focalLengthMm,
            fNumber = CameraLensModule.TELEPHOTO_PERISCOPE.apertureFNumber
        )
        // 120mm / 3.0 = 40.0mm
        assertEquals(40.0f, pupilPeriscope, 0.05f)
    }

    @Test
    fun `hyperfocal distance increases quadratically with focal length`() {
        val hyperfocalUW = CameraOpticsMath.computeHyperfocalDistanceMeters(13f, 2.2f)
        val hyperfocalPrimary = CameraOpticsMath.computeHyperfocalDistanceMeters(24f, 1.8f)
        val hyperfocalTele = CameraOpticsMath.computeHyperfocalDistanceMeters(120f, 3.0f)

        assertTrue("Primary hyperfocal must be greater than ultra-wide", hyperfocalPrimary > hyperfocalUW)
        assertTrue("Telephoto hyperfocal must be significantly greater than primary", hyperfocalTele > hyperfocalPrimary)
    }

    @Test
    fun `sensor convergence brings rays closer to optical center at infinity focus`() {
        val opticalCenterY = 100f
        val entryYOffset = 40f

        val macroConvergenceY = CameraOpticsMath.computeSensorConvergenceY(
            entryYOffset = entryYOffset,
            opticalCenterY = opticalCenterY,
            focusNormalized = 0f,
            sensorX = 500f,
            lensX = 400f
        )

        val infinityConvergenceY = CameraOpticsMath.computeSensorConvergenceY(
            entryYOffset = entryYOffset,
            opticalCenterY = opticalCenterY,
            focusNormalized = 1f,
            sensorX = 500f,
            lensX = 400f
        )

        // At infinity (focus = 1), rays converge closer to optical center (100)
        val macroDiff = kotlin.math.abs(macroConvergenceY - opticalCenterY)
        val infinityDiff = kotlin.math.abs(infinityConvergenceY - opticalCenterY)
        assertTrue("Infinity focus must focus tighter to optical center than macro", infinityDiff < macroDiff)
    }

    @Test
    fun `periscope camera module is flagged with periscope prism and folded optics`() {
        assertTrue(CameraLensModule.TELEPHOTO_PERISCOPE.isPeriscope)
        assertTrue(CameraLensModule.TELEPHOTO_PERISCOPE.hasOis)
        assertFalse(CameraLensModule.ULTRA_WIDE.isPeriscope)
    }

    // --- Battery & Thermal Simulator Tests ---

    @Test
    fun `chassis temperature increases with charging wattage and vapor chamber mitigates heat`() {
        val temp15W = BatteryThermalMath.computeChassisTemperature(
            ambientTemp = 25f,
            wattage = 15f,
            efficiency = 0.90f,
            hasVaporChamber = true
        )
        val temp45W = BatteryThermalMath.computeChassisTemperature(
            ambientTemp = 25f,
            wattage = 45f,
            efficiency = 0.90f,
            hasVaporChamber = true
        )
        val temp45WNoVc = BatteryThermalMath.computeChassisTemperature(
            ambientTemp = 25f,
            wattage = 45f,
            efficiency = 0.90f,
            hasVaporChamber = false
        )

        assertTrue("Higher wattage must produce higher chassis temperature", temp45W > temp15W)
        assertTrue("Vapor chamber must significantly reduce temperature under high power", temp45W < temp45WNoVc)
    }

    @Test
    fun `time to full decreases as charging power increases`() {
        val time15W = BatteryThermalMath.computeTimeToFullMinutes(15f, 0.74f)
        val time45W = BatteryThermalMath.computeTimeToFullMinutes(45f, 0.92f)
        val time65W = BatteryThermalMath.computeTimeToFullMinutes(65f, 0.89f)

        assertTrue("15W Qi wireless takes longer than 45W wired", time15W > time45W)
        assertTrue("45W wired takes longer than 65W turbo injection", time45W > time65W)
    }

    @Test
    fun `800 cycle retention degrades faster at extreme wattage and temperatures`() {
        val retentionMild = BatteryThermalMath.compute800CycleRetentionPercent(15f, 32f)
        val retentionAggressive = BatteryThermalMath.compute800CycleRetentionPercent(65f, 46f)

        assertTrue("Mild charging preserves more cycle health than aggressive charging", retentionMild > retentionAggressive)
        assertTrue("Retention percentage stays within realistic physical bounds [75..96%]", retentionAggressive in 75f..96f)
    }

    @Test
    fun `charge modes reflect realistic commercial profiles`() {
        assertEquals(15f, ChargeMode.QI_WIRELESS.defaultWattage, 0.01f)
        assertEquals(45f, ChargeMode.WIRED_FAST.defaultWattage, 0.01f)
        assertEquals(65f, ChargeMode.WIRED_EXTREME.defaultWattage, 0.01f)
        assertTrue("Wired charging must be more efficient than inductive Qi", ChargeMode.WIRED_FAST.efficiency > ChargeMode.QI_WIRELESS.efficiency)
    }
}
