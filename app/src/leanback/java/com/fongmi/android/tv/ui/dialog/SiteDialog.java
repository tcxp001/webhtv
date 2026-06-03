package com.fongmi.android.tv.ui.dialog;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.Window;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.impl.SiteListener;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.adapter.SiteAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SiteDialog extends BaseAlertDialog implements SiteAdapter.OnClickListener {

    private static final int GRID_COUNT = 10;
    private static final Pattern GROUP_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");
    private static String selectedGroup = "";

    private RecyclerView.ItemDecoration decoration;
    private DialogSiteBinding binding;
    private SiteListener listener;
    private SiteAdapter adapter;
    private List<String> groups;
    private boolean action;
    private int type;

    public static SiteDialog create() {
        return new SiteDialog();
    }

    public SiteDialog search() {
        type = 1;
        return this;
    }

    public SiteDialog action() {
        action = true;
        return this;
    }

    public void show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
        if (activity instanceof SiteListener) listener = (SiteListener) activity;
    }

    private boolean list() {
        return action && Setting.getSiteMode() == 0;
    }

    private int getCount() {
        return list() ? 1 : Math.min(adapter.getItemCount(), 3);
    }

    private int getIcon() {
        return list() ? com.fongmi.android.tv.R.drawable.ic_site_grid : com.fongmi.android.tv.R.drawable.ic_site_list;
    }

    private float getWidth() {
        return 0.4f + (getCount() - 1) * 0.2f;
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogSiteBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setView(getBinding().getRoot());
    }

    @Override
    protected void initView() {
        adapter = new SiteAdapter(this);
        groups = getGroups();
        if (action) binding.action.setVisibility(View.VISIBLE);
        setGroupView();
        setType(type);
        setRecyclerView();
        setMode();
    }

    @Override
    protected void initEvent() {
        binding.mode.setOnClickListener(this::onMode);
        binding.select.setOnClickListener(v -> adapter.selectAll());
        binding.cancel.setOnClickListener(v -> adapter.cancelAll());
        binding.search.setOnClickListener(v -> setType(v.isSelected() ? 0 : 1));
        binding.change.setOnClickListener(v -> setType(v.isSelected() ? 0 : 2));
    }

    private void setRecyclerView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        if (decoration != null) binding.recycler.removeItemDecoration(decoration);
        binding.recycler.addItemDecoration(decoration = new SpaceItemDecoration(getCount(), 12));
        binding.recycler.setLayoutManager(new GridLayoutManager(requireContext(), getCount()));
        if (!binding.mode.hasFocus()) focusSelectedSite();
    }

    private List<String> getGroups() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Site site : VodConfig.get().getSites()) {
            if (site.isHide()) continue;
            Matcher matcher = GROUP_PATTERN.matcher(site.getName());
            while (matcher.find()) result.add("[" + matcher.group(1) + "]");
        }
        return new ArrayList<>(result);
    }

    private void setGroupView() {
        if (groups.isEmpty()) {
            selectedGroup = "";
            binding.groupScroll.setVisibility(View.GONE);
            return;
        }
        if (!TextUtils.isEmpty(selectedGroup) && !groups.contains(selectedGroup)) selectedGroup = "";
        binding.groupScroll.setVisibility(View.VISIBLE);
        binding.groupList.removeAllViews();
        for (String group : groups) binding.groupList.addView(getGroupView(group));
        setGroupFocus();
        updateGroupView();
        adapter.filter(selectedGroup);
        if (!TextUtils.isEmpty(selectedGroup)) binding.groupScroll.post(this::centerSelectedGroup);
    }

    private TextView getGroupView(String group) {
        TextView view = new TextView(requireContext());
        LinearLayoutCompat.LayoutParams params = new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(ResUtil.dp2px(8));
        view.setLayoutParams(params);
        view.setText(group);
        view.setTextSize(16);
        view.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.site_item_text));
        view.setSingleLine(true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(ResUtil.dp2px(14), ResUtil.dp2px(8), ResUtil.dp2px(14), ResUtil.dp2px(8));
        view.setBackgroundResource(R.drawable.selector_site_group);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setOnClickListener(v -> onGroupClick(group, view));
        return view;
    }

    private void setGroupFocus() {
        View focusTarget = getSelectedGroupView();
        if (focusTarget == null && binding.groupList.getChildCount() > 0) focusTarget = binding.groupList.getChildAt(0);
        if (focusTarget == null) return;
        if (focusTarget.getId() == View.NO_ID) focusTarget.setId(View.generateViewId());
        binding.recycler.setNextFocusUpId(focusTarget.getId());
        for (int i = 0; i < binding.groupList.getChildCount(); i++) {
            View view = binding.groupList.getChildAt(i);
            view.setNextFocusDownId(R.id.recycler);
        }
    }

    private void onGroupClick(String group, View view) {
        selectedGroup = group.equals(selectedGroup) ? "" : group;
        adapter.filter(selectedGroup);
        updateGroupView();
        setGroupFocus();
        setRecyclerView();
        setMode();
        setWidth();
        if (!TextUtils.isEmpty(selectedGroup)) centerGroup(view);
    }

    private void updateGroupView() {
        for (int i = 0; i < binding.groupList.getChildCount(); i++) {
            TextView view = (TextView) binding.groupList.getChildAt(i);
            boolean selected = view.getText().toString().equals(selectedGroup);
            view.setSelected(selected);
            view.setAlpha(TextUtils.isEmpty(selectedGroup) || selected ? 1.0f : 0.5f);
        }
    }

    private void centerSelectedGroup() {
        View selected = getSelectedGroupView();
        if (selected != null) centerGroup(selected);
    }

    private View getSelectedGroupView() {
        for (int i = 0; i < binding.groupList.getChildCount(); i++) {
            TextView view = (TextView) binding.groupList.getChildAt(i);
            if (view.getText().toString().equals(selectedGroup)) return view;
        }
        return null;
    }

    private void centerGroup(View view) {
        binding.groupScroll.smoothScrollTo(Math.max(0, view.getLeft() + view.getWidth() / 2 - binding.groupScroll.getWidth() / 2), 0);
    }

    private void setType(int type) {
        binding.search.setSelected(type == 1);
        binding.change.setSelected(type == 2);
        binding.select.setClickable(type > 0);
        binding.cancel.setClickable(type > 0);
        adapter.setType(this.type = type);
    }

    private void setMode() {
        binding.mode.setEnabled(adapter.getItemCount() > 1);
        binding.mode.setImageResource(getIcon());
    }

    private void setWidth() {
        setWidth(getWidth());
    }

    private void focusSelectedSite() {
        int position = adapter.getSelectedPosition();
        binding.recycler.post(() -> {
            binding.recycler.scrollToPosition(position);
            binding.recycler.post(() -> {
                RecyclerView.ViewHolder holder = binding.recycler.findViewHolderForAdapterPosition(position);
                if (holder != null) holder.itemView.requestFocus();
                else binding.recycler.requestFocus();
            });
        });
    }

    private void onMode(View view) {
        Setting.putSiteMode(Math.abs(Setting.getSiteMode() - 1));
        setRecyclerView();
        setMode();
        setWidth();
    }

    @Override
    public void onItemClick(Site item) {
        if (listener != null) listener.setSite(item);
        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter.getItemCount() == 0) dismiss();
        else {
            setBackground();
            setWidth();
            focusSelectedSite();
        }
    }

    private void setBackground() {
        if (getDialog() == null) return;
        Window window = getDialog().getWindow();
        if (window != null) window.setBackgroundDrawableResource(android.R.color.transparent);
    }
}
