package com.example.simpmusic.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.data.model.Song;
import com.example.simpmusic.ui.activity.MainActivity;
import com.example.simpmusic.ui.activity.PlayerActivity;
import com.example.simpmusic.ui.adapter.SongAdapter;
import com.example.simpmusic.ui.dialog.PlaylistSelectionBottomSheet;
import com.example.simpmusic.ui.dialog.SongOptionsBottomSheet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment implements SongAdapter.OnSongClickListener, SongOptionsBottomSheet.OnOptionClickListener, PlaylistSelectionBottomSheet.OnPlaylistSelectedListener {

    private SearchView searchView;
    private RecyclerView rvSearchResults, rvTrending;
    private SongAdapter searchAdapter, trendingAdapter;
    private LinearLayout llNoResults, llInitialView;
    private ProgressBar pbLoading;

    private static final String PREF_NAME = "search_prefs";
    private static final String KEY_HISTORY = "search_history";
    private static final int MAX_HISTORY_SIZE = 10;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Chỉ inflate layout giao diện tìm kiếm
        View view = inflater.inflate(R.layout.activity_search, container, false);

        // Ẩn Navbar và MiniPlayer thừa trong layout cũ của Activity nếu có
        View duplicateNav = view.findViewById(R.id.bottomNavigation);
        if (duplicateNav != null) duplicateNav.setVisibility(View.GONE);
        View duplicatePlayer = view.findViewById(R.id.miniPlayerCard);
        if (duplicatePlayer != null) duplicatePlayer.setVisibility(View.GONE);

        initViews(view);
        setupSearch();
        fetchTrendingSongs();
        return view;
    }

    private void initViews(View view) {
        searchView = view.findViewById(R.id.searchView);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        rvTrending = view.findViewById(R.id.rvTrending);
        llInitialView = view.findViewById(R.id.llInitialView);
        llNoResults = view.findViewById(R.id.llNoResults);
        pbLoading = view.findViewById(R.id.pbLoading);

        if (rvSearchResults != null) {
            rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
            searchAdapter = new SongAdapter(new ArrayList<>(), this);
            rvSearchResults.setAdapter(searchAdapter);
        }

        if (rvTrending != null) {
            rvTrending.setLayoutManager(new LinearLayoutManager(getContext()));
            trendingAdapter = new SongAdapter(new ArrayList<>(), this);
            rvTrending.setAdapter(trendingAdapter);
        }
    }

    private void setupSearch() {
        if (searchView == null) return;
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!query.trim().isEmpty()) performSearch(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().isEmpty()) showInitialState();
                else hideInitialState();
                return false;
            }
        });
    }

    private void showInitialState() {
        if (llInitialView != null) llInitialView.setVisibility(View.VISIBLE);
        if (rvSearchResults != null) rvSearchResults.setVisibility(View.GONE);
        if (llNoResults != null) llNoResults.setVisibility(View.GONE);
        if (pbLoading != null) pbLoading.setVisibility(View.GONE);
    }

    private void hideInitialState() {
        if (llInitialView != null) llInitialView.setVisibility(View.GONE);
        if (rvSearchResults != null) rvSearchResults.setVisibility(View.VISIBLE);
    }

    private void fetchTrendingSongs() {
        RetrofitClient.getApiService().getAllSongs().enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null && trendingAdapter != null) {
                    List<Song> songs = new ArrayList<>(response.body());
                    Collections.sort(songs, (s1, s2) -> Long.compare(s2.getViewCount(), s1.getViewCount()));
                    List<Song> topSongs = songs.subList(0, Math.min(5, songs.size()));
                    trendingAdapter.updateSongs(topSongs);
                }
            }
            @Override public void onFailure(Call<List<Song>> call, Throwable t) {}
        });
    }

    private void performSearch(String keyword) {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        if (llNoResults != null) llNoResults.setVisibility(View.GONE);
        if (rvSearchResults != null) rvSearchResults.setVisibility(View.GONE);

        RetrofitClient.getApiService().searchSongs(keyword).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                if (rvSearchResults != null) rvSearchResults.setVisibility(View.VISIBLE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Song> songs = response.body();
                    if (searchAdapter != null) searchAdapter.updateSongs(songs);
                    if (llNoResults != null) llNoResults.setVisibility(songs.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void onFailure(Call<List<Song>> call, Throwable t) {
                if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                if (rvSearchResults != null) rvSearchResults.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onSongClick(Song song) {
        saveToHistory(song);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onSongClick(song);
        }
    }

    @Override public void onArtistClick(Song song) {}

    private void saveToHistory(Song song) {
        if (getContext() == null) return;
        SharedPreferences prefs = get_context_safe().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(KEY_HISTORY, null);
        List<Song> historyList = json == null ? new ArrayList<>() : gson.fromJson(json, new TypeToken<List<Song>>(){}.getType());
        historyList.removeIf(s -> s.getId() == song.getId());
        historyList.add(0, song);
        if (historyList.size() > MAX_HISTORY_SIZE) historyList = historyList.subList(0, MAX_HISTORY_SIZE);
        prefs.edit().putString(KEY_HISTORY, gson.toJson(historyList)).apply();
    }

    private Context get_context_safe() {
        return getContext() != null ? getContext() : getActivity();
    }

    @Override public void onMoreClick(Song song) { if (getActivity() != null) new SongOptionsBottomSheet(song).show(getChildFragmentManager(), "SongOptions"); }
    @Override public void onAddToPlaylist(Song song) { if (getActivity() != null) new PlaylistSelectionBottomSheet(song).show(getChildFragmentManager(), "PlaylistSelection"); }
    @Override public void onViewAlbum(Song song) {}
    @Override public void onShare(Song song) {}
    @Override public void onPlaylistSelected(Playlist playlist, Song song) {}
    @Override public void onCreateNewPlaylist(Song song) {}
}
