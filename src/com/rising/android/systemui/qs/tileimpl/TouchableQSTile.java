package com.rising.android.systemui.qs.tileimpl;

import android.view.View;

// For use with SliderQSTileViewImpl
public interface TouchableQSTile {

    public View.OnTouchListener getTouchListener();

    public String getSettingsSystemKey();
}
