# Speed Monitor — LESCO IT Directorate

Android Studio project (Kotlin, JDK 17, compileSdk 35, applicationId `com.lesco.speedmonitor`).

## Features
- Live Internet download/upload/ping test, started from its button.
- Manual Level-1 API and SFTP port checks.
- Complaint email draft with a PIDC support-team complaint subject and generated status details.
- Save a WhatsApp number locally; open WhatsApp with a prefilled complaint. The user reviews and presses Send.
- Background service starts when the dashboard opens, checks Level-1 and SFTP every 15 seconds, and raises a sound/vibration notification when a server changes to down or recovers.
- Stop Background Monitoring button stops the service.
- GitHub Actions builds a debug APK on push to `main` or manual workflow dispatch.

## Important behavior/limitations
- Android does not allow an ordinary app to silently send an email through the user's email account without authenticated mail delivery. The email button opens the installed email composer with the complaint prefilled; the user must press Send. Direct automatic email requires a properly authenticated backend/SMTP or email API integration. Do not put SMTP passwords in the APK.
- WhatsApp opens with the complaint prefilled; the user must press Send.
- Background service is user-visible via an ongoing notification. Android/OEM battery restrictions may still stop background work; allow notifications and, if required by the phone, allow unrestricted battery use for Speed Monitor.
- The SFTP check verifies TCP port 2232 availability/response only; it does not authenticate or measure SFTP file transfer speed.
- Monitoring calls the Level-1 endpoint using the existing sample request values. Confirm those request parameters remain valid for the target environment.

## Build
Open this folder in Android Studio and build `app` > `assembleDebug`, or push to GitHub `main` and download the `Speed-Monitor-APK` artifact from Actions.
