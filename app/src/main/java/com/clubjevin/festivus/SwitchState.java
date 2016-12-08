package com.clubjevin.festivus;

import android.widget.CompoundButton;

/**
 * Created by kevin on 12/6/16.
 */

public class SwitchState implements CompoundButton.OnCheckedChangeListener {
    private Boolean isChecked;

    public Boolean getIsChecked() {
        return isChecked;
    }

    public SwitchState(Boolean isChecked) {
        this.isChecked = isChecked;
    }

    @Override
    public synchronized void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        this.isChecked = isChecked;
    }
}
