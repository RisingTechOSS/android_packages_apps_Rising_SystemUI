/*
 * Copyright (C) 2022 StatiXOS
 * SPDX-License-Identifer: Apache-2.0
 */

package com.rising.android.systemui;

import android.app.AlarmManager;
import android.content.Context;

import com.android.systemui.Dumpable;
import com.android.systemui.R;
import com.android.systemui.VendorServices;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.flags.FeatureFlags;
import com.android.systemui.statusbar.phone.CentralSurfaces;
import com.android.systemui.statusbar.policy.FlashlightController;

import com.rising.android.systemui.ambient.AmbientIndicationContainer;
import com.rising.android.systemui.ambient.AmbientIndicationService;
import com.rising.android.systemui.elmyra.ElmyraService;
import com.rising.android.systemui.smartpixels.SmartPixelsReceiver;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.inject.Inject;

import dagger.Lazy;

@SysUISingleton
public class RisingServices extends VendorServices {

    private final ArrayList<Object> mServices = new ArrayList<>();
    private final AlarmManager mAlarmManager;
    private final AssistManager mAssistManager;
    private final CentralSurfaces mCentralSurfaces;
    private final Context mContext;
    private final FlashlightController mFlashlightController;

    @Inject
    public RisingServices(Context context, AlarmManager alarmManager, AssistManager assistManager, CentralSurfaces centralSurfaces, FlashlightController flashlightController) {
        super();
        mAlarmManager = alarmManager;
        mAssistManager = assistManager;
        mCentralSurfaces = centralSurfaces;
        mContext = context;
        mFlashlightController = flashlightController;
    }

    @Override
    public void start() {
        addService(new SmartPixelsReceiver(mContext));
        if (mContext.getPackageManager().hasSystemFeature("android.hardware.context_hub") && mContext.getPackageManager().hasSystemFeature("android.hardware.sensor.assist")) {
            addService(new ElmyraService(mContext, mAssistManager, mFlashlightController));
        }
        AmbientIndicationContainer ambientIndicationContainer = (AmbientIndicationContainer) mCentralSurfaces.getNotificationShadeWindowView().findViewById(R.id.ambient_indication_container);
        ambientIndicationContainer.initializeView(mCentralSurfaces);
        addService(new AmbientIndicationService(mContext, ambientIndicationContainer, mAlarmManager));
    }

    @Override
    public void dump(PrintWriter printWriter, String[] strArr) {
        for (int i = 0; i < mServices.size(); i++) {
            if (mServices.get(i) instanceof Dumpable) {
                ((Dumpable) mServices.get(i)).dump(printWriter, strArr);
            }
        }
    }

    private void addService(Object obj) {
        if (obj != null) {
            mServices.add(obj);
        }
    }

}
