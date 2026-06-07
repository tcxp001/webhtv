package com.fongmi.android.tv.ui.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.FragmentSettingPersonalBinding;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.base.BaseFragment;

public class SettingPersonalFragment extends BaseFragment {

    private FragmentSettingPersonalBinding mBinding;

    public static SettingPersonalFragment newInstance() {
        return new SettingPersonalFragment();
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingPersonalBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setText();
    }

    @Override
    protected void initEvent() {
        mBinding.playBackToDetail.setOnClickListener(this::setPlayBackToDetail);
    }

    private void setText() {
        mBinding.playBackToDetailText.setText(getSwitch(Setting.isPlayBackToDetail()));
    }

    private void setPlayBackToDetail(View view) {
        Setting.putPlayBackToDetail(!Setting.isPlayBackToDetail());
        setText();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) setText();
    }
}
