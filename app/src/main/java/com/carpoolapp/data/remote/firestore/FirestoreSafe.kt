package com.carpoolapp.data.remote.firestore

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException

suspend inline fun <T> firestoreSafe(tag: String, fallback: T, crossinline block: suspend () -> T): T {
    return try {
        block()
    } catch (e: Exception) {
        Log.w(tag, "Firestore operation failed", e)
        try {
            val crash = FirebaseCrashlytics.getInstance()
            val isPermissionDenied = e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            if (isPermissionDenied) {
                crash.log("PERMISSION_DENIED on $tag: ${e.message}")
                try {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) crash.setCustomKey("firestore_permission_denied_uid", uid)
                } catch (_: Exception) {}
                crash.setCustomKey("firestore_permission_denied", true)
            }
            crash.recordException(e)
        } catch (_: Exception) {
            // ignore if crashlytics isn't available/initialized
        }
        fallback
    }
}
