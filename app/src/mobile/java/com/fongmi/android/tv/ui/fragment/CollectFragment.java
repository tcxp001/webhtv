package com.fongmi.android.tv.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Collect;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentCollectBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.setting.SiteHealthStore;
import com.fongmi.android.tv.ui.activity.FolderActivity;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.fongmi.android.tv.ui.adapter.CollectAdapter;
import com.fongmi.android.tv.ui.adapter.SearchAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.custom.CustomScroller;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class CollectFragment extends BaseFragment implements MenuProvider, CollectAdapter.OnClickListener, SearchAdapter.OnClickListener, CustomScroller.Callback {

    private static final int MENU_GROUP_ALL = 1;
    private static final int MENU_GROUP_OFFSET = 100;
    private static final int SEARCH_DEFAULT_GRID_COUNT = 2;
    private static final long SEARCH_UPDATE_DELAY = 80;
    private static final long SEARCH_SCROLL_DELAY = 180;
    private static final long SEARCH_FIRST_IMAGE_DELAY = 0;
    private static final long SEARCH_AFTER_SCROLL_DELAY = 240;
    private static final long SEARCH_IMAGE_DELAY = 80;
    private static final int SEARCH_BATCH_SIZE = 24;
    private static final int SEARCH_IMAGE_BATCH_SIZE = 2;
    private static final int COLLECT_BATCH_SIZE = 8;

    private FragmentCollectBinding mBinding;
    private CollectAdapter mCollectAdapter;
    private SearchAdapter mSearchAdapter;
    private CustomScroller mScroller;
    private SiteViewModel mViewModel;
    private List<Site> mSites;
    private List<String> mGroups;
    private final List<Collect> mAllCollectItems;
    private final List<Collect> pendingCollectItems;
    private final List<Vod> pendingSearchItems;
    private final List<Vod> pendingActiveSearchItems;
    private final Runnable flushSearchUpdates;
    private final Runnable restoreSearchImages;
    private String pendingActiveSiteKey;
    private String mFilterGroup;
    private int pendingImageStart;
    private int pendingImageEnd;
    private int loadedImageStart;
    private int loadedImageEnd;
    private boolean imageRestoreScheduled;
    private boolean searchScrolling;

    public CollectFragment() {
        mAllCollectItems = new ArrayList<>();
        pendingCollectItems = new ArrayList<>();
        pendingSearchItems = new ArrayList<>();
        pendingActiveSearchItems = new ArrayList<>();
        flushSearchUpdates = this::flushSearchUpdates;
        restoreSearchImages = this::restoreSearchImages;
        pendingActiveSiteKey = "";
        mFilterGroup = "";
        pendingImageStart = RecyclerView.NO_POSITION;
        pendingImageEnd = RecyclerView.NO_POSITION;
        loadedImageStart = RecyclerView.NO_POSITION;
        loadedImageEnd = RecyclerView.NO_POSITION;
    }

    public static CollectFragment newInstance(String keyword) {
        return newInstance(keyword, "");
    }

    public static CollectFragment newInstance(String keyword, String siteKey) {
        return newInstance(keyword, siteKey, "");
    }

    public static CollectFragment newInstance(String keyword, String siteKey, String group) {
        Bundle args = new Bundle();
        args.putString("keyword", keyword);
        args.putString("siteKey", siteKey);
        args.putString("group", group);
        CollectFragment fragment = new CollectFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private String getKeyword() {
        return getArguments().getString("keyword");
    }

    private String getSiteKey() {
        String siteKey = getArguments().getString("siteKey");
        return siteKey == null ? "" : siteKey;
    }

    private String getSearchGroup() {
        String group = getArguments().getString("group");
        return group == null ? "" : group;
    }

    private boolean isSiteSearch() {
        return !TextUtils.isEmpty(getSiteKey());
    }

    private boolean isGroupSearch() {
        return !TextUtils.isEmpty(getSearchGroup());
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentCollectBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initMenu() {
        if (isHidden()) return;
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar(mBinding.toolbar);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activity.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        activity.setTitle(getTitleText());
    }

    private String getTitleText() {
        if (isSiteSearch()) return getString(R.string.search_result_current, getKeyword());
        if (isGroupSearch()) return getString(R.string.search_result_group, getSearchGroup(), getKeyword());
        return getString(R.string.search_result_all, getKeyword());
    }

    @Override
    protected void initView() {
        mScroller = new CustomScroller(this);
        setRecyclerView();
        setViewModel();
        setSites();
        setSearchLayout();
        search();
    }

    @Override
    protected void initEvent() {
        mBinding.toolbar.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putBoolean("edit", true);
            getParentFragmentManager().setFragmentResult("result", result);
            getParentFragmentManager().popBackStack();
        });
    }

    private void setRecyclerView() {
        mBinding.collect.setItemAnimator(null);
        mBinding.collect.setHasFixedSize(true);
        mBinding.collect.setAdapter(mCollectAdapter = new CollectAdapter(this, isHorizontalUi()));
        mBinding.recycler.setItemAnimator(null);
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.addOnScrollListener(mScroller);
        mBinding.recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                boolean scrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
                if (searchScrolling == scrolling) return;
                searchScrolling = scrolling;
                if (scrolling) {
                    if (mSearchAdapter != null) mSearchAdapter.setLoadImages(false);
                    App.removeCallbacks(flushSearchUpdates, restoreSearchImages);
                    imageRestoreScheduled = false;
                    resetLoadedSearchImages();
                } else {
                    scheduleVisibleSearchImages(SEARCH_AFTER_SCROLL_DELAY);
                    App.post(flushSearchUpdates, SEARCH_AFTER_SCROLL_DELAY);
                }
            }
        });
        mBinding.recycler.setAdapter(mSearchAdapter = new SearchAdapter(this));
    }

    private void scheduleVisibleSearchImages(long delayMillis) {
        if (mBinding == null || mSearchAdapter == null || searchScrolling) return;
        RecyclerView.LayoutManager manager = mBinding.recycler.getLayoutManager();
        if (!(manager instanceof LinearLayoutManager layoutManager)) return;
        int first = layoutManager.findFirstVisibleItemPosition();
        int last = layoutManager.findLastVisibleItemPosition();
        if (first == RecyclerView.NO_POSITION || last < first) return;
        if (isSearchImageRangeLoaded(first, last)) return;
        if (imageRestoreScheduled) {
            if (pendingImageStart == RecyclerView.NO_POSITION || pendingImageEnd < pendingImageStart) pendingImageStart = getFirstUnloadedSearchImage(first, last);
            pendingImageEnd = Math.max(pendingImageEnd, last);
            return;
        }
        pendingImageStart = getFirstUnloadedSearchImage(first, last);
        pendingImageEnd = last;
        if (pendingImageStart == RecyclerView.NO_POSITION || pendingImageEnd < pendingImageStart) return;
        imageRestoreScheduled = true;
        App.post(restoreSearchImages, delayMillis);
    }

    private void restoreSearchImages() {
        imageRestoreScheduled = false;
        if (mBinding == null || mSearchAdapter == null || searchScrolling) return;
        if (pendingImageStart == RecyclerView.NO_POSITION || pendingImageEnd < pendingImageStart) return;
        int count = Math.min(SEARCH_IMAGE_BATCH_SIZE, pendingImageEnd - pendingImageStart + 1);
        for (int i = 0; i < count; i++) {
            int position = pendingImageStart + i;
            RecyclerView.ViewHolder holder = mBinding.recycler.findViewHolderForAdapterPosition(position);
            if (holder != null) {
                mSearchAdapter.loadImage(holder);
                markSearchImageLoaded(position);
            }
        }
        pendingImageStart += count;
        if (pendingImageStart <= pendingImageEnd) {
            imageRestoreScheduled = true;
            App.post(restoreSearchImages, SEARCH_IMAGE_DELAY);
        } else {
            pendingImageStart = RecyclerView.NO_POSITION;
            pendingImageEnd = RecyclerView.NO_POSITION;
            if (!mSearchAdapter.isGridMode()) mSearchAdapter.setLoadImages(true);
        }
    }

    private void addSearchItems(List<Vod> items) {
        if (items.isEmpty()) return;
        if (mSearchAdapter.isGridMode()) mSearchAdapter.setLoadImages(false);
        mSearchAdapter.addAll(items, () -> scheduleVisibleSearchImages(SEARCH_FIRST_IMAGE_DELAY));
    }

    private void setSearchItems(List<Vod> items, Runnable runnable) {
        resetLoadedSearchImages();
        if (mSearchAdapter.isGridMode()) mSearchAdapter.setLoadImages(false);
        mSearchAdapter.setItems(items, () -> {
            runnable.run();
            scheduleVisibleSearchImages(SEARCH_FIRST_IMAGE_DELAY);
        });
    }

    private int getFirstUnloadedSearchImage(int first, int last) {
        if (loadedImageStart == RecyclerView.NO_POSITION || first < loadedImageStart || first > loadedImageEnd) return first;
        int position = loadedImageEnd + 1;
        return position <= last ? position : RecyclerView.NO_POSITION;
    }

    private boolean isSearchImageRangeLoaded(int first, int last) {
        return loadedImageStart != RecyclerView.NO_POSITION && first >= loadedImageStart && last <= loadedImageEnd;
    }

    private void markSearchImageLoaded(int position) {
        if (position == RecyclerView.NO_POSITION) return;
        if (loadedImageStart == RecyclerView.NO_POSITION) {
            loadedImageStart = position;
            loadedImageEnd = position;
            return;
        }
        loadedImageStart = Math.min(loadedImageStart, position);
        loadedImageEnd = Math.max(loadedImageEnd, position);
    }

    private void resetLoadedSearchImages() {
        loadedImageStart = RecyclerView.NO_POSITION;
        loadedImageEnd = RecyclerView.NO_POSITION;
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class).init();
        mViewModel.getSearch().observe(this, this::setCollect);
        mViewModel.getResult().observe(this, this::setSearch);
    }

    private void setSites() {
        String siteKey = getSiteKey();
        String group = getSearchGroup();
        mSites = new ArrayList<>();
        for (Site site : VodConfig.get().getSites()) {
            if (!site.isSearchable()) continue;
            if (!TextUtils.isEmpty(siteKey) && !site.getKey().equals(siteKey)) continue;
            if (!TextUtils.isEmpty(group) && !site.inGroup(group)) continue;
            mSites.add(site);
        }
        SiteHealthStore.sortSites(mSites);
        mGroups = isSiteSearch() || isGroupSearch() ? new ArrayList<>() : Site.getGroups(mSites);
    }

    private void search() {
        if (mSites.isEmpty()) {
            if (isSiteSearch()) Notify.show(R.string.detail_site_not_searchable);
            return;
        }
        Collect all = Collect.all();
        mAllCollectItems.clear();
        mAllCollectItems.add(all);
        mCollectAdapter.setItems(List.of(all), () -> mViewModel.searchContent(mSites, getKeyword(), false));
    }

    private void setSearchLayout() {
        boolean horizontal = isHorizontalUi();
        int gap = ResUtil.dp2px(8);
        mBinding.content.setOrientation(horizontal ? LinearLayoutCompat.VERTICAL : LinearLayoutCompat.HORIZONTAL);
        mBinding.collect.setLayoutManager(new LinearLayoutManager(requireActivity(), horizontal ? LinearLayoutManager.HORIZONTAL : LinearLayoutManager.VERTICAL, false));
        mCollectAdapter.setHorizontal(horizontal);

        LinearLayoutCompat.LayoutParams collectParams = (LinearLayoutCompat.LayoutParams) mBinding.collect.getLayoutParams();
        LinearLayoutCompat.LayoutParams recyclerParams = (LinearLayoutCompat.LayoutParams) mBinding.recycler.getLayoutParams();
        collectParams.width = horizontal ? ViewGroup.LayoutParams.MATCH_PARENT : getCollectWidth();
        collectParams.height = horizontal ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
        collectParams.weight = 0;
        collectParams.topMargin = -gap;
        recyclerParams.width = horizontal ? ViewGroup.LayoutParams.MATCH_PARENT : 0;
        recyclerParams.height = horizontal ? 0 : ViewGroup.LayoutParams.MATCH_PARENT;
        recyclerParams.weight = 1;
        recyclerParams.topMargin = horizontal ? 0 : -gap;
        mBinding.collect.setPadding(horizontal ? gap : ResUtil.dp2px(6), 0, horizontal ? gap : 0, horizontal ? 0 : gap);
        mBinding.recycler.setPadding(horizontal ? gap : 0, 0, horizontal ? gap : ResUtil.dp2px(6), gap);
        mBinding.collect.setLayoutParams(collectParams);
        mBinding.recycler.setLayoutParams(recyclerParams);

        setSearchColumn(getCount(), false);
    }

    private int getCollectWidth() {
        int width = 0;
        int space = ResUtil.dp2px(48);
        int minWidth = ResUtil.dp2px(128);
        int maxWidth = ResUtil.dp2px(160);
        for (Site site : mSites) width = Math.max(width, ResUtil.getTextWidth(site.getName(), 14));
        return Math.max(minWidth, Math.min(width + space, maxWidth));
    }

    private boolean isHorizontalUi() {
        return Setting.getSearchUi() == 0;
    }

    private int getCount() {
        int column = Setting.getSearchColumn();
        if (column > 0) return column;
        return ResUtil.isLand(requireActivity()) || ResUtil.isPad() ? SEARCH_DEFAULT_GRID_COUNT : 1;
    }

    private void setSearchColumn(int count, boolean persist) {
        int safeCount = Math.max(1, count);
        if (persist) Setting.putSearchColumn(safeCount > 1 ? 2 : 1);
        App.removeCallbacks(restoreSearchImages);
        pendingImageStart = RecyclerView.NO_POSITION;
        pendingImageEnd = RecyclerView.NO_POSITION;
        imageRestoreScheduled = false;
        resetLoadedSearchImages();
        mSearchAdapter.setLoadImages(safeCount <= 1);
        ((GridLayoutManager) (mBinding.recycler.getLayoutManager())).setSpanCount(safeCount);
        mSearchAdapter.setColumnCount(safeCount);
        postUpdateGridWidth();
        if (safeCount > 1) mBinding.recycler.post(() -> scheduleVisibleSearchImages(SEARCH_FIRST_IMAGE_DELAY));
    }

    private void setSearchView(boolean grid) {
        setSearchColumn(grid ? SEARCH_DEFAULT_GRID_COUNT : 1, true);
        mBinding.recycler.post(() -> mBinding.recycler.scrollToPosition(0));
        requireActivity().invalidateOptionsMenu();
    }

    private void postUpdateGridWidth() {
        mBinding.recycler.post(this::updateGridWidth);
    }

    private void updateGridWidth() {
        int width = mBinding.recycler.getWidth() - mBinding.recycler.getPaddingStart() - mBinding.recycler.getPaddingEnd();
        if (width > 0) mSearchAdapter.setGridWidth(width);
    }

    private void setCollect(Result result) {
        if (result == null || result.getList().isEmpty()) return;
        Collect collect = addMasterCollect(result.getList());
        if (!matchFilter(collect.getSite())) return;
        mCollectAdapter.add(result.getList());
        if (!hasCollect(mCollectAdapter.getItems(), collect) && !hasCollect(pendingCollectItems, collect)) pendingCollectItems.add(collect);
        if (mCollectAdapter.getPosition() == 0) pendingSearchItems.addAll(result.getList());
        scheduleSearchFlush();
    }

    private Collect addMasterCollect(List<Vod> items) {
        mAllCollectItems.get(0).getList().addAll(items);
        Site site = items.get(0).getSite();
        Collect collect = findCollect(mAllCollectItems, site.getKey());
        if (collect == null) {
            collect = new Collect(site, new ArrayList<>());
            mAllCollectItems.add(collect);
        }
        collect.getList().addAll(items);
        return collect;
    }

    private boolean hasCollect(List<Collect> items, Collect collect) {
        return findCollect(items, collect.getSite().getKey()) != null;
    }

    private Collect findCollect(List<Collect> items, String siteKey) {
        for (Collect item : items) if (item.getSite().getKey().equals(siteKey)) return item;
        return null;
    }

    private boolean matchFilter(Site site) {
        return TextUtils.isEmpty(mFilterGroup) || site.inGroup(mFilterGroup);
    }

    private void scheduleSearchFlush() {
        App.post(flushSearchUpdates, searchScrolling ? SEARCH_SCROLL_DELAY : SEARCH_UPDATE_DELAY);
    }

    private void flushSearchUpdates() {
        if (mBinding == null) return;
        if (searchScrolling && mSearchAdapter.getItemCount() > 0) {
            App.post(flushSearchUpdates, SEARCH_SCROLL_DELAY);
            return;
        }
        if (!pendingCollectItems.isEmpty()) {
            int count = Math.min(COLLECT_BATCH_SIZE, pendingCollectItems.size());
            List<Collect> items = new ArrayList<>(pendingCollectItems.subList(0, count));
            pendingCollectItems.subList(0, count).clear();
            mCollectAdapter.addAll(items);
        }
        if (!pendingSearchItems.isEmpty()) {
            int count = Math.min(SEARCH_BATCH_SIZE, pendingSearchItems.size());
            List<Vod> items = new ArrayList<>(pendingSearchItems.subList(0, count));
            pendingSearchItems.subList(0, count).clear();
            if (mCollectAdapter.getPosition() == 0) addSearchItems(items);
        }
        if (!pendingActiveSearchItems.isEmpty()) {
            Collect activated = mCollectAdapter.getActivated();
            boolean same = activated != null && activated.getSite().getKey().equals(pendingActiveSiteKey);
            if (same) {
                int count = Math.min(SEARCH_BATCH_SIZE, pendingActiveSearchItems.size());
                List<Vod> items = new ArrayList<>(pendingActiveSearchItems.subList(0, count));
                pendingActiveSearchItems.subList(0, count).clear();
                addSearchItems(items);
            } else {
                pendingActiveSearchItems.clear();
                pendingActiveSiteKey = "";
            }
        }
        if (!pendingCollectItems.isEmpty() || !pendingSearchItems.isEmpty() || !pendingActiveSearchItems.isEmpty()) scheduleSearchFlush();
    }

    private void setSearch(Result result) {
        if (result == null) return;
        mScroller.endLoading(result);
        boolean same = !result.getList().isEmpty() && mCollectAdapter.getActivated().getSite().equals(result.getVod().getSite());
        if (same) mCollectAdapter.getActivated().getList().addAll(result.getList());
        if (!same) return;
        if (searchScrolling && mSearchAdapter.getItemCount() > 0) {
            addPendingActiveSearchItems(result.getVod().getSite().getKey(), result.getList());
            return;
        }
        addSearchItems(result.getList());
    }

    private void addPendingActiveSearchItems(String siteKey, List<Vod> items) {
        if (!pendingActiveSiteKey.equals(siteKey)) {
            pendingActiveSearchItems.clear();
            pendingActiveSiteKey = siteKey;
        }
        pendingActiveSearchItems.addAll(items);
        scheduleSearchFlush();
    }

    @Override
    public void onItemClick(int position, Collect item) {
        flushSearchUpdates();
        setSearchItems(item.getList(), () -> mBinding.recycler.scrollToPosition(0));
        mCollectAdapter.setSelected(position);
        mScroller.setPage(item.getPage());
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.isFolder()) FolderActivity.start(requireActivity(), item.getSiteKey(), Result.folder(item));
        else VideoActivity.collect(requireActivity(), item.getSiteKey(), item.getId(), item.getName(), item.getPic());
    }

    @Override
    public boolean onLoadMore(String page) {
        Collect activated = mCollectAdapter.getActivated();
        if ("all".equals(activated.getSite().getKey())) return false;
        mViewModel.searchContent(activated.getSite(), getKeyword(), false, page);
        activated.setPage(Integer.parseInt(page));
        return true;
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_collect, menu);
    }

    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        MenuItem item = menu.findItem(R.id.action_view);
        if (item != null && mSearchAdapter != null) item.setIcon(mSearchAdapter.isGridMode() ? R.drawable.ic_action_list : R.drawable.ic_action_grid);
        MenuItem group = menu.findItem(R.id.action_group);
        if (group != null) {
            group.setVisible(canFilterGroup());
            group.setTitle(TextUtils.isEmpty(mFilterGroup) ? getString(R.string.search_scope_all) : mFilterGroup);
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        if (menuItem.getItemId() == R.id.action_view) {
            setSearchView(!mSearchAdapter.isGridMode());
            return true;
        }
        if (menuItem.getItemId() == R.id.action_group) {
            onGroupFilter();
            return true;
        }
        return true;
    }

    private boolean canFilterGroup() {
        return !isSiteSearch() && !isGroupSearch() && mGroups != null && !mGroups.isEmpty();
    }

    private void onGroupFilter() {
        if (!canFilterGroup()) return;
        View anchor = mBinding.toolbar.findViewById(R.id.action_group);
        PopupMenu popup = new PopupMenu(requireContext(), anchor == null ? mBinding.toolbar : anchor);
        popup.getMenu().add(0, MENU_GROUP_ALL, 0, R.string.search_scope_all);
        for (int i = 0; i < mGroups.size(); i++) popup.getMenu().add(1, MENU_GROUP_OFFSET + i, i + 1, mGroups.get(i));
        popup.setOnMenuItemClickListener(item -> onGroupFilterSelected(item.getItemId()));
        popup.show();
    }

    private boolean onGroupFilterSelected(int itemId) {
        if (itemId == MENU_GROUP_ALL) {
            setFilterGroup("");
        } else if (itemId >= MENU_GROUP_OFFSET) {
            int index = itemId - MENU_GROUP_OFFSET;
            if (index >= 0 && index < mGroups.size()) setFilterGroup(mGroups.get(index));
        }
        return true;
    }

    private void setFilterGroup(String group) {
        flushSearchUpdates();
        mFilterGroup = group == null ? "" : group;
        applyFilterGroup(getActiveSiteKey());
        requireActivity().invalidateOptionsMenu();
    }

    private String getActiveSiteKey() {
        if (mCollectAdapter == null || mCollectAdapter.getItemCount() == 0) return "all";
        return mCollectAdapter.getActivated().getSite().getKey();
    }

    private void applyFilterGroup(String activeSiteKey) {
        clearPendingSearchItems();
        List<Collect> items = getFilteredCollectItems(activeSiteKey);
        Collect activated = getSelectedCollect(items);
        mCollectAdapter.setItems(items, () -> {
            setSearchItems(activated.getList(), () -> mBinding.recycler.scrollToPosition(0));
            mScroller.setPage(activated.getPage());
        });
    }

    private List<Collect> getFilteredCollectItems(String activeSiteKey) {
        List<Collect> items = new ArrayList<>();
        Collect all = Collect.all();
        all.setSelected(false);
        items.add(all);
        for (int i = 1; i < mAllCollectItems.size(); i++) {
            Collect item = mAllCollectItems.get(i);
            if (!matchFilter(item.getSite())) continue;
            all.getList().addAll(item.getList());
            item.setSelected(item.getSite().getKey().equals(activeSiteKey));
            items.add(item);
        }
        if (getSelectedCollect(items) == all) all.setSelected(true);
        return items;
    }

    private Collect getSelectedCollect(List<Collect> items) {
        for (Collect item : items) if (item.isSelected()) return item;
        return items.get(0);
    }

    private void clearPendingSearchItems() {
        pendingCollectItems.clear();
        pendingSearchItems.clear();
        pendingActiveSearchItems.clear();
        pendingActiveSiteKey = "";
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) requireActivity().removeMenuProvider(this);
        else initMenu();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        App.removeCallbacks(flushSearchUpdates, restoreSearchImages);
        pendingCollectItems.clear();
        pendingSearchItems.clear();
        pendingActiveSearchItems.clear();
        mAllCollectItems.clear();
        pendingActiveSiteKey = "";
        pendingImageStart = RecyclerView.NO_POSITION;
        pendingImageEnd = RecyclerView.NO_POSITION;
        resetLoadedSearchImages();
        imageRestoreScheduled = false;
        searchScrolling = false;
        mViewModel.stopSearch();
        SiteHealthStore.flush();
        requireActivity().removeMenuProvider(this);
        mBinding = null;
    }
}
