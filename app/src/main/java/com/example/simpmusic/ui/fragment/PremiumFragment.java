package com.example.simpmusic.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.simpmusic.R;
import com.google.android.material.button.MaterialButton;

public class PremiumFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_premium, container, false);

        // Loại bỏ MiniPlayer và Navbar trùng lặp trong layout cũ
        View duplicateNav = view.findViewById(R.id.bottomNavigation);
        if (duplicateNav != null) duplicateNav.setVisibility(View.GONE);
        View duplicatePlayer = view.findViewById(R.id.miniPlayerCard);
        if (duplicatePlayer != null) duplicatePlayer.setVisibility(View.GONE);

        MaterialButton btnBack = view.findViewById(R.id.btnPremiumBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().onBackPressed();
            });
        }

        return view;
    }
}
