/*
 * Copyright (C) 2022 StatiXOS
 * SPDX-License-Identifer: Apache-2.0
 */

package com.rising.android.systemui.biometrics

import android.os.RemoteException

import com.android.systemui.biometrics.AlternateUdfpsTouchProvider

import com.google.hardware.biometrics.fingerprint.IFingerprintExt

import javax.inject.Inject

class RisingUdfpsTouchProvider @Inject constructor(
    private val fingerprintExtProvider: FingerprintExtProvider
) : AlternateUdfpsTouchProvider {

    private var fingerprintExt: IFingerprintExt? = null

    private fun getFingerprintExt(): IFingerprintExt? {
        val extension = fingerprintExt ?: fingerprintExtProvider.getExtension()
        if (fingerprintExt == null) {
            fingerprintExt = extension
        }
        return extension
    }

    override fun onPointerDown(pointerId: Long, x: Int, y: Int, minor: Float, major: Float) {
        try {
            getFingerprintExt()?.onPointerDown(pointerId, x, y, minor, major)
        } catch (e: RemoteException) {
            fingerprintExt = null
        }
    }

    override fun onPointerUp(pointerId: Long) {
        try {
            getFingerprintExt()?.onPointerUp(pointerId)
        } catch (e: RemoteException) {
            fingerprintExt = null
        }
    }

    override fun onUiReady() {
        try {
            getFingerprintExt()?.onUiReady()
        } catch (e: RemoteException) {
            fingerprintExt = null
        }
    }

}
