/*
 * Copyright (C) 2022 StatiXOS
 * SPDX-License-Identifer: Apache-2.0
 */

package com.rising.android.systemui;

import android.content.Context;

import com.rising.android.systemui.dagger.DaggerGlobalRootComponentRising;

import com.android.systemui.SystemUIInitializer;
import com.android.systemui.dagger.GlobalRootComponent;

public class SystemUIRisingInitializer extends SystemUIInitializer {

    public SystemUIRisingInitializer(Context context) {
        super(context);
    }

    @Override
    protected GlobalRootComponent.Builder getGlobalRootComponentBuilder() {
        return DaggerGlobalRootComponentRising.builder();
    }
}
