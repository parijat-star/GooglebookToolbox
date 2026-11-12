# Security & Privacy Policy

- **100% On-Device Operation**: Googlebook Toolbox stores onboarding preferences and desktop workspace snapshots strictly in local app/device-protected storage (`allowBackup="false"`, cloud backup & device transfer disabled).
- **Zero Cleartext Traffic**: `android:usesCleartextTraffic="false"` is enforced in `AndroidManifest.xml`; support links are strictly validated HTTPS URLs.
- **No Root or ADB Required**: All cross-device checks and desktop window restoration features use standard user-grantable Android APIs.
