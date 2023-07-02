/*
 * Copyright (C) 2021 The Pixel Experience Project
 * Copyright (C) 2022 StatiXOS
 * SPDX-License-Identifer: Apache-2.0
 */

package com.rising.android.systemui.dagger;

import com.android.systemui.CoreStartable;
import com.android.systemui.keyguard.dagger.KeyguardModule;
import com.android.systemui.recents.RecentsModule;

import com.rising.android.systemui.statusbar.dagger.RisingCentralSurfacesModule;

import com.rising.android.systemui.RisingServices;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;

@Module(includes = {RecentsModule.class, RisingCentralSurfacesModule.class, KeyguardModule.class})
public abstract class SystemUIRisingBinder {

    /**
     * Inject into RisingServices.
     */
    @Binds
    @IntoMap
    @ClassKey(RisingServices.class)
    public abstract CoreStartable bindRisingServices(RisingServices sysui);

}
