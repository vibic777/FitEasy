# Third-party provenance

Build wrapper and dependency version references were adapted from local Notio-Android/Notio-Windows projects by vibic777 (MIT). FitEasy application sources are developed separately. Any reused implementation is listed here as it is added.

Notio MIT notice: Copyright (c) 2026 vibic777. Permission is granted, free of charge, to any person obtaining a copy of this software and associated documentation files to deal in the Software without restriction, including to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies, subject to inclusion of this copyright and permission notice in all copies or substantial portions. THE SOFTWARE IS PROVIDED AS IS, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. AUTHORS ARE NOT LIABLE FOR CLAIMS OR DAMAGES ARISING FROM USE.

Dependency licenses and catalogue sources must be finalized before public distribution. No Notio signing secrets are included in this repository.

The following implementations are adapted from the local Notio-Windows shared JVM sources (same author's Notio code): SyncCrypto.kt, SyncSession.kt, CredentialHasher.kt, Totp.kt and SmtpClient.kt. Package names are changed to com.fiteasy.shared. The complete upstream MIT notice is preserved in docs/licenses/Notio-MIT.txt and must accompany distributions.

Discovery.kt is also adapted from Notio-Windows, with a separate _fiteasy-sync._tcp.local. service type. FitEasy does not connect to Notio's data protocol.

Android BiometricAuth.kt is adapted from Notio-Android, using the platform prompt and adding explicit completion callbacks.

FitTheme.kt reuses the named palette accent values from Notio-Android core/ui/theme/Color.kt and Notio-Windows desktop/theme/Colors.kt (MIT, Copyright (c) 2026 vibic777). FitEasy implements its own surface/container composition. IconAction.kt follows their accessible tooltip interaction pattern; the FitEasy canvas icons are original. The full MIT notice accompanies distributions in docs/licenses/Notio-MIT.txt.

Managed Windows installers include unmodified WiX DTF runtime/SfxCA and standard WiX UI translations, Copyright (c) .NET Foundation and contributors, under MS-RL. The full license is included as docs/licenses/WiX-MS-RL.txt and packaged licenses/WiX-MS-RL.txt. The Belarusian WiX UI translation in desktopApp/installer/be.json and generated WiX localization resources are also provided under MS-RL. FitEasy's independent installer code and own messages remain separate sources. Build provenance: WiX 3.11 archive already used by Compose packaging; upstream https://github.com/wixtoolset/wix3.
