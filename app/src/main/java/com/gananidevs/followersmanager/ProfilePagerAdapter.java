package com.gananidevs.followersmanager;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

import okhttp3.ResponseBody;

import static com.gananidevs.followersmanager.Helper.A_USERS_FOLLOWERS;
import static com.gananidevs.followersmanager.Helper.A_USERS_FOLLOWING;
import static com.gananidevs.followersmanager.Helper.LIST_NAME;
import static com.gananidevs.followersmanager.Helper.SCREEN_NAME_OF_USER;
import static com.gananidevs.followersmanager.Helper.USER_ID;
import static com.gananidevs.followersmanager.Helper.changeButtonTextAndColor;
import static com.gananidevs.followersmanager.Helper.incrementApiRequestCount;
import static com.gananidevs.followersmanager.Helper.insertCommas;
import static com.gananidevs.followersmanager.Helper.proceedWithApiCall;
import static com.gananidevs.followersmanager.UsersRecyclerAdapter.hideProgressShowButtonText;
import static com.gananidevs.followersmanager.UsersRecyclerAdapter.showProgressHideButtonText;

public class ProfilePagerAdapter extends FragmentStatePagerAdapter {

    ArrayList<UserItem> userItems;
    static TwitterSession twitterSession;
    static MyTwitterApiClient twitterApiClient;

    public ProfilePagerAdapter(FragmentManager fm,ArrayList<UserItem> items) {
        super(fm,FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        twitterSession = TwitterCore.getInstance().getSessionManager().getActiveSession();
        twitterApiClient = new MyTwitterApiClient(twitterSession);
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
            bindViewToUserItem(view);
            return view;
        }



        private void bindViewToUserItem(View view) {

            Dialog dialog = new BottomSheetDialog(getContext());

            ImageView popupIcon = view.findViewById(R.id.popup_icon);
            ImageView profileImage = view.findViewById(R.id.profile_image);
            ImageView verifiedIcon = view.findViewById(R.id.verified_icon);
            TextView nameTv = view.findViewById(R.id.name_tv);
            TextView screenNameTv = view.findViewById(R.id.screen_name_tv);
            TextView followersCountTv = view.findViewById(R.id.followers_count_tv);
            TextView followingCountTv = view.findViewById(R.id.following_count_tv);
            TextView whitelistStatusTv = view.findViewById(R.id.whitelisted_status_tv);
            final MaterialButton followUnfollowBtn = view.findViewById(R.id.follow_unfollow_button);
            MaterialButton followStatusBtn = view.findViewById(R.id.follow_status_button);
            TextView followersTv = view.findViewById(R.id.followers_tv);
            TextView followingTv = view.findViewById(R.id.following_tv);
            TextView descriptionEt = view.findViewById(R.id.description_et);
            EditText locationEt = view.findViewById(R.id.location_et);
            EditText dateCreatedEt = view.findViewById(R.id.date_et);
            EditText userLinkEt = view.findViewById(R.id.link_et);
            TextView twitterLinkTv = view.findViewById(R.id.twitter_link_tv);
            ImageView linkIcon = view.findViewById(R.id.link_icon);
            final ProgressBar btnProgressBar = view.findViewById(R.id.btn_progress_bar);
            ConstraintLayout constraintLayout = view.findViewById(R.id.constraint_layout);
            ProgressBar progressBar = view.findViewById(R.id.progress_bar);

            /*
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

             */
            verifiedIcon = view.findViewById(R.id.verified_icon);

            // Check if a user is verified
            if(userItem.isVerified){
                verifiedIcon.setVisibility(View.VISIBLE);
            }else{
                verifiedIcon.setVisibility(View.GONE);
            }


            if(twitterSession.getUserId() == userItem.id) {
                followStatusBtn.setVisibility(View.GONE);
                followUnfollowBtn.setVisibility(View.GONE);
            }else {
                followStatusBtn.setVisibility(View.VISIBLE);
                followUnfollowBtn.setVisibility(View.VISIBLE);

                // check if the user is a follower for follow status button
                if (Helper.isFollower(userItem.id)) {
                    changeButtonTextAndColor(getContext(), followStatusBtn, R.string.follows_you, R.color.green);
                } else {
                    changeButtonTextAndColor(getContext(), followStatusBtn, R.string.doesnt_follow_you, R.color.mid_gray_777);
                }

            }

            // check if you are following the user or not for follow or unfollow button
            if(Helper.isFriend(userItem.id)){
                changeButtonTextAndColor(getContext(),followUnfollowBtn,R.string.unfollow_all_lowercase,R.color.colorAccent);
            }else{
                changeButtonTextAndColor(getContext(),followUnfollowBtn,R.string.follow_all_lowercase,R.color.follow_btn_back_color);
            }

            try{
                Glide.with(this).load(userItem.profileImageUrlHttps).into(profileImage);
            }catch (Exception e){
                e.printStackTrace();
            }

            nameTv.setText(userItem.name);
            String screenName = "@"+ userItem.screenName;
            screenNameTv.setText(screenName);

            followersCountTv.setText(insertCommas(userItem.followersCount));
            followingCountTv.setText(insertCommas(userItem.friendsCount));

            TextClickListener textClickListener = new TextClickListener();
            followingCountTv.setOnClickListener(textClickListener);
            followersCountTv.setOnClickListener(textClickListener);
            followingTv.setOnClickListener(textClickListener);
            followersTv.setOnClickListener(textClickListener);

            descriptionEt.setText(userItem.description);
            locationEt.setText(userItem.location);
            userLinkEt.setText(userItem.url);
            dateCreatedEt.setText(userItem.createdAt);

            twitterLinkTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/"+ userItem.screenName));
                        startActivity(intent);
                    }catch (Exception e){
                        e.printStackTrace();
                        if( BuildConfig.DEBUG){
                            Toast.makeText(getContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    }
                }

            });

            if(Helper.isWhielisted(userItem.id)){
                whitelistStatusTv.setVisibility(View.VISIBLE);
            }else{
                whitelistStatusTv.setVisibility(View.GONE);
            }

            linkIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if(!userItem.url.isEmpty()) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(userItem.url));
                            startActivity(intent);
                        }
                    }catch (Exception e){e.printStackTrace();
                        Toast.makeText(getContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                }
            });

            popupIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(getContext(),v);
                    Menu menu = popupMenu.getMenu();
                    menu.add(getString(R.string.view_followers));
                    menu.add(getString(R.string.view_following));
                    menu.add(getString(R.string.just_view_on_twitter));

                    // Add menu options
                    if(userItem.id != twitterSession.getUserId()) {
                        if (!Helper.isFollower(userItem.id)) {

                            if(Helper.isFriend(userItem.id)) {
                                menu.add(getString(R.string.request_follow_back));

                                if (Helper.isWhielisted(userItem.id)) {
                                    menu.add(getString(R.string.remove_from_whitelist));
                                } else {
                                    menu.add(getString(R.string.add_to_whitelist));
                                }
                            }

                        }
                    }

                    // Add menu actions
                    popupMenu.setOnMenuItemClickListener(new UserProfileActivity.MyPopMenuItemClickListener());
                    popupMenu.show();

                }
            });

            // what follow/unfollow button click handler
            followUnfollowBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Long userId = userItem.id;
                    final String userScreenName = userItem.screenName;

                    // follow or unfollow users as neccessary
                    String btnText = followUnfollowBtn.getText().toString();

                    long interval = System.currentTimeMillis() - MainActivity.last15MinTimeStamp;
                    if(proceedWithApiCall(interval)) {

                        if (btnText.equals(getString(R.string.follow_all_lowercase))) {
                            // follow the specific user
                            showProgressHideButtonText(btnProgressBar, followUnfollowBtn, getContext());

                            int delay = new Random().nextInt(1000) + 1000;
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    followUser(userId);
                                }
                            }, delay);

                        } else if (btnText.equals(getString(R.string.unfollow_all_lowercase))) {

                            // unfollow the specified user
                            final Dialog confirmDialog = new Dialog(getContext());
                            View dialogView = getLayoutInflater().inflate(R.layout.confirm_dialog_layout,null,false);
                            TextView confirmMessage = dialogView.findViewById(R.id.message_tv);
                            confirmMessage.setText("unfollow "+userScreenName+"?");
                            Button positiveBtn = dialogView.findViewById(R.id.positive_btn);
                            positiveBtn.setText(R.string.yes);
                            positiveBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    confirmDialog.dismiss();
                                    showProgressHideButtonText(btnProgressBar, followUnfollowBtn, getContext());
                                    int delay = new Random().nextInt(1000) + 1500;
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            unfollowUser(userId);
                                        }
                                    }, delay);
                                }
                            });

                            Button negativeBtn = dialogView.findViewById(R.id.negative_btn);
                            negativeBtn.setText(R.string.cancel);
                            negativeBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    confirmDialog.dismiss();
                                }
                            });
                            confirmDialog.setContentView(dialogView);
                            confirmDialog.show();

                        }

                    }else{ // Request count is >= 15 within 15 minutes, so do not proceed with request
                        if(btnText.equals(getString(R.string.unfollow_all_lowercase))){
                            // show alert dialog then waste time for nothing :)
                            showDialogAndFeignWork(userScreenName);
                        }else{
                            placeboAction();
                        }
                    }


                }
            });

            // Profile image click handler
            profileImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Dialog photoDialog = new Dialog(getContext(),android.R.style.Theme_DeviceDefault_NoActionBar);
                    View dialogView = getLayoutInflater().inflate(R.layout.user_photo_dialog,null,false);
                    isImageLoaded = false;

                    final ImageView dialogImage = dialogView.findViewById(R.id.user_profile_image);

                    Glide.with(getContext()).load(userItem.profileImageUrlHttps.replace("_normal","")).listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            isImageLoaded = true;
                            return false;
                        }
                    }).into(dialogImage);

                    ImageView downloadIcon = dialogView.findViewById(R.id.download_icon);
                    ImageView closeIcon = dialogView.findViewById(R.id.close_icon);

                    downloadIcon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            if(isImageLoaded) {
                                try {
                                    dialogImage.setDrawingCacheEnabled(true);
                                    bitmap = dialogImage.getDrawingCache();
                                    imageTitle = System.currentTimeMillis() + userItem.screenName;
                                    imageDescription = userItem.screenName + " profile photo from followers manager app";

                                    if(Build.VERSION.SDK_INT >= 23){
                                        if(checkWritePermission()){
                                            saveImage(bitmap, imageTitle, imageDescription);
                                        }
                                    }else{
                                        saveImage(bitmap, imageTitle, imageDescription);
                                    }

                                }catch(Exception e){
                                    Toast.makeText(getContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });

                    closeIcon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            photoDialog.dismiss();
                        }
                    });

                    photoDialog.setContentView(dialogView);
                    photoDialog.show();

                }
            });


        }


        class TextClickListener implements View.OnClickListener{

            @Override
            public void onClick(View v) {
                int id = v.getId();

                if(id == R.id.followers_count_tv || id == R.id.followers_tv){
                    // view user's followers
                    viewUserFollowers();

                }else if(id == R.id.following_count_tv || id == R.id.following_tv){
                    //view user's following
                    viewUserFollowing();
                }

            }
        }

        private void viewUserFollowing() {
            Intent intent = new Intent(getContext(),UsersListActivity.class);
            intent.putExtra(LIST_NAME,A_USERS_FOLLOWING);
            intent.putExtra(USER_ID, userItem.id);
            intent.putExtra(SCREEN_NAME_OF_USER, userItem.screenName);
            startActivity(intent);
        }

        private void viewUserFollowers() {
            Intent intent = new Intent(getContext(),UsersListActivity.class);
            intent.putExtra(LIST_NAME,A_USERS_FOLLOWERS);
            intent.putExtra(USER_ID, userItem.id);
            intent.putExtra(SCREEN_NAME_OF_USER, userItem.screenName);
            startActivity(intent);
        }

        private void followUser(final Long userId) {

            twitterApiClient.getFriendshipsCreateCustomService().post(userId).enqueue(new Callback<ResponseBody>() {
                @Override
                public void success(Result<ResponseBody> result) {
                    hideProgressShowButtonText(btnProgressBar,followUnfollowBtn,getContext());
                    followUnfollowBtn.setText(getString(R.string.unfollow_all_lowercase));
                    changeButtonTextAndColor(getContext(),followUnfollowBtn,R.string.unfollow_all_lowercase,R.color.colorAccent);
                    MainActivity.friendsIdsList.add(userId);
                    incrementApiRequestCount(getContext());
                }

                @Override
                public void failure(TwitterException exception) {
                    hideProgressShowButtonText(btnProgressBar,followUnfollowBtn,getContext());
                    Toast.makeText(getContext(),exception.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });

        }

        private void unfollowUser(final Long userId){

            twitterApiClient.getFriendshipsDestroyCustomService().post(userId).enqueue(new Callback<ResponseBody>() {
                @Override
                public void success(Result<ResponseBody> result) {
                    hideProgressShowButtonText(btnProgressBar,followUnfollowBtn,getContext());
                    followUnfollowBtn.setText(getString(R.string.unfollow_all_lowercase));
                    changeButtonTextAndColor(getContext(),followUnfollowBtn,R.string.follow_all_lowercase,R.color.follow_btn_back_color);
                    MainActivity.friendsIdsList.remove(userId);
                    incrementApiRequestCount(getContext());
                }

                @Override
                public void failure(TwitterException exception) {
                    hideProgressShowButtonText(btnProgressBar,followUnfollowBtn,getContext());
                    Toast.makeText(getContext(),exception.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });
        }




    }


}
