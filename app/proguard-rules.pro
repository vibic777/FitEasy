# R8 in release builds (D091), following Notio: shrinking and optimisation on, renaming off.
# Names stay readable: the app journal shows stack traces that the owner copies and sends.
-dontobfuscate
-keepattributes SourceFile,LineNumberTable

# The app's own code is kept whole; R8 removes only unused library code. Serialization, SQLCipher and Compose bring their own rules.
-keep class com.fiteasy.** { *; }
-keep class net.zetetic.** { *; }
-keep class javax.jmdns.** { *; }
-dontwarn org.slf4j.**
-dontwarn javax.jmdns.**
