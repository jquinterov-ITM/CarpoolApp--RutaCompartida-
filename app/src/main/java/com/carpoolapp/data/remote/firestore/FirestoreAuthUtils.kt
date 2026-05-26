package com.carpoolapp.data.remote.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.ProducerScope

fun requireAuthOrClose(scope: ProducerScope<*>): FirebaseUser? {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        try { scope.close() } catch (_: Exception) {}
        return null
    }
    return user
}
