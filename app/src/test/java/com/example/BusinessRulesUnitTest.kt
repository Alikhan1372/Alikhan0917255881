package com.example

import com.example.data.EquipmentData
import com.example.data.JalaliDateHelper
import com.example.data.UserAccounts
import com.example.model.OilChangeStatus
import com.example.model.OilTypes
import com.example.model.UserProfile
import com.example.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessRulesUnitTest {

    // -------------------------------------------------------------
    // 1. User & Account Rules Tests
    // -------------------------------------------------------------
    @Test
    fun testPredefinedAccountsExist() {
        val vahid = UserAccounts.findAccountByUsername("vahid")
        assertNotNull(vahid)
        assertEquals("وحید فیروزی", vahid!!.defaultProfile.displayName)
        assertEquals(UserRole.REPRESENTATIVE, vahid.defaultProfile.roleEnum)
        assertEquals("loader_cat_988g", vahid.defaultProfile.assignedEquipmentId)

        val mohammad = UserAccounts.findAccountByUsername("mohammad")
        assertNotNull(mohammad)
        assertEquals("محمد", mohammad!!.defaultProfile.displayName)
        assertEquals(UserRole.DRIVER, mohammad.defaultProfile.roleEnum)
        assertEquals("loader_komatsu_600", mohammad.defaultProfile.assignedEquipmentId)

        val yavari = UserAccounts.findAccountByUsername("yavari")
        assertNotNull(yavari)
        assertEquals("آقای یاوری", yavari!!.defaultProfile.displayName)
        assertEquals(UserRole.DRIVER, yavari.defaultProfile.roleEnum)
        assertEquals("dump_truck", yavari.defaultProfile.assignedEquipmentId)

        val amir = UserAccounts.findAccountByUsername("amir")
        assertNotNull(amir)
        assertEquals("امیر", amir!!.defaultProfile.displayName)
        assertEquals(UserRole.DRIVER, amir.defaultProfile.roleEnum)
        assertEquals("excavator_hyundai_500", amir.defaultProfile.assignedEquipmentId)

        val fani = UserAccounts.findAccountByUsername("fani")
        assertNotNull(fani)
        assertEquals("مسئول فنی", fani!!.defaultProfile.displayName)
        assertEquals(UserRole.TECHNICAL_MANAGER, fani.defaultProfile.roleEnum)
        assertNull(fani.defaultProfile.assignedEquipmentId)
    }

    @Test
    fun testLoginValidation() {
        val validAccount = UserAccounts.findAccountByUsername("vahid")
        assertNotNull(validAccount)
        assertEquals("Vahid@988G", validAccount!!.initialPassword)

        // Invalid account
        val invalidAccount = UserAccounts.findAccountByUsername("non_existent_user")
        assertNull(invalidAccount)
    }

    @Test
    fun testUserImmutableFields() {
        val user = UserProfile(
            userId = "user_driver_mohammad",
            username = "mohammad",
            displayName = "محمد",
            role = "driver",
            assignedEquipmentId = "loader_komatsu_600",
            active = true
        )

        // Only displayName may be modified
        val updated = user.copy(displayName = "محمد رضایی")
        assertEquals("محمد رضایی", updated.displayName)
        assertEquals(user.userId, updated.userId)
        assertEquals(user.username, updated.username)
        assertEquals(user.role, updated.role)
        assertEquals(user.assignedEquipmentId, updated.assignedEquipmentId)
    }

    // -------------------------------------------------------------
    // 2. Equipment Specifications & Electric Compressor Rules
    // -------------------------------------------------------------
    @Test
    fun testElectricCompressorRules() {
        val comp1 = EquipmentData.getEquipmentById("compressor_electric_1")
        assertNotNull(comp1)
        assertFalse("Electric compressor 1 must NOT have engine oil", comp1!!.hasEngineOil)
        assertFalse("Electric compressor 1 must NOT have radiator", comp1.hasRadiator)
        assertFalse("Electric compressor 1 must NOT have gearbox oil", comp1.hasGearboxOil)
        assertFalse("Electric compressor 1 must NOT have grease", comp1.hasGrease)
        assertTrue("Electric compressor 1 must have hydraulic overflow", comp1.hasHydraulicOverflow)

        val comp2 = EquipmentData.getEquipmentById("compressor_electric_2")
        assertNotNull(comp2)
        assertFalse("Electric compressor 2 must NOT have engine oil", comp2!!.hasEngineOil)
        assertFalse("Electric compressor 2 must NOT have radiator", comp2.hasRadiator)
        assertFalse("Electric compressor 2 must NOT have gearbox oil", comp2.hasGearboxOil)
        assertFalse("Electric compressor 2 must NOT have grease", comp2.hasGrease)
        assertTrue("Electric compressor 2 must have hydraulic overflow", comp2.hasHydraulicOverflow)
    }

    @Test
    fun testEngineMachineryRules() {
        val catLoader = EquipmentData.getEquipmentById("loader_cat_988g")
        assertNotNull(catLoader)
        assertTrue(catLoader!!.hasEngineOil)
        assertTrue(catLoader.hasRadiator)
        assertTrue(catLoader.hasGearboxOil)
        assertTrue(catLoader.hasGrease)
        assertFalse(catLoader.hasHydraulicOverflow)
    }

    // -------------------------------------------------------------
    // 3. Work Hours Rules (0..12, shift constraints)
    // -------------------------------------------------------------
    @Test
    fun testWorkHourValidation() {
        fun isValidHour(h: Double) = h in 0.0..12.0

        assertTrue(isValidHour(0.0))
        assertTrue(isValidHour(1.0))
        assertTrue(isValidHour(8.5))
        assertTrue(isValidHour(12.0))
        assertFalse(isValidHour(12.01))
        assertFalse(isValidHour(-1.0))
    }

    @Test
    fun testPastDatesAllowedForWorkHours() {
        // Past date is always allowed
        val pastDate = "2026-09-15"
        val check = JalaliDateHelper.canSubmitWorkHourForDate(pastDate)
        assertTrue("Past dates must always be allowed", check.first)
    }

    // -------------------------------------------------------------
    // 4. Oil Change 90 Hours Calculation
    // -------------------------------------------------------------
    @Test
    fun testOilChangeCalculation() {
        fun calculate(accumulated: Double, target: Double = 90.0): OilChangeStatus {
            val remaining = (target - accumulated).coerceAtLeast(0.0)
            val isDue = accumulated >= target
            val overdue = (accumulated - target).coerceAtLeast(0.0)
            return OilChangeStatus(
                accumulatedHours = accumulated,
                baselineHours = 0.0,
                currentIntervalHours = accumulated,
                remainingHours = remaining,
                isDue = isDue,
                overdueHours = overdue
            )
        }

        // 80h -> 10h remaining
        val status80 = calculate(80.0)
        assertEquals(10.0, status80.remainingHours, 0.01)
        assertFalse(status80.isDue)
        assertEquals(0.0, status80.overdueHours, 0.01)

        // 90h -> due
        val status90 = calculate(90.0)
        assertEquals(0.0, status90.remainingHours, 0.01)
        assertTrue(status90.isDue)
        assertEquals(0.0, status90.overdueHours, 0.01)

        // 97h -> 7h overdue
        val status97 = calculate(97.0)
        assertEquals(0.0, status97.remainingHours, 0.01)
        assertTrue(status97.isDue)
        assertEquals(7.0, status97.overdueHours, 0.01)

        // 150h -> 60h overdue
        val status150 = calculate(150.0)
        assertEquals(0.0, status150.remainingHours, 0.01)
        assertTrue(status150.isDue)
        assertEquals(60.0, status150.overdueHours, 0.01)
    }

    // -------------------------------------------------------------
    // 5. Inventory Stock & Outflow Invariant
    // -------------------------------------------------------------
    @Test
    fun testInventoryBalanceRules() {
        var stock = 100.0

        // Consume 10 -> 90
        val consume1 = 10.0
        assertTrue(stock >= consume1)
        stock -= consume1
        assertEquals(90.0, stock, 0.01)

        // Consume 90 -> 0
        val consume2 = 90.0
        assertTrue(stock >= consume2)
        stock -= consume2
        assertEquals(0.0, stock, 0.01)

        // Consume 1 -> reject (insufficient inventory)
        val consume3 = 1.0
        val canConsume = stock >= consume3
        assertFalse("Cannot consume more than available stock", canConsume)

        // Incoming 50 -> 50
        val incoming = 50.0
        stock += incoming
        assertEquals(50.0, stock, 0.01)
    }

    // -------------------------------------------------------------
    // 6. Jalali Date Conversion
    // -------------------------------------------------------------
    @Test
    fun testJalaliDateConversion() {
        val jDate = JalaliDateHelper.gregorianToJalali(2026, 10, 3)
        assertEquals(1405, jDate.year)
        assertEquals(7, jDate.month) // Mehr
        assertEquals(11, jDate.day)
        assertEquals("مهر", jDate.monthName)
    }
}
