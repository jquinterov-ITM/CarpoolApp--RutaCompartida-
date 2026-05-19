# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# DTOs de Firestore
-keep class com.carpoolapp.data.remote.dto.** { *; }

# Hilt
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.** { *; }

# Enums de dominio
-keepclassmembers enum com.carpoolapp.domain.model.** { *; }
