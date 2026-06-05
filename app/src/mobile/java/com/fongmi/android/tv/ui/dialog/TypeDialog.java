package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.databinding.DialogTypeBinding;
import com.fongmi.android.tv.ui.adapter.TypeAdapter;
import com.fongmi.android.tv.ui.adapter.TypeDialogAdapter;

import java.util.List;

public class TypeDialog extends BaseBottomSheetDialog implements TypeAdapter.OnClickListener {

    private DialogTypeBinding binding;
    private TypeAdapter.OnClickListener listener;
    private List<Class> items;

    public static TypeDialog create() {
        return new TypeDialog();
    }

    public TypeDialog items(List<Class> items) {
        this.items = items;
        return this;
    }

    public void show(Fragment fragment) {
        for (Fragment f : fragment.getChildFragmentManager().getFragments()) if (f instanceof TypeDialog) return;
        this.listener = (TypeAdapter.OnClickListener) fragment;
        show(fragment.getChildFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogTypeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setLayoutManager(new GridLayoutManager(requireContext(), getSpanCount()));
        binding.recycler.setAdapter(new TypeDialogAdapter(this, items));
    }

    private int getSpanCount() {
        if (items == null || items.isEmpty()) return 3;
        int length = 0;
        for (Class item : items) length += item.getTypeName().length();
        int average = (int) Math.ceil(length * 1f / items.size());
        if (average >= 12) return 1;
        if (average >= 8) return 2;
        if (average >= 4) return 3;
        return 4;
    }

    @Override
    public void onItemClick(int position, Class item) {
        dismiss();
        listener.onItemClick(position, item);
    }
}
