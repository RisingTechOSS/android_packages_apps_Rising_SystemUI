/*
 * Copyright (C) 2022 StatiXOS
 * SPDX-License-Identifer: Apache-2.0
 */

package com.rising.android.systemui.biometrics

import android.os.IBinder
import android.os.ServiceManager

import com.google.hardware.biometrics.fingerprint.IFingerprintExt

import javax.inject.Inject

class FingerprintExtProvider @Inject constructor() {
    public fun getExtension(): IFingerprintExt? {
        val binder: IBinder? = ServiceManager.waitForDeclaredService(FINGERPRINT_EXT_SERVICE)
        if (binder == null) {
            return null
        }

        try {
            val fingerprintExt = IFingerprintExt.Stub.asInterface(binder.getExtension())
            return fingerprintExt
        } catch (e: NullPointerException) {
            return null
        }
    }

    companion object {
        const val FINGERPRINT_EXT_SERVICE: String = "android.hardware.biometrics.fingerprint.IFingerprint/default"
    }

}
