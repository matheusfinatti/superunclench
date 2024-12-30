## Super Unclench: Privacy policy

Welcome to the Super Unclench app for Android!

This is an open source Android app developed by Matheus Finatti.
As an avid Android user myself, I take privacy very seriously.
I know how frustrating it is when apps collect your data without your knowledge.

### Data collected by the app

I hereby state, to the best of my knowledge and belief, that I have not programmed this app to collect any personally identifiable information. All data (app preferences (like theme) and alarms) created by the you (the user) is stored locally in your device only, and can be simply erased by clearing the app's data or uninstalling it. No analytics software is present in the app either.

### Explanation of permissions requested in the app

| Permission | Why it is required |
| :---: | --- |
| `android.permission.USE_EXACT_ALARM` | Introduced in Android 13 (API level 33), this permission allows the app to set an exact alarm with the Android system, ensuring that the system will wake up from doze mode when the alarm rings. Granted by the system by default and cannot be revoked by the user. Requested in Android 13 and above. |
| `android.permission.SCHEDULE_EXACT_ALARM` | Was introduced in Android 12 and required to set an exact alarm. If your device is running Android 12, the app requests this permission to set an exact alarm. This is the same as `USE_EXACT_ALARM`, except that you, the user, or the system, can revoke this permission at any time from Settings. Revoking this permission will, however, kill the app immediately if it was in foreground, and cancel all alarms set by the app. |
| `com.android.alarm.permission.SET_ALARM` | Basic permission required to set alarms, whether exact or inexact. Automatically granted by the system; cannot be revoked by user. |

If you find any security vulnerability that has been inadvertently caused by me, or have any question regarding how the app protectes your privacy, please send me an email or post a discussion on GitHub, and I will surely try to fix it/help you.

Yours sincerely,  
Matheus Finatti
