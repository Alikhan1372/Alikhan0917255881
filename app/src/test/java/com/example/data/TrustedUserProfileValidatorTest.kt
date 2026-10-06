package com.example.data

import com.example.model.EquipmentAssignment
import com.example.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedUserProfileValidatorTest {
    private val domain = "accounts.example.invalid"

    @Test
    fun `accepts a valid active trusted profile and maps manager assignment`() {
        val result = TrustedUserProfileValidator.validate(
            uid = "firebase-uid-1",
            email = "technical@$domain",
            aliasDomain = domain,
            data = profile(username = "technical", role = "technical_manager", assignment = "ALL")
        )

        assertTrue(result is TrustedProfileValidation.Valid)
        val trusted = (result as TrustedProfileValidation.Valid).profile
        assertEquals("firebase-uid-1", trusted.uid)
        assertEquals(UserRole.TECHNICAL_MANAGER, trusted.role)
        assertEquals(EquipmentAssignment.ALL, trusted.assignment)
        assertEquals(null, trusted.toAppProfile().assignedEquipmentId)
    }

    @Test
    fun `rejects missing profile`() {
        assertRejected(
            TrustedUserProfileValidator.validate("uid", "vahid@$domain", domain, null),
            TrustedProfileRejection.MALFORMED
        )
    }

    @Test
    fun `rejects inactive profile`() {
        assertRejected(
            TrustedUserProfileValidator.validate(
                "uid", "vahid@$domain", domain,
                profile(username = "vahid", role = "representative", assignment = "CATERPILLAR_988_G", active = false)
            ),
            TrustedProfileRejection.INACTIVE
        )
    }

    @Test
    fun `rejects invalid role and assignment`() {
        assertRejected(
            TrustedUserProfileValidator.validate(
                "uid", "vahid@$domain", domain,
                profile(username = "vahid", role = "admin", assignment = "CATERPILLAR_988_G")
            ),
            TrustedProfileRejection.INVALID_ROLE
        )
        assertRejected(
            TrustedUserProfileValidator.validate(
                "uid", "vahid@$domain", domain,
                profile(username = "vahid", role = "representative", assignment = "unknown")
            ),
            TrustedProfileRejection.INVALID_ASSIGNMENT
        )
    }

    @Test
    fun `rejects identity mismatch and incompatible role assignment`() {
        assertRejected(
            TrustedUserProfileValidator.validate(
                "uid", "mohammad@$domain", domain,
                profile(username = "vahid", role = "representative", assignment = "CATERPILLAR_988_G")
            ),
            TrustedProfileRejection.IDENTITY_MISMATCH
        )
        assertRejected(
            TrustedUserProfileValidator.validate(
                "uid", "vahid@$domain", domain,
                profile(username = "vahid", role = "representative", assignment = "DUMP_TRUCK")
            ),
            TrustedProfileRejection.ROLE_ASSIGNMENT_MISMATCH
        )
    }

    private fun profile(
        username: String,
        role: String,
        assignment: String,
        active: Boolean = true
    ) = mapOf(
        "username" to username,
        "displayName" to "نام نمایشی",
        "role" to role,
        "assignedEquipmentId" to assignment,
        "active" to active
    )

    private fun assertRejected(
        result: TrustedProfileValidation,
        expected: TrustedProfileRejection
    ) {
        assertTrue(result is TrustedProfileValidation.Rejected)
        assertEquals(expected, (result as TrustedProfileValidation.Rejected).reason)
    }
}