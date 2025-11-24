package com.example.talkify.Fragment; // (Package của bạn)

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.talkify.R;
import com.example.talkify.auth.LoginActivity;
import com.example.talkify.services.SharedPrefManager;
import com.google.android.material.imageview.ShapeableImageView;

public class SettingsFragment extends Fragment {

    // Views
    private View rootView;
    private ShapeableImageView ivUserAvatar;
    private TextView tvFullName;
    private LinearLayout layoutProfile;
    private Button btnLogout;

    // Item Option Includes
    private View itemAccountPrivacy, itemNotifications, itemMessagesMedia,
            itemDevicesSessions, itemAppearance, itemSecurity,
            itemTwoFactorAuth, itemDataSync;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews();
        loadUserProfile();
        setupClickListeners();
    }

    private void initViews() {
        ivUserAvatar = rootView.findViewById(R.id.ivUserAvatar);
        tvFullName = rootView.findViewById(R.id.tvFullName);
        layoutProfile = rootView.findViewById(R.id.layoutProfile);
        btnLogout = rootView.findViewById(R.id.btnLogout);

        // Ánh xạ các layout <include>
        itemAccountPrivacy = rootView.findViewById(R.id.itemAccountPrivacy);
        itemNotifications = rootView.findViewById(R.id.itemNotifications);
        itemMessagesMedia = rootView.findViewById(R.id.itemMessagesMedia);
        itemDevicesSessions = rootView.findViewById(R.id.itemDevicesSessions);
        itemAppearance = rootView.findViewById(R.id.itemAppearance);
        itemSecurity = rootView.findViewById(R.id.itemSecurity);
        itemTwoFactorAuth = rootView.findViewById(R.id.itemTwoFactorAuth);
        itemDataSync = rootView.findViewById(R.id.itemDataSync);

        // GỌI HÀM HELPER: Gán Icon và Text cho các <include>
        // (Thay R.drawable... bằng tên icon chính xác của bạn)
        setSettingOption(itemAccountPrivacy, R.drawable.ic_user, getString(R.string.settings_account_privacy));
        setSettingOption(itemNotifications, R.drawable.ic_bell, getString(R.string.settings_notifications));
        setSettingOption(itemMessagesMedia, R.drawable.ic_chat, getString(R.string.settings_messages_media));
        setSettingOption(itemDevicesSessions, R.drawable.ic_device, getString(R.string.settings_devices_sessions));
        setSettingOption(itemAppearance, R.drawable.ic_theme, getString(R.string.settings_appearance));
        setSettingOption(itemSecurity, R.drawable.ic_shield, getString(R.string.settings_security));
        setSettingOption(itemTwoFactorAuth, R.drawable.ic_user_sheild, getString(R.string.settings_two_factor_auth));
        setSettingOption(itemDataSync, R.drawable.ic_sync, getString(R.string.settings_data_sync));
    }

    /**
     * Hàm helper để gán icon và text cho mỗi item_setting_option
     */
    private void setSettingOption(View layout, int iconResId, String text) {
        ImageView icon = layout.findViewById(R.id.ivOptionIcon);
        TextView textView = layout.findViewById(R.id.tvOptionText);

        if (icon != null) {
            icon.setImageResource(iconResId);
        }
        if (textView != null) {
            textView.setText(text);
        }
    }

    private void loadUserProfile() {
        if (getContext() == null) return;

        String fullName = SharedPrefManager.getInstance(getContext()).getUserFullName();
        String avatarUrl = SharedPrefManager.getInstance(getContext()).getUserAvatarUrl();

        if (fullName != null && !fullName.isEmpty()) {
            tvFullName.setText(fullName);
        }

        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.avatar_placeholder)
                .error(R.drawable.avatar_placeholder)
                .into(ivUserAvatar);
    }

    private void setupClickListeners() {
        layoutProfile.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Chuyển đến trang cá nhân", Toast.LENGTH_SHORT).show();
            // TODO: Intent to ProfileActivity
        });

        // (Các listener khác)
        itemAccountPrivacy.setOnClickListener(v -> Toast.makeText(getContext(), getString(R.string.settings_account_privacy), Toast.LENGTH_SHORT).show());
        itemNotifications.setOnClickListener(v -> Toast.makeText(getContext(), getString(R.string.settings_notifications), Toast.LENGTH_SHORT).show());
        // ... (thêm các listener khác) ...

        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void showLogoutConfirmationDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.settings_logout)
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton(R.string.settings_logout, (dialog, which) -> performLogout())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performLogout() {
        if (getActivity() == null) return;

        SharedPrefManager.getInstance(getContext()).clearUserSession();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        getActivity().finish();
        Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();
    }
}