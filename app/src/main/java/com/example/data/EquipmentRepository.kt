package com.example.data

import android.util.Log
import com.example.model.AuditLog
import com.example.model.DailyService
import com.example.model.DailyWorkHour
import com.example.model.Equipment
import com.example.model.InventoryStock
import com.example.model.MonthlyReport
import com.example.model.OilChange
import com.example.model.OilChangeStatus
import com.example.model.OilTransaction
import com.example.model.OilTypes
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

private const val TAG = "EquipmentRepository"

class EquipmentRepository(
    private val db: FirebaseFirestore
) {

    // -------------------------------------------------------------
    // Initialization & Seeding Predefined Equipment & Inventory
    // -------------------------------------------------------------
    suspend fun seedInitialDataIfEmpty() {
        try {
            // Seed Equipment
            val equipCollection = db.collection("equipment")
            for (equip in EquipmentData.predefinedEquipmentList) {
                equipCollection.document(equip.equipmentId).set(
                    mapOf(
                        "equipmentId" to equip.equipmentId,
                        "name" to equip.name,
                        "category" to equip.category,
                        "hasWorkHours" to equip.hasWorkHours,
                        "hasEngineOil" to equip.hasEngineOil,
                        "hasRadiator" to equip.hasRadiator,
                        "hasGearboxOil" to equip.hasGearboxOil,
                        "hasGrease" to equip.hasGrease,
                        "hasHydraulicOverflow" to equip.hasHydraulicOverflow,
                        "driverName" to equip.driverName,
                        "driverRole" to equip.driverRole
                    ),
                    SetOptions.merge()
                ).await()
            }

            // Ensure inventory docs exist (default 0 if not present)
            val invRef = db.collection("inventory")
            val engineSnap = invRef.document(OilTypes.ENGINE_OIL_20W50).get().await()
            if (!engineSnap.exists()) {
                invRef.document(OilTypes.ENGINE_OIL_20W50).set(
                    mapOf(
                        "oilType" to OilTypes.ENGINE_OIL_20W50,
                        "oilTypeName" to OilTypes.ENGINE_OIL_NAME,
                        "currentAmountLiters" to 0.0,
                        "lastUpdated" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val hydSnap = invRef.document(OilTypes.HYDRAULIC_OIL_1068).get().await()
            if (!hydSnap.exists()) {
                invRef.document(OilTypes.HYDRAULIC_OIL_1068).set(
                    mapOf(
                        "oilType" to OilTypes.HYDRAULIC_OIL_1068,
                        "oilTypeName" to OilTypes.HYDRAULIC_OIL_NAME,
                        "currentAmountLiters" to 0.0,
                        "lastUpdated" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in seedInitialDataIfEmpty", e)
        }
    }

    // -------------------------------------------------------------
    // Realtime Flows
    // -------------------------------------------------------------
    fun observeEquipmentList(): Flow<List<Equipment>> = callbackFlow {
        val listener = db.collection("equipment").addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Fallback to predefined if network/permission issue
                trySend(EquipmentData.predefinedEquipmentList)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Equipment::class.java)
                }
                trySend(list)
            } else {
                trySend(EquipmentData.predefinedEquipmentList)
            }
        }
        awaitClose { listener.remove() }
    }

    fun observeInventory(): Flow<List<InventoryStock>> = callbackFlow {
        val listener = db.collection("inventory").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing inventory", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val type = doc.getString("oilType") ?: doc.id
                    val name = doc.getString("oilTypeName") ?: OilTypes.getDisplayName(type)
                    val amount = doc.getDouble("currentAmountLiters") ?: 0.0
                    val updated = doc.getTimestamp("lastUpdated")
                    InventoryStock(type, name, amount, updated)
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    fun observeOilTransactions(): Flow<List<OilTransaction>> = callbackFlow {
        val listener = db.collection("oil_transactions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing transactions", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(OilTransaction::class.java)
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeWorkHoursForEquipment(equipmentId: String): Flow<List<DailyWorkHour>> = callbackFlow {
        val listener = db.collection("daily_work_hours")
            .whereEqualTo("equipmentId", equipmentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing work hours", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(DailyWorkHour::class.java)
                    }.sortedByDescending { it.date }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeDailyServicesForEquipment(equipmentId: String): Flow<List<DailyService>> = callbackFlow {
        val listener = db.collection("daily_services")
            .whereEqualTo("equipmentId", equipmentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing daily services", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(DailyService::class.java)
                    }.sortedByDescending { it.date }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeOilChangesForEquipment(equipmentId: String): Flow<List<OilChange>> = callbackFlow {
        val listener = db.collection("oil_changes")
            .whereEqualTo("equipmentId", equipmentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing oil changes", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(OilChange::class.java)
                    }.sortedByDescending { it.date }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeMonthlyReports(): Flow<List<MonthlyReport>> = callbackFlow {
        val listener = db.collection("monthly_reports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing monthly reports", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(MonthlyReport::class.java)
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // -------------------------------------------------------------
    // Business Rule: Logging Daily Work Hours
    // -------------------------------------------------------------
    suspend fun recordWorkHours(
        user: UserProfile,
        equipmentId: String,
        date: String,
        hours: Double
    ): Result<Unit> {
        // Validation: hours 0..12
        if (hours < 0.0 || hours > 12.0) {
            return Result.failure(Exception("ساعت کاری روزانه باید عددی بین ۰ تا ۱۲ ساعت باشد. مقدار واردشده: $hours"))
        }

        // Permission check
        when (user.roleEnum) {
            UserRole.DRIVER -> {
                if (user.assignedEquipmentId != equipmentId) {
                    return Result.failure(Exception("شما به عنوان راننده فقط مجاز به ثبت ساعت کاری وسیله اختصاصی خود هستید."))
                }
            }
            UserRole.REPRESENTATIVE -> {
                // Allowed for all equipment that track work hours
            }
            UserRole.TECHNICAL_MANAGER -> {
                // Allowed for all equipment
            }
        }

        // Shift and Date rule
        val canSubmit = JalaliDateHelper.canSubmitWorkHourForDate(date)
        if (!canSubmit.first) {
            return Result.failure(Exception(canSubmit.second ?: "امکان ثبت ساعت کاری در این زمان وجود ندارد."))
        }

        val recordId = "${equipmentId}_${date}"
        val docRef = db.collection("daily_work_hours").document(recordId)

        // Check if already recorded
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                return Result.failure(Exception("برای تاریخ $date قبلاً ساعت کاری (${existing.getDouble("hours")} ساعت) ثبت شده است و طبق قوانین، ساعت کاری قابل ویرایش مجدد نیست."))
            }

            val workHour = DailyWorkHour(
                recordId = recordId,
                equipmentId = equipmentId,
                date = date,
                hours = hours,
                userId = user.userId,
                displayNameSnapshot = user.displayName,
                createdAt = Timestamp.now()
            )

            docRef.set(workHour).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in recordWorkHours", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Business Rule: Calculating Oil Change Status (Every 90 Hours)
    // -------------------------------------------------------------
    suspend fun calculateOilChangeStatus(equipmentId: String): Result<OilChangeStatus> {
        val equipment = EquipmentData.getEquipmentById(equipmentId)
        if (equipment?.hasEngineOil != true) {
            return Result.success(OilChangeStatus(0.0, 0.0, 0.0, 0.0, false, 0.0))
        }

        return try {
            // 1. Get latest oil change to determine baseline
            val oilChangesSnap = db.collection("oil_changes")
                .whereEqualTo("equipmentId", equipmentId)
                .get().await()

            val latestOilChange = oilChangesSnap.documents
                .mapNotNull { it.toObject(OilChange::class.java) }
                .maxByOrNull { it.date }

            val baselineDate = latestOilChange?.date ?: "1970-01-01"
            val lastAccumulated = latestOilChange?.accumulatedHours ?: 0.0

            // 2. Sum work hours since baseline
            val workHoursSnap = db.collection("daily_work_hours")
                .whereEqualTo("equipmentId", equipmentId)
                .get().await()

            val allWorkHours = workHoursSnap.documents
                .mapNotNull { it.toObject(DailyWorkHour::class.java) }

            val totalHoursAllTime = allWorkHours.sumOf { it.hours }

            // Hours accumulated specifically since the last oil change
            val hoursSinceLastChange = if (latestOilChange != null) {
                allWorkHours.filter { it.date > baselineDate }.sumOf { it.hours }
            } else {
                totalHoursAllTime
            }

            val intervalTarget = 90.0
            val remainingHours = (intervalTarget - hoursSinceLastChange).coerceAtLeast(0.0)
            val isDue = hoursSinceLastChange >= intervalTarget
            val overdueHours = (hoursSinceLastChange - intervalTarget).coerceAtLeast(0.0)

            Result.success(
                OilChangeStatus(
                    accumulatedHours = totalHoursAllTime,
                    baselineHours = lastAccumulated,
                    currentIntervalHours = hoursSinceLastChange,
                    remainingHours = remainingHours,
                    isDue = isDue,
                    overdueHours = overdueHours
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating oil change status for $equipmentId", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Business Rule: Recording Oil Change
    // -------------------------------------------------------------
    suspend fun recordOilChange(
        user: UserProfile,
        equipmentId: String,
        date: String,
        oilAmountLiters: Double,
        dieselFilter: Boolean,
        oilFilter: Boolean,
        waterSeparatorFilter: Boolean,
        notes: String
    ): Result<Unit> {
        if (user.roleEnum != UserRole.TECHNICAL_MANAGER) {
            return Result.failure(Exception("تنها مسئول فنی مجاز به ثبت تعویض روغن است."))
        }

        if (oilAmountLiters <= 0) {
            return Result.failure(Exception("مقدار مصرف روغن موتور باید بیشتر از صفر باشد."))
        }

        val equipment = EquipmentData.getEquipmentById(equipmentId)
            ?: return Result.failure(Exception("وسیله مورد نظر یافت نشد."))

        if (!equipment.hasEngineOil) {
            return Result.failure(Exception("این وسیله دارای موتور احتراقی نبوده و تعویض روغن موتور برای آن تعریف نشده است."))
        }

        val status = calculateOilChangeStatus(equipmentId).getOrNull()
        val accumulatedAtChange = status?.accumulatedHours ?: 0.0

        val recordId = UUID.randomUUID().toString()
        val txId = UUID.randomUUID().toString()
        val currentTime = JalaliDateHelper.getCurrentTime()

        val oilDocRef = db.collection("inventory").document(OilTypes.ENGINE_OIL_20W50)
        val oilChangeRef = db.collection("oil_changes").document(recordId)
        val txRef = db.collection("oil_transactions").document(txId)

        return try {
            db.runTransaction { transaction ->
                val invSnapshot = transaction.get(oilDocRef)
                val currentStock = invSnapshot.getDouble("currentAmountLiters") ?: 0.0

                if (currentStock < oilAmountLiters) {
                    throw IllegalStateException(
                        "موجودی ${OilTypes.ENGINE_OIL_NAME} کافی نیست. موجودی فعلی: $currentStock لیتر. مقدار درخواستی: $oilAmountLiters لیتر."
                    )
                }

                val newStock = currentStock - oilAmountLiters

                // 1. Decrement inventory
                transaction.update(
                    oilDocRef,
                    mapOf(
                        "currentAmountLiters" to newStock,
                        "lastUpdated" to FieldValue.serverTimestamp()
                    )
                )

                // 2. Create OilChange record
                val oilChange = OilChange(
                    recordId = recordId,
                    equipmentId = equipmentId,
                    date = date,
                    time = currentTime,
                    accumulatedHours = accumulatedAtChange,
                    oilAmountLiters = oilAmountLiters,
                    dieselFilter = dieselFilter,
                    oilFilter = oilFilter,
                    waterSeparatorFilter = waterSeparatorFilter,
                    notes = notes,
                    userId = user.userId,
                    displayNameSnapshot = user.displayName,
                    createdAt = Timestamp.now()
                )
                transaction.set(oilChangeRef, oilChange)

                // 3. Create OilTransaction record (outflow)
                val filterDesc = buildList {
                    if (dieselFilter) add("فیلتر گازوئیل")
                    if (oilFilter) add("فیلتر روغن")
                    if (waterSeparatorFilter) add("فیلتر آبگیر")
                }.joinToString("، ")

                val txReason = "تعویض روغن ${equipment.name}" + if (filterDesc.isNotEmpty()) " (تعویض: $filterDesc)" else ""
                val tx = OilTransaction(
                    transactionId = txId,
                    oilType = OilTypes.ENGINE_OIL_20W50,
                    direction = "out",
                    amountLiters = oilAmountLiters,
                    equipmentId = equipmentId,
                    sourceOperation = "oil_change",
                    sourceOperationId = recordId,
                    reason = txReason,
                    date = date,
                    time = currentTime,
                    notes = notes,
                    userId = user.userId,
                    displayNameSnapshot = user.displayName,
                    createdAt = Timestamp.now()
                )
                transaction.set(txRef, tx)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in recordOilChange", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Business Rule: Daily Service & Service Oil Consumption
    // -------------------------------------------------------------
    suspend fun recordDailyService(
        user: UserProfile,
        service: DailyService,
        oilConsumptions: List<ServiceOilConsumption> = emptyList()
    ): Result<Unit> {
        if (user.roleEnum != UserRole.TECHNICAL_MANAGER) {
            return Result.failure(Exception("تنها مسئول فنی مجاز به ثبت سرویس روزانه است."))
        }

        val recordId = "${service.equipmentId}_${service.date}"
        val serviceDocRef = db.collection("daily_services").document(recordId)

        return try {
            db.runTransaction { transaction ->
                // Check if any oil consumptions need to be decremented
                for (cons in oilConsumptions) {
                    if (cons.amountLiters > 0) {
                        val invRef = db.collection("inventory").document(cons.oilType)
                        val invSnap = transaction.get(invRef)
                        val currentStock = invSnap.getDouble("currentAmountLiters") ?: 0.0

                        if (currentStock < cons.amountLiters) {
                            val oilName = OilTypes.getDisplayName(cons.oilType)
                            throw IllegalStateException(
                                "موجودی $oilName کافی نیست. موجودی فعلی: $currentStock لیتر. مقدار درخواستی: ${cons.amountLiters} لیتر."
                            )
                        }

                        // Decrement stock
                        val newStock = currentStock - cons.amountLiters
                        transaction.update(
                            invRef,
                            mapOf(
                                "currentAmountLiters" to newStock,
                                "lastUpdated" to FieldValue.serverTimestamp()
                            )
                        )

                        // Create transaction record
                        val txId = UUID.randomUUID().toString()
                        val txRef = db.collection("oil_transactions").document(txId)
                        val tx = OilTransaction(
                            transactionId = txId,
                            oilType = cons.oilType,
                            direction = "out",
                            amountLiters = cons.amountLiters,
                            equipmentId = service.equipmentId,
                            sourceOperation = cons.operationType,
                            sourceOperationId = recordId,
                            reason = cons.reason,
                            date = service.date,
                            time = JalaliDateHelper.getCurrentTime(),
                            notes = cons.notes,
                            userId = user.userId,
                            displayNameSnapshot = user.displayName,
                            createdAt = Timestamp.now()
                        )
                        transaction.set(txRef, tx)
                    }
                }

                // Save daily service
                val finalService = service.copy(
                    recordId = recordId,
                    userId = user.userId,
                    displayNameSnapshot = user.displayName,
                    createdAt = Timestamp.now()
                )
                transaction.set(serviceDocRef, finalService)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in recordDailyService", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Business Rule: Electric Compressor Hydraulic Overflow
    // -------------------------------------------------------------
    suspend fun recordHydraulicOverflow(
        user: UserProfile,
        equipmentId: String,
        amountLiters: Double,
        notes: String
    ): Result<Unit> {
        if (amountLiters <= 0) {
            return Result.failure(Exception("مقدار سرریز روغن هیدرولیک باید بیشتر از صفر باشد."))
        }

        val equipment = EquipmentData.getEquipmentById(equipmentId)
            ?: return Result.failure(Exception("وسیله یافت نشد."))

        val invRef = db.collection("inventory").document(OilTypes.HYDRAULIC_OIL_1068)
        val txId = UUID.randomUUID().toString()
        val txRef = db.collection("oil_transactions").document(txId)
        val today = JalaliDateHelper.getTodayIsoDate()
        val time = JalaliDateHelper.getCurrentTime()

        return try {
            db.runTransaction { transaction ->
                val invSnap = transaction.get(invRef)
                val currentStock = invSnap.getDouble("currentAmountLiters") ?: 0.0

                if (currentStock < amountLiters) {
                    throw IllegalStateException(
                        "موجودی ${OilTypes.HYDRAULIC_OIL_NAME} کافی نیست. موجودی فعلی: $currentStock لیتر."
                    )
                }

                val newStock = currentStock - amountLiters
                transaction.update(
                    invRef,
                    mapOf(
                        "currentAmountLiters" to newStock,
                        "lastUpdated" to FieldValue.serverTimestamp()
                    )
                )

                val tx = OilTransaction(
                    transactionId = txId,
                    oilType = OilTypes.HYDRAULIC_OIL_1068,
                    direction = "out",
                    amountLiters = amountLiters,
                    equipmentId = equipmentId,
                    sourceOperation = "hydraulic_overflow",
                    reason = "سرریز روغن هیدرولیک ${equipment.name}",
                    date = today,
                    time = time,
                    notes = notes,
                    userId = user.userId,
                    displayNameSnapshot = user.displayName,
                    createdAt = Timestamp.now()
                )
                transaction.set(txRef, tx)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in recordHydraulicOverflow", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Business Rule: Inventory Incoming & Initial Stock
    // -------------------------------------------------------------
    suspend fun recordIncomingOil(
        user: UserProfile,
        oilType: String,
        amountLiters: Double,
        date: String,
        time: String,
        notes: String,
        isInitial: Boolean = false
    ): Result<Unit> {
        if (user.roleEnum != UserRole.REPRESENTATIVE && user.roleEnum != UserRole.TECHNICAL_MANAGER) {
            return Result.failure(Exception("تنها نماینده و مسئول فنی مجاز به ثبت ورود روغن به انبار هستند."))
        }

        if (amountLiters <= 0) {
            return Result.failure(Exception("مقدار روغن وارده باید بیشتر از صفر باشد."))
        }

        val invRef = db.collection("inventory").document(oilType)
        val txId = UUID.randomUUID().toString()
        val txRef = db.collection("oil_transactions").document(txId)

        val reason = if (isInitial) "موجودی اولیه انبار" else "ورود روغن به انبار"
        val op = if (isInitial) "initial" else "incoming"

        return try {
            db.runTransaction { transaction ->
                val invSnap = transaction.get(invRef)
                val currentStock = if (invSnap.exists()) invSnap.getDouble("currentAmountLiters") ?: 0.0 else 0.0
                val newStock = currentStock + amountLiters

                transaction.set(
                    invRef,
                    mapOf(
                        "oilType" to oilType,
                        "oilTypeName" to OilTypes.getDisplayName(oilType),
                        "currentAmountLiters" to newStock,
                        "lastUpdated" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )

                val tx = OilTransaction(
                    transactionId = txId,
                    oilType = oilType,
                    direction = "in",
                    amountLiters = amountLiters,
                    equipmentId = null,
                    sourceOperation = op,
                    reason = reason,
                    date = date,
                    time = time,
                    notes = notes,
                    userId = user.userId,
                    displayNameSnapshot = user.displayName,
                    createdAt = Timestamp.now()
                )
                transaction.set(txRef, tx)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in recordIncomingOil", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Business Rule: Controlled Correction (Technical Manager Only)
    // -------------------------------------------------------------
    suspend fun correctOilTransaction(
        user: UserProfile,
        transactionId: String,
        newAmountLiters: Double,
        reasonNote: String
    ): Result<Unit> {
        if (user.roleEnum != UserRole.TECHNICAL_MANAGER) {
            return Result.failure(Exception("تنها مسئول فنی اجازه اصلاح اطلاعات را دارد."))
        }

        if (newAmountLiters <= 0) {
            return Result.failure(Exception("مقدار اصلاح‌شده باید بیشتر از صفر باشد."))
        }

        val txRef = db.collection("oil_transactions").document(transactionId)

        return try {
            db.runTransaction { transaction ->
                val txSnap = transaction.get(txRef)
                if (!txSnap.exists()) {
                    throw IllegalStateException("تراکنش یافت نشد.")
                }

                val oldAmount = txSnap.getDouble("amountLiters") ?: 0.0
                val direction = txSnap.getString("direction") ?: "out"
                val oilType = txSnap.getString("oilType") ?: OilTypes.ENGINE_OIL_20W50

                val invRef = db.collection("inventory").document(oilType)
                val invSnap = transaction.get(invRef)
                val currentStock = invSnap.getDouble("currentAmountLiters") ?: 0.0

                // Calculate difference
                // e.g., if direction is OUT: old was 20, new is 18. diff = 2. newStock = currentStock + 2
                // if direction is OUT: old was 18, new is 20. diff = -2. newStock = currentStock - 2
                val delta = newAmountLiters - oldAmount
                val adjustedStock = if (direction == "out") {
                    currentStock - delta
                } else {
                    currentStock + delta
                }

                if (adjustedStock < 0) {
                    throw IllegalStateException("این اصلاح باعث منفی شدن موجودی انبار می‌شود (موجودی فعلی: $currentStock).")
                }

                // 1. Update inventory
                transaction.update(
                    invRef,
                    mapOf(
                        "currentAmountLiters" to adjustedStock,
                        "lastUpdated" to FieldValue.serverTimestamp()
                    )
                )

                // 2. Update transaction
                val existingNotes = txSnap.getString("notes") ?: ""
                val updatedNotes = "$existingNotes [اصلاح توسط ${user.displayName}: مقدار قبلی $oldAmount به $newAmountLiters تغییر یافت. دلیل: $reasonNote]".trim()

                transaction.update(
                    txRef,
                    mapOf(
                        "amountLiters" to newAmountLiters,
                        "notes" to updatedNotes
                    )
                )

                // 3. Create AuditLog
                val auditId = UUID.randomUUID().toString()
                val auditRef = db.collection("audit_logs").document(auditId)
                val audit = AuditLog(
                    auditId = auditId,
                    entityType = "oil_transaction",
                    entityId = transactionId,
                    action = "edit_oil_amount",
                    oldValueJson = "amount: $oldAmount",
                    newValueJson = "amount: $newAmountLiters, note: $reasonNote",
                    userId = user.userId,
                    displayName = user.displayName,
                    createdAt = Timestamp.now()
                )
                transaction.set(auditRef, audit)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in correctOilTransaction", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Daily Service Backlog Detection
    // -------------------------------------------------------------
    suspend fun getDailyServiceBacklog(equipmentId: String, daysToCheck: Int = 10): List<String> {
        val recentDays = JalaliDateHelper.getRecentDays(daysToCheck)
        val servicesSnap = db.collection("daily_services")
            .whereEqualTo("equipmentId", equipmentId)
            .get().await()

        val completedDates = servicesSnap.documents.mapNotNull { it.getString("date") }.toSet()

        // Days that are missing daily service, in chronological order (oldest first)
        return recentDays.filter { it !in completedDates }.sorted()
    }

    // -------------------------------------------------------------
    // Close Month & Generate Monthly Report
    // -------------------------------------------------------------
    suspend fun closeMonth(
        user: UserProfile,
        monthKey: String // e.g. "1405-07"
    ): Result<List<MonthlyReport>> {
        if (user.roleEnum != UserRole.TECHNICAL_MANAGER) {
            return Result.failure(Exception("تنها مسئول فنی مجاز به بستن ماه و جمع‌بندی گزارش است."))
        }

        // 1. Verify if any equipment has overdue/missing daily services in the last 7 days
        val allEquip = EquipmentData.predefinedEquipmentList
        val incompleteEquipList = mutableListOf<String>()

        for (eq in allEquip) {
            val backlog = getDailyServiceBacklog(eq.equipmentId, daysToCheck = 7)
            if (backlog.isNotEmpty()) {
                incompleteEquipList.add("${eq.name} (${backlog.size} روز عقب‌افتاده)")
            }
        }

        if (incompleteEquipList.isNotEmpty()) {
            return Result.failure(
                Exception(
                    "امکان جمع‌بندی ماه وجود ندارد زیرا سرویس‌های روزانه عقب‌افتاده برای موارد زیر تکمیل نشده است:\n" +
                            incompleteEquipList.joinToString("\n") +
                            "\nلطفاً ابتدا تمام سرویس‌های روزانه را تکمیل نمایید."
                )
            )
        }

        // 2. Build snapshot report for each equipment
        val reports = mutableListOf<MonthlyReport>()

        for (eq in allEquip) {
            val reportId = "${monthKey}_${eq.equipmentId}"
            val existingReport = db.collection("monthly_reports").document(reportId).get().await()
            if (existingReport.exists()) {
                reports.add(existingReport.toObject(MonthlyReport::class.java)!!)
                continue
            }

            // Gather all work hours, services, and oil changes
            val workHoursSnap = db.collection("daily_work_hours")
                .whereEqualTo("equipmentId", eq.equipmentId)
                .get().await()
            val workHoursList = workHoursSnap.documents.mapNotNull { it.toObject(DailyWorkHour::class.java) }

            val servicesSnap = db.collection("daily_services")
                .whereEqualTo("equipmentId", eq.equipmentId)
                .get().await()
            val servicesList = servicesSnap.documents.mapNotNull { it.toObject(DailyService::class.java) }

            val oilChangesSnap = db.collection("oil_changes")
                .whereEqualTo("equipmentId", eq.equipmentId)
                .get().await()
            val oilChangesList = oilChangesSnap.documents.mapNotNull { it.toObject(OilChange::class.java) }

            val totalWorkHours = workHoursList.sumOf { it.hours }
            val totalOilConsumed = oilChangesList.sumOf { it.oilAmountLiters }

            // Create immutable snapshot JSON string
            val snapshotJson = buildString {
                append("{")
                append("\"equipmentId\":\"${eq.equipmentId}\",")
                append("\"equipmentName\":\"${eq.name}\",")
                append("\"month\":\"$monthKey\",")
                append("\"totalWorkHours\":$totalWorkHours,")
                append("\"workDaysCount\":${workHoursList.size},")
                append("\"dailyServicesCount\":${servicesList.size},")
                append("\"oilChangesCount\":${oilChangesList.size},")
                append("\"totalOilConsumed\":$totalOilConsumed,")
                append("\"closedBy\":\"${user.displayName}\"")
                append("}")
            }

            val report = MonthlyReport(
                reportId = reportId,
                month = monthKey,
                equipmentId = eq.equipmentId,
                equipmentName = eq.name,
                totalWorkHours = totalWorkHours,
                workHoursCount = workHoursList.size,
                dailyServicesCount = servicesList.size,
                oilChangesCount = oilChangesList.size,
                totalOilConsumed = totalOilConsumed,
                snapshotDataJson = snapshotJson,
                closedByUserId = user.userId,
                closedByDisplayName = user.displayName,
                createdAt = Timestamp.now()
            )

            db.collection("monthly_reports").document(reportId).set(report).await()
            reports.add(report)
        }

        return Result.success(reports)
    }
}

data class ServiceOilConsumption(
    val oilType: String,
    val amountLiters: Double,
    val operationType: String,
    val reason: String,
    val notes: String = ""
)
