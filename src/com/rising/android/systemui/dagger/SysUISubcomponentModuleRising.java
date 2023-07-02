/*
 * Copyright (C) 2022 StatiXOS
 * SPDX-License-Identifer: Apache-2.0
 */

package com.rising.android.systemui.dagger;

import dagger.Module;

/**
 * Dagger module for including the WMComponent.
 */
@Module(subcomponents = {SysUIComponentRising.class})
public abstract class SysUISubcomponentModuleRising {
}
