const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const VAHID_UID = "user_vahid";
const MOHAMMAD_UID = "user_driver_mohammad";
const TECH_MGR_UID = "user_technical_manager";

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: "127.0.0.1",
      port: 8085,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();

    // Seed User profiles
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await db.collection("users").doc(VAHID_UID).set({
        userId: VAHID_UID,
        username: "vahid",
        displayName: "وحید فیروزی",
        role: "representative",
        assignedEquipmentId: "loader_cat_988g",
        active: true,
      });
      await db.collection("users").doc(MOHAMMAD_UID).set({
        userId: MOHAMMAD_UID,
        username: "mohammad",
        displayName: "محمد",
        role: "driver",
        assignedEquipmentId: "loader_komatsu_600",
        active: true,
      });
      await db.collection("users").doc(TECH_MGR_UID).set({
        userId: TECH_MGR_UID,
        username: "fani",
        displayName: "مسئول فنی",
        role: "technical_manager",
        active: true,
      });

      // Seed an existing work hour for dump_truck
      await db.collection("daily_work_hours").doc("dump_truck_2026-10-01").set({
        recordId: "dump_truck_2026-10-01",
        equipmentId: "dump_truck",
        date: "2026-10-01",
        hours: 8,
        userId: "user_driver_yavari",
        displayNameSnapshot: "آقای یاوری",
      });

      // Seed an existing work hour for komatsu 600
      await db.collection("daily_work_hours").doc("loader_komatsu_600_2026-10-01").set({
        recordId: "loader_komatsu_600_2026-10-01",
        equipmentId: "loader_komatsu_600",
        date: "2026-10-01",
        hours: 7,
        userId: MOHAMMAD_UID,
        displayNameSnapshot: "محمد",
      });
    });
  }
});

// -------------------------------------------------------------
// Scenario 1: Unauthenticated User Security (Zero Trust)
// -------------------------------------------------------------
test("Unauthenticated user: CANNOT read equipment", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("equipment").get());
});

test("Unauthenticated user: CANNOT read inventory", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("inventory").get());
});

test("Unauthenticated user: CANNOT read work hours", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("daily_work_hours").get());
});

test("Unauthenticated user: CANNOT write work hours", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("daily_work_hours").doc("h_unauth").set({
      recordId: "h_unauth",
      equipmentId: "loader_cat_988g",
      date: "2026-10-03",
      hours: 5,
      userId: "anon",
      displayNameSnapshot: "ناشناس",
    })
  );
});

test("Authenticated user: CANNOT read another user's profile", async () => {
  const authDb = testEnv.authenticatedContext(MOHAMMAD_UID).firestore();
  await assertFails(authDb.collection("users").doc(VAHID_UID).get());
});

test("Authenticated user: CANNOT create arbitrary admin-grade profile data", async () => {
  const authDb = testEnv.authenticatedContext("new_user_uid").firestore();
  await assertFails(
    authDb.collection("users").doc("new_user_uid").set({
      userId: "new_user_uid",
      username: "attacker",
      displayName: "Attacker",
      role: "technical_manager",
      assignedEquipmentId: "ALL",
      active: true,
    })
  );
});

// -------------------------------------------------------------
// Scenario 2: Driver Permissions & Isolation
// -------------------------------------------------------------
test("Driver: CAN read their assigned equipment work hours", async () => {
  const authDb = testEnv.authenticatedContext(MOHAMMAD_UID).firestore();
  await assertSucceeds(
    authDb.collection("daily_work_hours").doc("loader_komatsu_600_2026-10-01").get()
  );
});

test("Driver: CANNOT read other equipment work hours (dump_truck)", async () => {
  const authDb = testEnv.authenticatedContext(MOHAMMAD_UID).firestore();
  await assertFails(
    authDb.collection("daily_work_hours").doc("dump_truck_2026-10-01").get()
  );
});

test("Driver: CAN log work hours for assigned equipment (Komatsu 600)", async () => {
  const authDb = testEnv.authenticatedContext(MOHAMMAD_UID).firestore();
  await assertSucceeds(
    authDb.collection("daily_work_hours").doc("loader_komatsu_600_2026-10-03").set({
      recordId: "loader_komatsu_600_2026-10-03",
      equipmentId: "loader_komatsu_600",
      date: "2026-10-03",
      hours: 7.5,
      userId: MOHAMMAD_UID,
      displayNameSnapshot: "محمد",
    })
  );
});

test("Driver: CANNOT log work hours for another equipment (dump_truck)", async () => {
  const authDb = testEnv.authenticatedContext(MOHAMMAD_UID).firestore();
  await assertFails(
    authDb.collection("daily_work_hours").doc("dump_truck_2026-10-03").set({
      recordId: "dump_truck_2026-10-03",
      equipmentId: "dump_truck",
      date: "2026-10-03",
      hours: 8,
      userId: MOHAMMAD_UID,
      displayNameSnapshot: "محمد",
    })
  );
});

test("Driver: CANNOT record daily service", async () => {
  const authDb = testEnv.authenticatedContext(MOHAMMAD_UID).firestore();
  await assertFails(
    authDb.collection("daily_services").doc("service_driver_fail").set({
      recordId: "service_driver_fail",
      equipmentId: "loader_komatsu_600",
      date: "2026-10-03",
      userId: MOHAMMAD_UID,
      displayNameSnapshot: "محمد",
    })
  );
});

test("Driver: CANNOT record oil change", async () => {
  const authDb = testEnv.authenticatedContext(MOHAMMAD_UID).firestore();
  await assertFails(
    authDb.collection("oil_changes").doc("oil_change_driver_fail").set({
      recordId: "oil_change_driver_fail",
      equipmentId: "loader_komatsu_600",
      date: "2026-10-03",
      accumulatedHours: 90,
      oilAmountLiters: 18,
      userId: MOHAMMAD_UID,
      displayNameSnapshot: "محمد",
    })
  );
});

// -------------------------------------------------------------
// Scenario 3: Representative Permissions
// -------------------------------------------------------------
test("Representative: CAN log work hours for any equipment (generator_volvo)", async () => {
  const authDb = testEnv.authenticatedContext(VAHID_UID).firestore();
  await assertSucceeds(
    authDb.collection("daily_work_hours").doc("generator_volvo_2026-10-03").set({
      recordId: "generator_volvo_2026-10-03",
      equipmentId: "generator_volvo",
      date: "2026-10-03",
      hours: 6,
      userId: VAHID_UID,
      displayNameSnapshot: "وحید فیروزی",
    })
  );
});

test("Representative: CANNOT record daily service", async () => {
  const authDb = testEnv.authenticatedContext(VAHID_UID).firestore();
  await assertFails(
    authDb.collection("daily_services").doc("service_rep_fail").set({
      recordId: "service_rep_fail",
      equipmentId: "generator_volvo",
      date: "2026-10-03",
      userId: VAHID_UID,
      displayNameSnapshot: "وحید فیروزی",
    })
  );
});

test("Representative: CANNOT record oil change", async () => {
  const authDb = testEnv.authenticatedContext(VAHID_UID).firestore();
  await assertFails(
    authDb.collection("oil_changes").doc("oil_change_rep_fail").set({
      recordId: "oil_change_rep_fail",
      equipmentId: "generator_volvo",
      date: "2026-10-03",
      accumulatedHours: 90,
      oilAmountLiters: 18,
      userId: VAHID_UID,
      displayNameSnapshot: "وحید فیروزی",
    })
  );
});

test("Representative: CAN record incoming oil to inventory", async () => {
  const authDb = testEnv.authenticatedContext(VAHID_UID).firestore();
  await assertSucceeds(
    authDb.collection("oil_transactions").doc("tx_inc_rep").set({
      transactionId: "tx_inc_rep",
      oilType: "engine_oil_20w50",
      direction: "in",
      amountLiters: 100,
      sourceOperation: "incoming",
      reason: "ورود روغن توسط نماینده",
      userId: VAHID_UID,
      displayNameSnapshot: "وحید فیروزی",
    })
  );
});

// -------------------------------------------------------------
// Scenario 4: Technical Manager Permissions
// -------------------------------------------------------------
test("Technical Manager: CAN record daily service", async () => {
  const authDb = testEnv.authenticatedContext(TECH_MGR_UID).firestore();
  await assertSucceeds(
    authDb.collection("daily_services").doc("service_tech_ok").set({
      recordId: "service_tech_ok",
      equipmentId: "loader_komatsu_600",
      date: "2026-10-03",
      userId: TECH_MGR_UID,
      displayNameSnapshot: "مسئول فنی",
    })
  );
});

test("Technical Manager: CAN record oil change", async () => {
  const authDb = testEnv.authenticatedContext(TECH_MGR_UID).firestore();
  await assertSucceeds(
    authDb.collection("oil_changes").doc("oil_change_tech_ok").set({
      recordId: "oil_change_tech_ok",
      equipmentId: "loader_komatsu_600",
      date: "2026-10-03",
      accumulatedHours: 90,
      oilAmountLiters: 18,
      userId: TECH_MGR_UID,
      displayNameSnapshot: "مسئول فنی",
    })
  );
});

test("Technical Manager: CAN close month and create immutable monthly report", async () => {
  const authDb = testEnv.authenticatedContext(TECH_MGR_UID).firestore();
  await assertSucceeds(
    authDb.collection("monthly_reports").doc("rep_1405_07_cat").set({
      reportId: "rep_1405_07_cat",
      month: "1405-07",
      equipmentId: "loader_cat_988g",
      equipmentName: "لودر کاترپیلار 988-G",
      totalWorkHours: 180,
      snapshotDataJson: "{}",
      closedByUserId: TECH_MGR_UID,
      closedByDisplayName: "مسئول فنی",
    })
  );
});

// -------------------------------------------------------------
// Scenario 5: Immutability & Safety Constraints
// -------------------------------------------------------------
test("Immutability: Daily work hour CANNOT be updated or deleted", async () => {
  const authDb = testEnv.authenticatedContext(TECH_MGR_UID).firestore();
  await assertFails(
    authDb.collection("daily_work_hours").doc("loader_komatsu_600_2026-10-01").update({
      hours: 10,
    })
  );
  await assertFails(
    authDb.collection("daily_work_hours").doc("loader_komatsu_600_2026-10-01").delete()
  );
});

test("Immutability: Closed monthly report CANNOT be updated or deleted", async () => {
  const authDb = testEnv.authenticatedContext(TECH_MGR_UID).firestore();
  await authDb.collection("monthly_reports").doc("rep_test_immutable").set({
    reportId: "rep_test_immutable",
    month: "1405-06",
    equipmentId: "dump_truck",
    equipmentName: "دامپتراک",
    totalWorkHours: 120,
    snapshotDataJson: "{}",
    closedByUserId: TECH_MGR_UID,
    closedByDisplayName: "مسئول فنی",
  });

  await assertFails(
    authDb.collection("monthly_reports").doc("rep_test_immutable").update({
      totalWorkHours: 130,
    })
  );
  await assertFails(
    authDb.collection("monthly_reports").doc("rep_test_immutable").delete()
  );
});

test("Non-deletability: Oil transactions CANNOT be deleted by anyone", async () => {
  const authDb = testEnv.authenticatedContext(TECH_MGR_UID).firestore();
  await authDb.collection("oil_transactions").doc("tx_permanent").set({
    transactionId: "tx_permanent",
    oilType: "engine_oil_20w50",
    direction: "in",
    amountLiters: 50,
    sourceOperation: "incoming",
    reason: "ورود روغن",
    userId: TECH_MGR_UID,
    displayNameSnapshot: "مسئول فنی",
  });

  await assertFails(
    authDb.collection("oil_transactions").doc("tx_permanent").delete()
  );
});

test("Safety Constraint: Negative inventory is strictly rejected", async () => {
  const authDb = testEnv.authenticatedContext(TECH_MGR_UID).firestore();
  await assertFails(
    authDb.collection("inventory").doc("engine_oil_20w50").set({
      oilType: "engine_oil_20w50",
      currentAmountLiters: -15,
    })
  );
});

test("Safety Constraint: Work hours > 12 is strictly rejected", async () => {
  const authDb = testEnv.authenticatedContext(MOHAMMAD_UID).firestore();
  await assertFails(
    authDb.collection("daily_work_hours").doc("loader_komatsu_600_2026-10-04").set({
      recordId: "loader_komatsu_600_2026-10-04",
      equipmentId: "loader_komatsu_600",
      date: "2026-10-04",
      hours: 12.5,
      userId: MOHAMMAD_UID,
      displayNameSnapshot: "محمد",
    })
  );
});
