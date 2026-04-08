# Keep Activities
-keep public class * extends android.app.Activity

# Keep Fragments
-keep public class * extends androidx.fragment.app.Fragment

# Keep Services
-keep public class * extends android.app.Service

# Keep Broadcast Receivers
-keep public class * extends android.content.BroadcastReceiver

# Keep Application class
-keep public class * extends android.app.Application

# Keep Parcelable
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Keep Retrofit Models
-keepattributes Signature
-keepattributes *Annotation*

# Prevent crash from Gson
-keep class com.google.gson.** { *; }

# Prevent crash from Retrofit
-keep class retrofit2.** { *; }

# Prevent crash from OkHttp
-keep class okhttp3.** { *; }

# Keep Supabase / networking models if used
-keep class io.supabase.** { *; }

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable