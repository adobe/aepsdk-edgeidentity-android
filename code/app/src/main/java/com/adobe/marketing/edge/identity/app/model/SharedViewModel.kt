/*
 Copyright 2021 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.marketing.edge.identity.app.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adobe.marketing.mobile.edge.identity.AuthenticatedState

class SharedViewModel : ViewModel() {
    companion object {
        const val REGISTER_IDENTITY_STRING = "Register Identity"
        const val REGISTER_EDGE_IDENTITY_STRING = "Register Edge Identity"
        const val IDENTITY_IS_REGISTERED_STRING = "Identity is registered"
        const val EDGE_IDENTITY_IS_REGISTERED_STRING = "Edge Identity is registered"
    }

    // Models for Get Identities View

    private val _ecidText = MutableLiveData<String>("")
    val ecidText: LiveData<String> = _ecidText

    private val _ecidLegacyText = MutableLiveData<String>("")
    val ecidLegacyText: LiveData<String> = _ecidLegacyText

    private val _identitiesText = MutableLiveData<String>("")
    val identitiesText: LiveData<String> = _identitiesText

    fun setEcidValue(value: String) {
        _ecidText.value = value
    }

    fun setEcidLegacyValue(value: String) {
        _ecidLegacyText.value = value
    }

    fun setIdentitiesValue(value: String) {
        _identitiesText.value = value
    }

    // Models for Update Identities View

    private val _identifier = MutableLiveData<String>("")
    val identifier: LiveData<String> = _identifier

    private val _namespace = MutableLiveData<String>("")
    val namespace: LiveData<String> = _namespace

    private val _isPrimary = MutableLiveData<Boolean>(false)
    val isPrimary: LiveData<Boolean> = _isPrimary

    private val _authenticatedState = MutableLiveData<AuthenticatedState>(AuthenticatedState.AMBIGUOUS)
    val authenticatedState: LiveData<AuthenticatedState> = _authenticatedState

    fun setIdentifier(value: String) {
        if (_identifier.value == value) {
            return
        }
        _identifier.value = value
    }

    fun setNamespace(value: String) {
        if (_namespace.value == value) {
            return
        }
        _namespace.value = value
    }

    fun setIsPrimary(value: Boolean) {
        if (_isPrimary.value == value) {
            return
        }
        _isPrimary.value = value
    }

    fun setAuthenticatedState(value: AuthenticatedState) {
        if (_authenticatedState.value == value) {
            return
        }
        _authenticatedState.value = value
    }

    // Models for Multiple Identities View

    private val _isEdgeIdentityRegistered = MutableLiveData<Boolean>(true)
    val isEdgeIdentityRegistered: LiveData<Boolean> = _isEdgeIdentityRegistered

    private val _edgeIdentityRegisteredText = MutableLiveData<String>().apply {
        value = if (_isEdgeIdentityRegistered.value == true) {
            EDGE_IDENTITY_IS_REGISTERED_STRING
        } else {
            REGISTER_EDGE_IDENTITY_STRING
        }
    }
    val edgeIdentityRegisteredText: LiveData<String> = _edgeIdentityRegisteredText

    private val _isDirectIdentityRegistered = MutableLiveData<Boolean>(false)
    val isDirectIdentityRegistered: LiveData<Boolean> = _isDirectIdentityRegistered

    private val _directIdentityRegisteredText = MutableLiveData<String>().apply {
        value = if (_isDirectIdentityRegistered.value == true) {
            IDENTITY_IS_REGISTERED_STRING
        } else {
            REGISTER_IDENTITY_STRING
        }
    }
    val directIdentityRegisteredText: LiveData<String> = _directIdentityRegisteredText


    fun toggleEdgeIdentityRegistration() {
        if (_isEdgeIdentityRegistered.value == true) {
            _isEdgeIdentityRegistered.value = false
            _edgeIdentityRegisteredText.value = REGISTER_EDGE_IDENTITY_STRING
        } else {
            _isEdgeIdentityRegistered.value = true
            _edgeIdentityRegisteredText.value = EDGE_IDENTITY_IS_REGISTERED_STRING
        }
    }

    fun toggleDirectIdentityRegistration() {
        if (_isDirectIdentityRegistered.value == true) {
            _isDirectIdentityRegistered.value = false
            _directIdentityRegisteredText.value = REGISTER_IDENTITY_STRING
        } else {
            _isDirectIdentityRegistered.value = true
            _directIdentityRegisteredText.value = IDENTITY_IS_REGISTERED_STRING
        }
    }
}