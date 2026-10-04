require("fake-indexeddb/auto");

const assert = require("node:assert/strict");
const { randomBytes, randomUUID } = require("node:crypto");
const { after, before, test } = require("node:test");
const { deleteApp, initializeApp } = require("firebase/app");
const {
  deleteApp: deleteAdminApp,
  initializeApp: initializeAdminApp,
} = require("firebase-admin/app");
const { getAuth: getAdminAuth } = require("firebase-admin/auth");
const {
  connectAuthEmulator,
  createUserWithEmailAndPassword,
  getIdTokenResult,
  initializeAuth,
  indexedDBLocalPersistence,
  signInWithEmailAndPassword,
  signOut,
} = require("firebase/auth");

const PROJECT_ID = "demo-no-project";
const EMULATOR_HOST = "127.0.0.1:9099";
const APP_NAME = `auth-emulator-test-${randomUUID()}`;
const TEST_ROLES = [
  "representative",
  "driver",
  "technical_manager",
  "authenticated_user_without_role",
];

let app;
let auth;
let adminApp;
let testAccounts;

function createFirebaseApp() {
  const testApp = initializeApp(
    {
      apiKey: "emulator-only-api-key",
      appId: "1:123456789:web:emulatoronly",
      projectId: PROJECT_ID,
    },
    APP_NAME
  );
  const testAuth = initializeAuth(testApp, {
    persistence: [indexedDBLocalPersistence],
  });
  connectAuthEmulator(testAuth, `http://${EMULATOR_HOST}`, {
    disableWarnings: true,
  });
  return { app: testApp, auth: testAuth };
}

before(async () => {
  if (process.env.FIREBASE_AUTH_EMULATOR_HOST !== EMULATOR_HOST) {
    throw new Error("Refusing to run unless Firebase Auth Emulator is explicitly configured on localhost.");
  }
  if (!PROJECT_ID.startsWith("demo-")) {
    throw new Error("Refusing to run with a non-demo Firebase project ID.");
  }

  ({ app, auth } = createFirebaseApp());
  adminApp = initializeAdminApp({ projectId: PROJECT_ID }, `${APP_NAME}-admin`);
  const adminAuth = getAdminAuth(adminApp);
  testAccounts = new Map();

  for (const role of TEST_ROLES) {
    const email = `${role}-${randomUUID()}@example.test`;
    const password = `${randomBytes(32).toString("hex")}Aa1!`;
    const credential = await createUserWithEmailAndPassword(auth, email, password);
    testAccounts.set(role, { email, password, uid: credential.user.uid });
    if (role !== "authenticated_user_without_role") {
      const claims = { role };
      if (role === "representative") {
        claims.assignedEquipmentId = "loader_cat_988g";
      } else if (role === "driver") {
        claims.assignedEquipmentId = "loader_komatsu_600";
      }
      await adminAuth.setCustomUserClaims(credential.user.uid, claims);
    }
    await signOut(auth);
  }
});

after(async () => {
  if (app) {
    await deleteApp(app);
  }
  if (adminApp) {
    await deleteAdminApp(adminApp);
  }
});

test("Email/password accepts valid credentials in the Auth Emulator", async () => {
  const account = testAccounts.get("representative");
  const credential = await signInWithEmailAndPassword(auth, account.email, account.password);

  assert.equal(credential.user.uid, account.uid);
  await signOut(auth);
  assert.equal(auth.currentUser, null);
});

test("Email/password rejects an incorrect password", async () => {
  const account = testAccounts.get("driver");

  await assert.rejects(
    signInWithEmailAndPassword(auth, account.email, `${account.password}-wrong`),
    (error) => ["auth/invalid-credential", "auth/wrong-password"].includes(error.code)
  );
});

test("an authenticated account without role claims has no role claims", async () => {
  const account = testAccounts.get("authenticated_user_without_role");
  const credential = await signInWithEmailAndPassword(auth, account.email, account.password);
  const token = await getIdTokenResult(credential.user, true);

  assert.equal(token.claims.role, undefined);
  assert.equal(token.claims.assignedEquipmentId, undefined);
  await signOut(auth);
});

test("role test accounts receive only their Emulator-provisioned claims", async () => {
  const expectedClaims = new Map([
    ["representative", { role: "representative", assignedEquipmentId: "loader_cat_988g" }],
    ["driver", { role: "driver", assignedEquipmentId: "loader_komatsu_600" }],
    ["technical_manager", { role: "technical_manager" }],
  ]);

  for (const [role, expected] of expectedClaims) {
    const account = testAccounts.get(role);
    const credential = await signInWithEmailAndPassword(auth, account.email, account.password);
    const token = await getIdTokenResult(credential.user, true);

    assert.equal(token.claims.role, expected.role);
    assert.equal(token.claims.assignedEquipmentId, expected.assignedEquipmentId);
    await signOut(auth);
  }
});

test("Logout clears the active Firebase Auth session", async () => {
  const account = testAccounts.get("technical_manager");
  await signInWithEmailAndPassword(auth, account.email, account.password);

  await signOut(auth);
  assert.equal(auth.currentUser, null);
});

test("IndexedDB persistence restores the signed-in session after app recreation", async () => {
  const account = testAccounts.get("technical_manager");
  const credential = await signInWithEmailAndPassword(auth, account.email, account.password);
  const expectedUid = credential.user.uid;

  await deleteApp(app);
  ({ app, auth } = createFirebaseApp());
  await auth.authStateReady();

  assert.equal(auth.currentUser?.uid, expectedUid);
  await signOut(auth);
});
