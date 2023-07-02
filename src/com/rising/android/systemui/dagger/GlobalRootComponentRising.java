/*
 * Copyright (C) 2022 StatiXOS
 * SPDX-License-Identifer: Apache-2.0
 */

package com.rising.android.systemui.dagger;

import android.content.Context;

import com.android.systemui.dagger.GlobalModule;
import com.android.systemui.dagger.GlobalRootComponent;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        GlobalModule.class})
public interface GlobalRootComponentRising extends GlobalRootComponent {

    @Component.Builder
    interface Builder extends GlobalRootComponent.Builder {
        GlobalRootComponentRising build();
    }

    @Override
    SysUIComponentRising.Builder getSysUIComponent();
}
