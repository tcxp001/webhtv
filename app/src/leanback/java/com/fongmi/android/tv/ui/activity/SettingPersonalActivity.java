package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivitySettingPersonalBinding;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.base.BaseActivity;

public class SettingPersonalActivity extends BaseActivity {

    private ActivitySettingPersonalBinding mBinding;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingPersonalActivity.class));
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingPersonalBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mBinding.homeVodAutoLoad.requestFocus();
        setText();
    }

    @Override
    protected void initEvent() {
        mBinding.homeVodAutoLoad.setOnClickListener(this::setHomeVodAutoLoad);
        mBinding.playBackToDetail.setOnClickListener(this::setPlayBackToDetail);
    }

    private void setText() {
        mBinding.homeVodAutoLoadText.setText(getSwitch(Setting.isHomeVodAutoLoad()));
        mBinding.playBackToDetailText.setText(getSwitch(Setting.isPlayBackToDetail()));
    }

    private void setHomeVodAutoLoad(View view) {
        Setting.putHomeVodAutoLoad(!Setting.isHomeVodAutoLoad());
        setText();
    }

    private void setPlayBackToDetail(View view) {
        Setting.putPlayBackToDetail(!Setting.isPlayBackToDetail());
        setText();
    }
}
