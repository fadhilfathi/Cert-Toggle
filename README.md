# Cert Toggle

An Android utility application built with Jetpack Compose that allows users to quickly inspect and toggle the trust status of target Certificate Authorities (CAs) on the system.

## Features

- **Targeted Filtering**: Automatically scans and filters system-wide certificate authorities matching `DigiCert`, `GlobalSign`, and `SSL` (case-insensitive).
- **Dynamic Trust Status**: Queries the active `AndroidCAStore` KeyStore registry to detect if certificates are currently trusted or blocked.
- **Root & Non-Root Support**:
  - **With Root**: Toggle block/trust programmatically. The app registers disabled CAs under Android's keychain blocklist paths (`/data/misc/user/[id]/cacerts-removed/` and `/data/misc/keychain/cacerts-removed/`) with proper SELinux context restoration via `restorecon`.
  - **Without Root**: Acts as an inspector showing which target certificates are currently trusted/blocked, and provides a direct shortcut link to launch the Android "Trusted credentials" Settings screen.

## Under the Hood

When a certificate is toggled **OFF** (Blocked) on a rooted device, the app copies the certificate's file from its storage path (`/apex/com.android.conscrypt/cacerts/` or `/system/etc/security/cacerts/`) to the keychain blocklist folder:
- `/data/misc/user/<user_id>/cacerts-removed/<hash>.<index>`
- `/data/misc/keychain/cacerts-removed/<hash>.<index>`

Once registered in the blocklist and labeled with the correct SELinux context, the OS immediately ignores the certificates, making them blocked globally for apps and settings. Toggling them **ON** removes these files.

## Project Structure

- `app/` - The Jetpack Compose Material 3 Android project.
  - `src/main/java/.../data/DataRepository.kt` - Handles certificate directory scanning, KeyStore queries, and root-shell execution.
  - `src/main/java/.../ui/main/MainScreen.kt` - Main Compose layout with status card and manual settings intent triggers.
- `Cert Toggle.apk` - The compiled debug APK package ready for installation.

## Build

Compile the debug APK using the Gradle wrapper (requires JDK 17+):
```bash
./gradlew assembleDebug
```
