# Phase 0: Payload-First Security TDD Specification (Garbhagriha)

## 1. Data Invariants
1. **User Isolation (`/users/{userId}`)**: A `UserProgress` document can only be read, created, updated, or deleted by the authenticated user whose `request.auth.uid == userId` AND `incoming().userId == request.auth.uid`. `createdAt` and `userId` are immutable on update.
2. **Expedition Room Integrity (`/expedition_rooms/{roomId}`)**:
   - Creation requires `isSignedIn()`, `incoming().hostUid == request.auth.uid`, `request.auth.uid in incoming().memberUids`, and valid schema bounds (`status == 'waiting'`).
   - Reading (`get`, `list`) requires `isSignedIn()` and (`resource.data.status == 'waiting'` OR `request.auth.uid in resource.data.memberUids`). Client queries for open rooms filter `.whereEqualTo("status", "waiting")`, and joined rooms filter `.whereArrayContains("memberUids", uid)`.
   - Updates require `isSignedIn()`, `isValidExpeditionRoom(incoming())`, and immutability of `hostUid`, `roomCode`, `templeId`, `templeName`, `entityName`, `createdAt`. Terminal states (`escaped`, `consumed`) cannot be mutated once reached (`existing().status in ['waiting', 'active']`).
3. **Player Presence (`/expedition_rooms/{roomId}/players/{playerId}`)**:
   - Subcollection documents store real-time coordinates and ritual states for players inside a room.
   - Only `request.auth.uid == playerId` AND `incoming().userId == request.auth.uid` can create or update their own presence document.
   - Any authenticated user can read (`get`, `list`) player presences within an expedition room they are observing if `resource.data.userId is string`.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Profile Read**: `auth = null`, `GET /users/alice_123` -> REJECT.
2. **Cross-User Profile Read**: `auth = bob_456`, `GET /users/alice_123` -> REJECT.
3. **Identity Spoofing on Profile Create**: `auth = alice_123`, `SET /users/alice_123` with `userId: "bob_456"` -> REJECT.
4. **Shadow Field Injection on Profile Update**: `auth = alice_123`, `UPDATE /users/alice_123` adding `"isAdmin": true` -> REJECT (`hasOnly` guard).
5. **Immutable Field Mutation (`createdAt`)**: `auth = alice_123`, `UPDATE /users/alice_123` changing `createdAt` -> REJECT.
6. **Value Poisoning on Profile**: `auth = alice_123`, `UPDATE /users/alice_123` with `displayName` of 5000 chars or negative `sanctumsCleansed` -> REJECT.
7. **Unbounded Array Attack on `unlockedCodexIds`**: `auth = alice_123`, `SET /users/alice_123` with 50 items in `unlockedCodexIds` -> REJECT (max 25).
8. **Unfiltered List Query on `expedition_rooms`**: `auth = alice_123`, `LIST /expedition_rooms` without `where("status", "==", "waiting")` or `where("memberUids", "array-contains", uid)` when non-waiting rooms exist -> REJECT.
9. **Spoofed Host UID on Room Creation**: `auth = bob_456`, `CREATE /expedition_rooms/room_1` with `hostUid: "alice_123"` -> REJECT.
10. **Terminal State Resurrection**: `auth = alice_123`, `UPDATE /expedition_rooms/room_1` when `existing().status == 'escaped'` -> REJECT.
11. **Cross-User Player Presence Hijack**: `auth = bob_456`, `SET /expedition_rooms/room_1/players/alice_123` -> REJECT.
12. **Out-of-Bounds Prana Telemetry**: `auth = alice_123`, `SET /expedition_rooms/room_1/players/alice_123` with `prana: 9999` -> REJECT.
