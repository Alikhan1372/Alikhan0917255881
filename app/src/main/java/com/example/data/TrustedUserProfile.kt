package com.example.data

import com.example.model.EquipmentAssignment
import com.example.model.UserProfile
import com.example.model.UserRole

data class TrustedUserProfile(
    val uid: String,
    val username: String,
    val displayName: String,
    val role: UserRole,
    val assignment: EquipmentAssignment,
    val active: Boolean
) {
    fun toAppProfile() = UserProfile(
        userId = uid,
        username = username,
        displayName = displayName,
        role = role.firestoreValue,
        assignedEquipmentId = assignment.equipmentId,
        active = active
    )
}

enum class TrustedProfileRejection {
    MALFORMED,
    UNKNOWN_AUTH_IDENTITY,
    IDENTITY_MISMATCH,
    INVALID_ROLE,
    INVALID_ASSIGNMENT,
    ROLE_ASSIGNMENT_MISMATCH,
    INACTIVE
}

sealed interface TrustedProfileValidation {
    data class Valid(val profile: TrustedUserProfile) : TrustedProfileValidation
    data class Rejected(val reason: TrustedProfileRejection) : TrustedProfileValidation
}

object TrustedUserProfileValidator {
    fun validate(
        uid: String,
        email: String?,
        aliasDomain: String,
        data: Map<String, Any?>?
    ): TrustedProfileValidation {
        if (uid.isBlank() || data == null) {
            return TrustedProfileValidation.Rejected(TrustedProfileRejection.MALFORMED)
        }

        val username = UsernameAliasMapper.usernameFromFirebaseEmail(email, aliasDomain)
            ?: return TrustedProfileValidation.Rejected(TrustedProfileRejection.UNKNOWN_AUTH_IDENTITY)
        val profileUsername = data["username"] as? String
            ?: return TrustedProfileValidation.Rejected(TrustedProfileRejection.MALFORMED)
        if (!profileUsername.equals(username, ignoreCase = true)) {
            return TrustedProfileValidation.Rejected(TrustedProfileRejection.IDENTITY_MISMATCH)
        }

        val displayName = (data["displayName"] as? String)?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return TrustedProfileValidation.Rejected(TrustedProfileRejection.MALFORMED)
        val role = UserRole.fromTrustedValue(data["role"] as? String)
            ?: return TrustedProfileValidation.Rejected(TrustedProfileRejection.INVALID_ROLE)
        val assignment = EquipmentAssignment.fromTrustedValue(data["assignedEquipmentId"] as? String)
            ?: return TrustedProfileValidation.Rejected(TrustedProfileRejection.INVALID_ASSIGNMENT)
        val active = data["active"] as? Boolean
            ?: return TrustedProfileValidation.Rejected(TrustedProfileRejection.MALFORMED)
        if (!active) return TrustedProfileValidation.Rejected(TrustedProfileRejection.INACTIVE)

        val assignmentMatchesRole = when (role) {
            UserRole.REPRESENTATIVE -> assignment == EquipmentAssignment.CATERPILLAR_988_G
            UserRole.DRIVER -> assignment in setOf(
                EquipmentAssignment.KOMATSU_600_3,
                EquipmentAssignment.DUMP_TRUCK,
                EquipmentAssignment.HYUNDAI_500
            )
            UserRole.TECHNICAL_MANAGER -> assignment == EquipmentAssignment.ALL
        }
        if (!assignmentMatchesRole) {
            return TrustedProfileValidation.Rejected(TrustedProfileRejection.ROLE_ASSIGNMENT_MISMATCH)
        }

        return TrustedProfileValidation.Valid(
            TrustedUserProfile(
                uid = uid,
                username = username,
                displayName = displayName,
                role = role,
                assignment = assignment,
                active = active
            )
        )
    }
}