/*
 * Copyright (C) 2022 StatiXOS
 * SPDX-License-Identifer: Apache-2.0
 */

package com.rising.android.systemui.dagger;

import com.android.systemui.dagger.SystemUICoreStartableModule;
import com.android.systemui.dagger.DependencyProvider;
import com.android.systemui.dagger.ReferenceSystemUIModule;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.SysUIComponent;
import com.android.systemui.dagger.SystemUIModule;
import com.android.systemui.keyguard.CustomizationProvider;
import com.android.systemui.statusbar.NotificationInsetsModule;
import com.android.systemui.statusbar.QsFrameTranslateModule;

import dagger.Subcomponent;

@SysUISingleton
@Subcomponent(modules = {
        DependencyProvider.class,
        NotificationInsetsModule.class,
        QsFrameTranslateModule.class,
        RisingComponentBinder.class,
        SystemUICoreStartableModule.class,
        SystemUIModule.class,
        SystemUIRisingBinder.class,
        SystemUIRisingModule.class})

public interface SysUIComponentRising extends SysUIComponent {
    @SysUISingleton
    @Subcomponent.Builder
    interface Builder extends SysUIComponent.Builder {
        SysUIComponentRising build();
    }

    /**
     * Member injection into the supplied argument.
     */
    void inject(CustomizationProvider customizationProvider);

}
