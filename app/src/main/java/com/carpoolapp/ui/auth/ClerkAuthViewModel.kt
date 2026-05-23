package com.carpoolapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.browser.customtabs.CustomTabsIntent
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

sealed class ClerkAuthState {
    object Loading : ClerkAuthState()
    object SignedOut : ClerkAuthState()
    object SignedIn : ClerkAuthState()
    data class Error(val message: String) : ClerkAuthState()
}

@HiltViewModel
class ClerkAuthViewModel @Inject constructor() : ViewModel() {
    private val _authState = MutableStateFlow<ClerkAuthState>(ClerkAuthState.SignedOut)
    val authState: StateFlow<ClerkAuthState> = _authState

    fun getSignInUrl(): String {
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=YOUR_GOOGLE_CLIENT_ID&" +
                "redirect_uri=carpoolapp://oauth-callback&" +
                "response_type=code&" +
                "scope=openid%20profile%20email"
    }

    fun getSignUpUrl(): String {
        return getSignInUrl() + "&prompt=consent"
    }

    fun setSignedIn() {
        _authState.value = ClerkAuthState.SignedIn
    }

    fun setError(message: String) {
        _authState.value = ClerkAuthState.Error(message)
    }
}

