package com.example.data

import com.example.model.Equipment
import com.example.model.EquipmentCategory

object EquipmentData {
    val predefinedEquipmentList = listOf(
        // Generators
        Equipment(
            equipmentId = "generator_volvo",
            name = "موتور برق ولو",
            category = EquipmentCategory.GENERATOR.name,
            hasWorkHours = true,
            hasEngineOil = true,
            hasRadiator = true,
            hasGearboxOil = true,
            hasGrease = true,
            hasHydraulicOverflow = false
        ),
        Equipment(
            equipmentId = "generator_penta",
            name = "موتور برق پنتا",
            category = EquipmentCategory.GENERATOR.name,
            hasWorkHours = true,
            hasEngineOil = true,
            hasRadiator = true,
            hasGearboxOil = true,
            hasGrease = true,
            hasHydraulicOverflow = false
        ),
        Equipment(
            equipmentId = "generator_cummins",
            name = "موتور برق کومنز",
            category = EquipmentCategory.GENERATOR.name,
            hasWorkHours = true,
            hasEngineOil = true,
            hasRadiator = true,
            hasGearboxOil = true,
            hasGrease = true,
            hasHydraulicOverflow = false
        ),
        // Compressors
        Equipment(
            equipmentId = "compressor_electric_1",
            name = "موتور باد برقی سینه کار ۱",
            category = EquipmentCategory.COMPRESSOR_ELECTRIC.name,
            hasWorkHours = true,
            hasEngineOil = false,
            hasRadiator = false,
            hasGearboxOil = false,
            hasGrease = false,
            hasHydraulicOverflow = true
        ),
        Equipment(
            equipmentId = "compressor_electric_2",
            name = "موتور باد برقی سینه کار ۲",
            category = EquipmentCategory.COMPRESSOR_ELECTRIC.name,
            hasWorkHours = true,
            hasEngineOil = false,
            hasRadiator = false,
            hasGearboxOil = false,
            hasGrease = false,
            hasHydraulicOverflow = true
        ),
        Equipment(
            equipmentId = "compressor_diesel",
            name = "موتور باد گازوئیلی",
            category = EquipmentCategory.COMPRESSOR_DIESEL.name,
            hasWorkHours = true,
            hasEngineOil = true,
            hasRadiator = true,
            hasGearboxOil = true,
            hasGrease = true,
            hasHydraulicOverflow = false
        ),
        // Machinery
        Equipment(
            equipmentId = "loader_cat_988g",
            name = "لودر کاترپیلار 988-G",
            category = EquipmentCategory.MACHINERY.name,
            hasWorkHours = true,
            hasEngineOil = true,
            hasRadiator = true,
            hasGearboxOil = true,
            hasGrease = true,
            hasHydraulicOverflow = false,
            driverName = "وحید فیروزی",
            driverRole = "نماینده"
        ),
        Equipment(
            equipmentId = "loader_komatsu_600",
            name = "لودر کوماتسو 600-3",
            category = EquipmentCategory.MACHINERY.name,
            hasWorkHours = true,
            hasEngineOil = true,
            hasRadiator = true,
            hasGearboxOil = true,
            hasGrease = true,
            hasHydraulicOverflow = false,
            driverName = "محمد",
            driverRole = "راننده"
        ),
        Equipment(
            equipmentId = "dump_truck",
            name = "دامپتراک",
            category = EquipmentCategory.MACHINERY.name,
            hasWorkHours = true,
            hasEngineOil = true,
            hasRadiator = true,
            hasGearboxOil = true,
            hasGrease = true,
            hasHydraulicOverflow = false,
            driverName = "آقای یاوری",
            driverRole = "راننده"
        ),
        Equipment(
            equipmentId = "excavator_hyundai_500",
            name = "بیل مکانیکی هیوندای 500",
            category = EquipmentCategory.MACHINERY.name,
            hasWorkHours = true,
            hasEngineOil = true,
            hasRadiator = true,
            hasGearboxOil = true,
            hasGrease = true,
            hasHydraulicOverflow = false,
            driverName = "امیر",
            driverRole = "راننده"
        )
    )

    fun getEquipmentById(id: String): Equipment? {
        return predefinedEquipmentList.find { it.equipmentId == id }
    }
}
