package com.example.data

import com.example.model.UserProfile
import com.example.model.UserRole

data class PredefinedAccount(
    val username: String,
    val initialPassword: String,
    val defaultProfile: UserProfile
)

object UserAccounts {
    val predefinedAccounts = listOf(
        PredefinedAccount(
            username = "vahid",
            initialPassword = "Vahid@988G",
            defaultProfile = UserProfile(
                userId = "user_vahid",
                username = "vahid",
                displayName = "وحید فیروزی",
                role = "representative",
                assignedEquipmentId = "loader_cat_988g",
                active = true
            )
        ),
        PredefinedAccount(
            username = "mohammad",
            initialPassword = "Mohammad#600",
            defaultProfile = UserProfile(
                userId = "user_driver_mohammad",
                username = "mohammad",
                displayName = "محمد",
                role = "driver",
                assignedEquipmentId = "loader_komatsu_600",
                active = true
            )
        ),
        PredefinedAccount(
            username = "yavari",
            initialPassword = "Yavari#Dump",
            defaultProfile = UserProfile(
                userId = "user_driver_yavari",
                username = "yavari",
                displayName = "آقای یاوری",
                role = "driver",
                assignedEquipmentId = "dump_truck",
                active = true
            )
        ),
        PredefinedAccount(
            username = "amir",
            initialPassword = "Amir#500",
            defaultProfile = UserProfile(
                userId = "user_driver_amir",
                username = "amir",
                displayName = "امیر",
                role = "driver",
                assignedEquipmentId = "excavator_hyundai_500",
                active = true
            )
        ),
        PredefinedAccount(
            username = "fani",
            initialPassword = "Fani#Tech2026",
            defaultProfile = UserProfile(
                userId = "user_technical_manager",
                username = "fani",
                displayName = "مسئول فنی",
                role = "technical_manager",
                assignedEquipmentId = null,
                active = true
            )
        )
    )

    fun findAccountByUsername(username: String): PredefinedAccount? {
        return predefinedAccounts.find { it.username.equals(username.trim(), ignoreCase = true) }
    }
}
