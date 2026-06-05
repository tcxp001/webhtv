package com.fongmi.android.tv.ui.adapter;

import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class InlineEpisodeAdapter extends RecyclerView.Adapter<InlineEpisodeAdapter.ViewHolder> {

    private static final int COLOR_NORMAL = 0x99263442;
    private static final int COLOR_ACTIVE = 0xCC2AA46B;
    private static final int COLOR_FOCUS = 0xFF2196F3;
    private static final int COLOR_FOCUS_BG = 0xFFEAF2F8;
    private static final int COLOR_TEXT = 0xFFEAF2F8;
    private static final int COLOR_FOCUS_TEXT = 0xFF0B5CAD;

    public interface Listener {
        void onItemClick(Episode item);
    }

    private final Listener listener;
    private final List<Episode> items = new ArrayList<>();
    private Episode selected;

    public InlineEpisodeAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<Episode> values, Episode selected) {
        items.clear();
        if (values != null) items.addAll(values);
        this.selected = selected;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MaterialButton button = new MaterialButton(parent.getContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setSingleLine(true);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setAllCaps(false);
        button.setTextSize(16f);
        button.setPadding(ResUtil.dp2px(10), 0, ResUtil.dp2px(10), 0);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ResUtil.dp2px(42));
        params.setMargins(ResUtil.dp2px(8), ResUtil.dp2px(5), ResUtil.dp2px(8), ResUtil.dp2px(5));
        button.setLayoutParams(params);
        return new ViewHolder(button);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Episode item = items.get(position);
        boolean active = item.equals(selected);
        holder.button.setText(EpisodeAdapter.getTitle(item));
        holder.button.setOnFocusChangeListener(null);
        applyState(holder.button, active, holder.button.hasFocus());
        holder.button.setOnFocusChangeListener((view, focused) -> applyState(holder.button, active, focused));
        holder.button.setOnClickListener(view -> listener.onItemClick(item));
    }

    private void applyState(MaterialButton button, boolean active, boolean focused) {
        button.setTextColor(active ? 0xFFFFFFFF : focused ? COLOR_FOCUS_TEXT : COLOR_TEXT);
        button.setBackgroundTintList(ColorStateList.valueOf(active ? COLOR_ACTIVE : focused ? COLOR_FOCUS_BG : COLOR_NORMAL));
        button.setStrokeColor(ColorStateList.valueOf(active ? 0xFF2AA46B : focused ? COLOR_FOCUS : 0x44FFFFFF));
        button.setStrokeWidth(ResUtil.dp2px(active || focused ? 2 : 1));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final MaterialButton button;

        ViewHolder(@NonNull MaterialButton button) {
            super(button);
            this.button = button;
        }
    }
}
