package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterSearchBinding;
import com.fongmi.android.tv.databinding.AdapterSearchGridBinding;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.List;

public class SearchAdapter extends BaseDiffAdapter<Vod, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_LIST = 1;
    private static final int VIEW_TYPE_GRID = 2;

    private final OnClickListener listener;
    private int columnCount = 2;
    private int gridWidth;
    private boolean loadImages = true;

    public SearchAdapter(OnClickListener listener) {
        this.listener = listener;
    }

    public interface OnClickListener {

        void onItemClick(Vod item);
    }

    public void setColumnCount(int columnCount) {
        int count = Math.max(1, columnCount);
        if (this.columnCount == count) return;
        this.columnCount = count;
        notifyDataSetChanged();
    }

    public void setGridWidth(int gridWidth) {
        int width = Math.max(0, gridWidth);
        if (this.gridWidth == width) return;
        this.gridWidth = width;
        if (isGrid()) notifyDataSetChanged();
    }

    private boolean isGrid() {
        return columnCount > 1;
    }

    public boolean isGridMode() {
        return isGrid();
    }

    public void setLoadImages(boolean loadImages) {
        this.loadImages = loadImages;
    }

    public boolean isLoadImages() {
        return loadImages;
    }

    public void loadImage(@NonNull RecyclerView.ViewHolder holder) {
        int position = holder.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION || position >= getItemCount()) return;
        Vod item = getItem(position);
        boolean load = loadImages;
        loadImages = true;
        try {
            if (holder instanceof GridViewHolder grid) {
                grid.bindImage(item);
                return;
            }
            if (holder instanceof ListViewHolder list) list.bindImage(item);
        } finally {
            loadImages = load;
        }
    }

    private void loadGridImage(Vod item, ImageView image) {
        if (!loadImages) {
            ImgUtil.hold(item.getPic(), image, true);
            return;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 && image.getParent() instanceof View parent) width = parent.getWidth();
        if (height <= 0) height = image.getLayoutParams().height;
        ImgUtil.loadThumb(item.getName(), item.getPic(), image, width, height);
    }

    private void loadListImage(Vod item, ImageView image) {
        if (!loadImages) {
            ImgUtil.hold(item.getPic(), image, true);
            return;
        }
        ImgUtil.load(item.getName(), item.getPic(), image);
    }

    @Override
    public int getItemViewType(int position) {
        return isGrid() ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_GRID) return new GridViewHolder(AdapterSearchGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)).size(parent, columnCount);
        return new ListViewHolder(AdapterSearchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Vod item = getItem(position);
        if (holder instanceof GridViewHolder grid) {
            grid.bind(item);
            return;
        }
        ((ListViewHolder) holder).bind(item);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }
        Vod item = getItem(position);
        if (holder instanceof GridViewHolder grid) {
            grid.bindImage(item);
            return;
        }
        ((ListViewHolder) holder).bindImage(item);
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        clearImage(holder);
    }

    private void clearImage(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof GridViewHolder grid) {
            ImgUtil.clear(grid.binding.image);
            return;
        }
        if (holder instanceof ListViewHolder list) ImgUtil.clear(list.binding.image);
    }

    public class ListViewHolder extends RecyclerView.ViewHolder {

        private final AdapterSearchBinding binding;

        ListViewHolder(@NonNull AdapterSearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(Vod item) {
            binding.name.setText(item.getName());
            binding.site.setText(item.getSiteName());
            binding.remark.setText(item.getRemarks());
            binding.site.setVisibility(item.getSiteVisible());
            binding.remark.setVisibility(item.getRemarkVisible());
            binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
            bindImage(item);
        }

        private void bindImage(Vod item) {
            loadListImage(item, binding.image);
        }
    }

    public class GridViewHolder extends RecyclerView.ViewHolder {

        private final AdapterSearchGridBinding binding;

        GridViewHolder(@NonNull AdapterSearchGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private GridViewHolder size(ViewGroup parent, int columnCount) {
            int count = Math.max(1, columnCount);
            int margin = ResUtil.dp2px(16);
            int available = gridWidth;
            if (available <= 0) available = parent.getWidth() - parent.getPaddingStart() - parent.getPaddingEnd();
            if (available <= 0) return this;
            int width = Math.max(ResUtil.dp2px(96), (available - margin * count) / count);
            int height = (int) (width / 0.75f);
            if (binding.getRoot().getLayoutParams().width != width) binding.getRoot().getLayoutParams().width = width;
            if (binding.image.getLayoutParams().height != height) binding.image.getLayoutParams().height = height;
            return this;
        }

        private void bind(Vod item) {
            if (binding.getRoot().getParent() instanceof ViewGroup parent) size(parent, columnCount);
            binding.name.setText(item.getName());
            binding.site.setText(item.getSiteName());
            binding.remark.setText(item.getRemarks());
            binding.year.setVisibility(item.getYearVisible());
            binding.year.setText(item.getYear());
            binding.site.setVisibility(item.getSiteVisible());
            binding.name.setVisibility(item.getNameVisible());
            binding.remark.setVisibility(item.getRemarkVisible());
            binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
            bindImage(item);
        }

        private void bindImage(Vod item) {
            loadGridImage(item, binding.image);
        }
    }
}
