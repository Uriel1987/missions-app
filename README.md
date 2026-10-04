# המשימות שלנו — Missions App

A native Android (Kotlin + Jetpack Compose) starter project for the shared couple's
to-do app: tasks with urgency, optional category, optional assignee, optional due
date/time, an optional photo and link, and "pass responsibility" handoffs with
notifications — matching the approved mockup. It runs entirely on Firebase's free
Spark plan: no credit card, no Cloud Functions, no Firebase Storage.

This is a working skeleton, not a finished product: the screens, data model and
sync logic are all real and wired together, but a few things are left as `TODO`s
(see below) for you to fill in once you're building in Android Studio, where you'll
get real error checking and autocomplete.

## 1. Rename the package before you install it

Open `app/build.gradle.kts` and `app/src/main/AndroidManifest.xml` and change
`com.ourhouse.missions` to something unique to you (e.g. `com.ronandmichal.missions`).
**Do this before your first install** — reusing a package name from an older
project causes `INSTALL_FAILED_UPDATE_INCOMPATIBLE` errors when the signatures
don't match.

## 2. Set up Firebase

1. Create a project at https://console.firebase.google.com.
2. Add an Android app to it using the package name you chose above, download the
   generated `google-services.json`, and drop it into `app/`.
3. In the console, turn on:
   - **Firestore Database** (start in production mode; the rules in `firestore.rules`
     restrict access to household members only).
   - **Authentication -> Sign-in method -> Anonymous** (the app signs each phone in
     anonymously so tasks can be assigned to a stable uid without a real account).
   Nothing else is needed — Storage, Cloud Messaging and Cloud Functions all require
   the paid Blaze plan and are deliberately not used (see "Notifications" below).
4. Deploy the security rules:
   ```
   firebase deploy --only firestore:rules
   ```
## 3. Open in Android Studio and run

Open the project root in Android Studio (Hedgehog or newer), let Gradle sync,
and run on two emulators or two physical phones to see the sync working — install
the app on both, pair them with the invite code, and add a task on one to watch
it appear on the other.

## What's implemented vs. left as a TODO

**Implemented:**
- Firestore-backed task list with real-time sync (`TaskRepository`), with load
  errors (permission-denied, offline, etc.) surfaced to the UI instead of an
  infinite spinner
- Household pairing by invite code (`HouseholdRepository`, `PairingScreen`)
- Add task screen: title, note, category, urgency, optional assignee, a real
  Material3 date + time picker, a photo attachment (picked from the device, shrunk
  to a few hundred KB on the phone, and stored in Firestore — see "Photos" below),
  and an optional link
- Task detail + "pass responsibility" flow with a handoff message, showing the
  sender's name (not just their internal id) and the attached image if any
- Local notifications (see "Notifications" below), plus the Android 13+ runtime
  notification-permission request in `MainActivity` (without it, nothing displays)
- Notifications screen (derived from recent handoffs), with the bottom-nav badge
  now reflecting whether something was actually just handed to you
- A "הבית שלנו" household/settings screen: shows the invite code (copy/share),
  the member list, and lets a device disconnect and re-pair
- `firestore.rules` restricts every read/write to household
  members — a signed-in stranger can no longer overwrite someone else's household
- A "Categories" screen (`ui/screens/categories/`) — groups every task by its
  built-in or free-text category, independent of Home's assignee filter, and
  is wired to the bottom-nav button.
- Heebo font — wired via Android's [Downloadable Fonts API](https://developer.android.com/develop/ui/views/text-and-emoji/downloadable-fonts)
  (`ui/theme/Type.kt` + `res/values/font_certs.xml`): Play Services fetches
  and caches the real Heebo weights on-device the first time they're needed,
  so no `.ttf` files are bundled into the APK, and it falls back to the
  system font automatically if Play Services is ever unavailable.
- A max-household-size check on the join flow — capped at 2 members, enforced
  both client-side (`HouseholdRepository.joinHousehold`, with a Hebrew error
  message if the household's already full) and server-side (`firestore.rules`,
  so the cap holds even against a modified client).

**Nothing left that blocks a build** — everything above is implemented.

## Notifications (no push server)

Sending a push needs a trusted server (a Cloud Function), which Firebase only allows
on the paid Blaze plan. Instead each phone watches the shared task list itself and
posts a local notification when a task is assigned to it or handed to it
(`notifications/TaskNotifier`):

- **Live:** while the app process is alive — even in the background — a Firestore
  listener (`MissionsApp.startLiveWatcher`) notifies as soon as the change lands.
- **Background check:** `TaskWatchWorker` re-checks about every 15 minutes (Android's
  minimum for periodic work) for the times the process was killed.
- **Trade-off:** if the app has been closed or force-stopped for a while, a
  notification can arrive up to ~15 minutes late (and not at all while the phone is
  offline). It is suppressed while the app is on screen, since the list is already
  updating in front of you.

## Photos (no Firebase Storage)

`ImageCodec` shrinks a picked photo (max 1280px, JPEG, ~280 KB) and the app stores it as
base64 in `households/{id}/images/{taskId}` — a separate document from the task, so the
task list every screen listens to stays small. `firestore.rules` caps its size.

## App icon

The real app icon is wired up as a proper adaptive icon:
- `res/drawable-xxxhdpi/ic_launcher_foreground.png` — the heart/people mark,
  padded into the adaptive-icon "safe zone" so it survives being cropped to a
  circle, squircle, or any other launcher mask shape without clipping.
- `res/drawable/ic_launcher_background.xml` — a solid fill in the mark's own
  navy (`#0E217D`) so the two layers blend seamlessly under any mask.

No further action needed here — just build and it'll show up correctly on the
home screen and app drawer.

## Project layout

```
app/src/main/java/com/ourhouse/missions/
  data/model/          Task, Household
  data/repository/     Firestore + Auth logic (the "backend" of the app)
  data/local/          SessionManager (remembers which household this phone is in)
  notifications/       Local notifications: TaskNotifier, background worker, display helper
  util/                ImageCodec (photo compression)
  ui/theme/            Colors, type, Material3 theme — matches the mockup palette
  ui/components/       Shared pieces: TaskCard, badges, bottom nav
  ui/screens/          One folder per screen, each with a ViewModel + Composable
  ui/navigation/       The nav graph tying screens together
firestore.rules         Security rules: only household members can read/write it
```

