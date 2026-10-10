const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
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
  }
});

function pastDate() {
  return new Date(Date.now() - 1000);
}

test("1. Unauthenticated user cannot read user progress", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("2. Authenticated user can create and read their own progress", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Aarav",
      sanctumsCleansed: 1,
      diyasLit: 5,
      puzzlesSolved: 3,
      highestSurvivalSec: 240,
      unlockedCodexIds: ["brahmarakshasa"],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("3. Cross-user cannot read or update another user's progress", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Aarav",
      sanctumsCleansed: 1,
      diyasLit: 5,
      puzzlesSolved: 3,
      highestSurvivalSec: 240,
      unlockedCodexIds: ["brahmarakshasa"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      sanctumsCleansed: 99,
      updatedAt: pastDate(),
    })
  );
});

test("4. Shadow update with ghost field on user progress is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Aarav",
      sanctumsCleansed: 1,
      diyasLit: 5,
      puzzlesSolved: 3,
      highestSurvivalSec: 240,
      unlockedCodexIds: ["brahmarakshasa"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(),
    })
  );
});

test("5. Host can create expedition room, Bob can join and update gameplay state", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const roomRef = aliceDb.collection("expedition_rooms").doc("ROOM01");
  const ts = pastDate();

  await assertSucceeds(
    roomRef.set({
      roomCode: "ROOM01",
      templeId: "brihadeeswarar",
      templeName: "Brihadeeswarar Sanctum",
      entityName: "Brahmarakshasa",
      hostUid: ALICE_UID,
      hostName: "Aarav",
      status: "waiting",
      diyasLitCount: 0,
      sealsUnlocked: 0,
      entityX: 7.5,
      entityY: 7.5,
      entityAlertLevel: 10,
      lastRitualEvent: "Sanctum entered",
      memberUids: [ALICE_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  // Bob queries waiting rooms
  await assertSucceeds(
    bobDb.collection("expedition_rooms").where("status", "==", "waiting").get()
  );

  // Bob joins waiting room
  await assertSucceeds(
    bobDb.collection("expedition_rooms").doc("ROOM01").update({
      memberUids: [ALICE_UID, BOB_UID],
      lastRitualEvent: "Devika joined the ritual",
      updatedAt: pastDate(),
    })
  );

  // Bob lights a Diya during gameplay
  await assertSucceeds(
    bobDb.collection("expedition_rooms").doc("ROOM01").update({
      status: "active",
      diyasLitCount: 1,
      lastRitualEvent: "Sacred Diya #1 ignited",
      updatedAt: pastDate(),
    })
  );
});

test("6. Terminal state locking prevents mutating escaped expedition room", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const roomRef = aliceDb.collection("expedition_rooms").doc("ROOM02");
  const ts = pastDate();

  await assertSucceeds(
    roomRef.set({
      roomCode: "ROOM02",
      templeId: "konark",
      templeName: "Konark Sun Crypt",
      entityName: "Chhaya Pret",
      hostUid: ALICE_UID,
      hostName: "Aarav",
      status: "waiting",
      diyasLitCount: 5,
      sealsUnlocked: 3,
      entityX: 4.0,
      entityY: 4.0,
      entityAlertLevel: 0,
      lastRitualEvent: "Garbhagriha cleansed",
      memberUids: [ALICE_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    roomRef.update({
      status: "escaped",
      updatedAt: pastDate(),
    })
  );

  // Cannot mutate after terminal state 'escaped'
  await assertFails(
    roomRef.update({
      diyasLitCount: 2,
      updatedAt: pastDate(),
    })
  );
});

test("7. Player presence subcollection enforces owner write and valid telemetry", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb
      .collection("expedition_rooms")
      .doc("ROOM01")
      .collection("players")
      .doc(ALICE_UID)
      .set({
        userId: ALICE_UID,
        roomCode: "ROOM01",
        displayName: "Aarav",
        x: 2.5,
        y: 3.5,
        angle: 1.57,
        prana: 95,
        diyaLit: true,
        chantingMantra: false,
        updatedAt: pastDate(),
      })
  );

  // Bob cannot overwrite Alice's presence
  await assertFails(
    bobDb
      .collection("expedition_rooms")
      .doc("ROOM01")
      .collection("players")
      .doc(ALICE_UID)
      .update({
        prana: 0,
        updatedAt: pastDate(),
      })
  );

  // Bob can read players in room
  await assertSucceeds(
    bobDb
      .collection("expedition_rooms")
      .doc("ROOM01")
      .collection("players")
      .where("roomCode", "==", "ROOM01")
      .get()
  );

  // Unauthenticated caller cannot list players in room
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb
      .collection("expedition_rooms")
      .doc("ROOM01")
      .collection("players")
      .get()
  );
});
