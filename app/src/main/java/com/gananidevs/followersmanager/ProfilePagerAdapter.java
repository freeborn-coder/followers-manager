package com.gananidevs.followersmanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Objects;

public class ProfilePagerAdapter extends FragmentStatePagerAdapter {

    ArrayList<UserItem> userItems;

    public ProfilePagerAdapter(FragmentManager fm,ArrayList<UserItem> items) {
        super(fm);
        this.userItems = items;
    }

    @Override
    public Fragment getItem(int position) {
        return new ProfileFragment(userItems.get(position));
    }

    @Override
    public int getCount() {
        return userItems.size();
    }

    public static class ProfileFragment extends Fragment{
        UserItem userItem;
        public ProfileFragment(UserItem userItem) {
            this.userItem = userItem;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.pager_layout,container,false);
            bindViewToUserItem(view,this.userItem);
            return view;
        }

        private void bindViewToUserItem(View view, UserItem item) {
            ImageView iv = view.findViewById(R.id.profile_image);
            Glide.with(Objects.requireNonNull(getContext())).load(item.profileImageUrlHttps).into(iv);

            TextView nameTv,screenNameTv,followersCountTv,followingCountTv;
            nameTv = view.findViewById(R.id.name_tv);
            screenNameTv = view.findViewById(R.id.screen_name_tv);
            followersCountTv = view.findViewById(R.id.followers_count_tv);
            followingCountTv = view.findViewById(R.id.following_count_tv);

            nameTv.setText(userItem.name);
            screenNameTv.setText(userItem.screenName);
            followersCountTv.setText(Helper.insertCommas(userItem.followersCount));
            followingCountTv.setText(Helper.insertCommas(userItem.friendsCount));

            // Perform other tasks
        }

    }

}
