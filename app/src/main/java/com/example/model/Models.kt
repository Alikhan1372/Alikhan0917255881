package com.example.model

import com.google.firebase.Timestamp

enum class UserRole(val firestoreValue: String, val titleFa: String) {
    REPRESENTATIVE("representative", "نماینده"),
    DRIVER("driver", "راننده"),
    TECHNICAL_MANAGER("technical_manager", "مسئول فنی");

    companion object {
        fun fromTrustedValue(role: String?): UserRole? = entries.firstOrNull { it.firestoreValue == role }

        fun fromString(role: String?): UserRole {
            return when (role?.lowercase()) {
                "representative", "نماینده" -> REPRESENTATIVE
                "driver", "راننده" -> DRIVER
                "technical_manager", "مسئول فنی" -> TECHNICAL_MANAGER
                else -> DRIVER
            }
        }
    }
}

enum class EquipmentAssignment(val firestoreValue: String, val equipmentId: String?) {
    CATERPILLAR_988_G("CATERPILLAR_988_G", "loader_cat_988g"),
    KOMATSU_600_3("KOMATSU_600_3", "loader_komatsu_600"),
    DUMP_TRUCK("DUMP_TRUCK", "dump_truck"),
    HYUNDAI_500("HYUNDAI_500", "excavator_hyundai_500"),
    ALL("ALL", null);

    companion object {
        fun fromTrustedValue(value: String?): EquipmentAssignment? =
            entries.firstOrNull { it.firestoreValue == value }
    }
}

enum class EquipmentCategory(val titleFa: String) {
    GENERATOR("ژنراتور"),
    COMPRESSOR_ELECTRIC("کمپرسور برقی"),
    COMPRESSOR_DIESEL("کمپرسور گازوئیلی"),
    MACHINERY("ماشین‌آلات سنگین")
}

object OilTypes {
    const val ENGINE_OIL_20W50 = "engine_oil_20w50"
    const val HYDRAULIC_OIL_1068 = "hydraulic_oil_1068"

    const val ENGINE_OIL_NAME = "روغن موتور 20W-50"
    const val HYDRAULIC_OIL_NAME = "روغن هیدرولیک 10.68"

    fun getDisplayName(type: String): String {
        return when (type) {
            ENGINE_OIL_20W50 -> ENGINE_OIL_NAME
            HYDRAULIC_OIL_1068 -> HYDRAULIC_OIL_NAME
            else -> type
        }
    }
}

data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val role: String = "driver",
    val assignedEquipmentId: String? = null,
    val active: Boolean = true
) {
    val roleEnum: UserRole get() = UserRole.fromString(role)
}

data class Equipment(
    val equipmentId: String = "",
    val name: String = "",
    val category: String = "",
    val hasWorkHours: Boolean = true,
    val hasEngineOil: Boolean = true,
    val hasRadiator: Boolean = true,
    val hasGearboxOil: Boolean = true,
    val hasGrease: Boolean = true,
    val hasHydraulicOverflow: Boolean = false,
    val driverName: String? = null,
    val driverRole: String? = null
)

data class DailyWorkHour(
    val recordId: String = "",
    val equipmentId: String = "",
    val date: String = "",
    val hours: Double = 0.0,
    val userId: String = "",
    val displayNameSnapshot: String = "",
    val createdAt: Timestamp? = null
)

data class DailyService(
    val recordId: String = "",
    val equipmentId: String = "",
    val date: String = "",
    val checkEngineOil: Boolean = false,
    val checkGearboxOil: Boolean = false,
    val checkHydraulicOil: Boolean = false,
    val checkOilLeaks: Boolean = false,
    val oilLeaksNotes: String = "",
    val checkRadiatorWater: Boolean = false,
    val checkHoses: Boolean = false,
    val hosesNotes: String = "",
    val checkAirCleaning: Boolean = false,
    val checkAirFilter: Boolean = false,
    val checkGreasing: Boolean = false,
    val checkWashing: Boolean = false,
    val checkCabinCleaning: Boolean = false,
    val userId: String = "",
    val displayNameSnapshot: String = "",
    val createdAt: Timestamp? = null
)

data class OilChange(
    val recordId: String = "",
    val equipmentId: String = "",
    val date: String = "",
    val time: String = "",
    val accumulatedHours: Double = 0.0,
    val oilAmountLiters: Double = 0.0,
    val dieselFilter: Boolean = false,
    val oilFilter: Boolean = false,
    val waterSeparatorFilter: Boolean = false,
    val notes: String = "",
    val userId: String = "",
    val displayNameSnapshot: String = "",
    val createdAt: Timestamp? = null
)

data class OilTransaction(
    val transactionId: String = "",
    val oilType: String = "",
    val direction: String = "in", // "in" or "out"
    val amountLiters: Double = 0.0,
    val equipmentId: String? = null,
    val sourceOperation: String = "", // "initial", "incoming", "oil_change", "service_engine", "service_gearbox", "service_hydraulic", "hydraulic_overflow", "adjustment"
    val sourceOperationId: String? = null,
    val reason: String = "",
    val date: String = "",
    val time: String = "",
    val notes: String = "",
    val userId: String = "",
    val displayNameSnapshot: String = "",
    val createdAt: Timestamp? = null
)

data class InventoryStock(
    val oilType: String = "",
    val oilTypeName: String = "",
    val currentAmountLiters: Double = 0.0,
    val lastUpdated: Timestamp? = null
)

data class MonthlyReport(
    val reportId: String = "",
    val month: String = "",
    val equipmentId: String = "",
    val equipmentName: String = "",
    val totalWorkHours: Double = 0.0,
    val workHoursCount: Int = 0,
    val dailyServicesCount: Int = 0,
    val oilChangesCount: Int = 0,
    val totalOilConsumed: Double = 0.0,
    val snapshotDataJson: String = "",
    val closedByUserId: String = "",
    val closedByDisplayName: String = "",
    val createdAt: Timestamp? = null
)

data class AuditLog(
    val auditId: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val action: String = "",
    val oldValueJson: String = "",
    val newValueJson: String = "",
    val userId: String = "",
    val displayName: String = "",
    val createdAt: Timestamp? = null
)

data class OilChangeStatus(
    val accumulatedHours: Double,
    val baselineHours: Double,
    val currentIntervalHours: Double,
    val remainingHours: Double,
    val isDue: Boolean,
    val overdueHours: Double
)
