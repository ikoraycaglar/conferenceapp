# Conference App — Code Explanation
Name : İsmet Koray Çağlar



This project is a small Android app for **conference participant registration + verification**.
It is written in **Kotlin + Jetpack Compose**, uses **MVVM**, and stores data in a local **Room (SQLite) database**.

The app has **two main screens**:
1. **Registration**: enter participant info + optionally take a profile photo
2. **Verification**: search by User ID and show “Found / Not Found” with color feedback


## What the app does (requirements mapping)

### Registration screen
- **User ID (Unique Integer)**: typed as digits, saved as `Int` in the database (primary key).
- **Full Name**: text input (required).
- **Title**: dropdown list: `Prof`, `Dr.`, `Student`.
- **Registration Type (RadioGroup)**:
  - `1 = Full`
  - `2 = Student`
  - `3 = None`
- **Camera integration**:
  - Tapping the photo area opens an **in-app camera screen** (CameraX).
  - Photo is optional. If the device has no camera (or camera is unavailable), the user can still register.
- **Conference Info**:
  - "Open" button launches the official website in the browser (ACTION_VIEW intent).
  - Current URL is in code: `CONFERENCE_URL = "https://ankarabilim.edu.tr"`

### Verification screen
- Enter **User ID** and press **Verify**.
- If found: show the user’s **Name / Title / Type** and the **saved photo** (if any).
- If not found: show **"User Not Found"** and a red background.
- Background color rules:
  - Type 1 (Full) → **Green**
  - Type 2 (Student) → **Blue**
  - Type 3 (None) → **Orange**
  - Not Found → **Red**


## Architecture (MVVM + Repository + Room)

### Data layer (Room database)
Files (folder: `data/`):
- `ParticipantEntity.kt`  
  Defines the `participants` table:
  - `userId` (Primary Key)
  - `fullName`, `title`, `registrationType`
  - `photoUri` (String?) → stores a FileProvider Uri string for the captured image
- `ParticipantDao.kt`  
  - `insertParticipant()` uses **ABORT** for duplicates (keeps "unique ID" rule strict).
  - `getParticipantById(id)` returns one record or `null`.
- `AppDatabase.kt`  
  Creates the Room database: `conference_db`
- `ParticipantRepository.kt`  
  Small wrapper around the DAO, used by ViewModels.

### ViewModels
- `RegistrationViewModel.kt`
  - Keeps UI state in a `StateFlow` (`RegistrationUiState`)
  - Validates input (ID must be number, name not empty)
  - Calls repository insert
  - Sends messages like "Registered Successfully" or error messages
- `VerificationViewModel.kt`
  - Keeps UI state in a `StateFlow` (`VerificationUiState`)
  - Queries database by ID
  - Sets `notFound = true/false` and updates message

### UI layer (Jetpack Compose)
- `MainActivity.kt`
  - Sets the Compose theme and sets up navigation with `NavHost`
  - Routes:
    - `"register"` → `RegistrationScreen`
    - `"verify"` → `VerificationScreen`
  - Uses a simple `ViewModelProvider.Factory` to pass the Repository to ViewModels

- `RegistrationScreen.kt`
  - Compose UI for the registration form
  - Shows messages via **Snackbar** so feedback is always visible (not hidden off-screen)
  - Handles:
    - Opening website intent
    - Asking for camera permission
    - Launching `CameraCaptureActivity` and receiving the returned photo Uri

- `VerificationScreen.kt`
  - Compose UI for searching by ID
  - Uses the color rules to paint the result panel background
  - Displays photo with Coil (`AsyncImage`) if `photoUri` exists

## Camera implementation (CameraX)

File:
- `CameraCaptureActivity.kt`

Why: Some Android devices/emulators have buggy vendor camera apps when called via `ACTION_IMAGE_CAPTURE`.  
This project uses **CameraX** inside the app, so the behavior is more consistent.

Flow:
1. Registration screen prepares a file path in internal storage:
   - `/files/profiles/profile_<timestamp>.jpg`
2. It launches `CameraCaptureActivity` with `EXTRA_FILE_PATH`.
3. `CameraCaptureActivity`:
   - Binds CameraX preview + ImageCapture
   - Saves the photo to the requested file
   - Returns a **FileProvider Uri string** back to Registration screen

FileProvider:
- Declared in `AndroidManifest.xml`
- Paths configured in `res/xml/file_paths.xml`

Note about edge cases:
- If the camera device becomes unavailable while the camera screen is open (example: unplug webcam on emulator), the preview can become black. The user can press **Cancel** and continue registration without a photo.


## App startup / dependency container

- `ConferenceApp.kt` (Application class)
  - Creates an `AppContainer`
- `AppContainer.kt`
  - Creates the Room database
  - Provides the `ParticipantRepository`

This is a simple "manual DI" approach (lightweight alternative to Hilt).


## How to run / test quickly
1. Open the project in Android Studio
2. Sync Gradle and run on emulator or device
3. On **Registration**:
   - Fill fields, optionally take a photo
   - Press **Register**
4. Go to **Verification**:
   - Enter the same ID
   - Press **Verify**
5. Try a random ID to see "Not Found" (red panel)

## Notes / small design choices
- **Unique ID rule** is enforced by Room primary key. Duplicates are rejected.
- UI feedback uses **Snackbar** so success/failure messages are clear.
- Camera is treated as **optional** so the app still works on emulators or devices without camera hardware.
- If you cant boot the camera because of app bailing out you can change the **delayMillis** variable on "CameraCaptureActivity.kt" at row "137"
