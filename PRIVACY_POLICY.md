# Privacy Policy

**Last updated:** August 30, 2026

## Arquivo IV — Arquivo digital de faturas

Arquivo IV is developed by **HAConnect**. This privacy policy applies to the Arquivo IV Android application.

---

## TL;DR — No Data Collection

**Arquivo IV does not collect, store, transmit, or share any personal data.**

All your data (invoices, documents, photos, preferences) is stored **exclusively on your device** in a local database (Room/SQLite). There are no analytics, no crash reporters, no ad networks, and no third-party services integrated into the app.

---

## Data Storage

| Data Type | Where It's Stored |
|-----------|-------------------|
| Fatura information (supplier, number, issue date, archive date) | Local Room database on device |
| Photos and documents attached to invoices | App-specific storage on device |
| Theme preferences | Local DataStore on device |
| Retention period alerts | WorkManager (device-only, offline) |

All data remains on your device. You can delete it at any time by uninstalling the app or clearing app data from Android Settings.

---

## Permissions

Arquivo IV requests the following Android permissions, **solely for app functionality**:

| Permission | Purpose |
|------------|---------|
| `POST_NOTIFICATIONS` | To send retention alerts |
| `CAMERA` | To take photos of invoices (only when you choose to) |
| `READ_MEDIA_IMAGES` | To attach existing photos or PDFs to invoices |

No permission is used for tracking, advertising, or data collection. All permissions are optional and requested only when you use the corresponding feature.

---

## Third-Party Services

**None.** Arquivo IV does not integrate any third-party services, SDKs, analytics, ad networks, or crash reporting tools.

The app uses only open-source libraries from the Android ecosystem (Jetpack Compose, Room, Hilt, WorkManager, iText, Coil — see `app/build.gradle.kts` for the full list). None of these libraries transmit data off-device.

---

## Internet Access

Arquivo IV operates **fully offline** for all business data (invoicing, OCR, search, export). The only network access is an **optional** OTA (Over-The-Air) update check: when enabled in Settings, the app queries the GitHub Releases API (`https://api.github.com/repos/halexys-dotcom/Arquivo_IV/releases/latest`) to compare the latest version with the one installed. No personal data, invoices, or device information is transmitted during this check — only the app version name is used for comparison. Downloading and installing updates is always initiated explicitly by the user.

---

## Children's Privacy

Arquivo IV does not collect any data from anyone, including children under 13.

---

## GDPR Compliance

Arquivo IV is fully compliant with the **General Data Protection Regulation (GDPR)** because:

- No personal data is collected or processed
- No data leaves the user's device
- Users retain full control over their data at all times

---

## Contact

For questions about this privacy policy, please contact:

📧 **hah_correia@hotmail.com**

---

**HAConnect**  
Arquivo IV is free and open-source software licensed under the [GNU General Public License v3.0](LICENSE).