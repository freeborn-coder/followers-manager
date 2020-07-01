package com.gananidevs.followersmanager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.User;

import java.util.Random;

import okhttp3.ResponseBody;

import static com.gananidevs.followersmanager.Helper.USER_PARCELABLE;
import static com.gananidevs.followersmanager.Helper.changeButtonTextAndColor;
import static com.gananidevs.followersmanager.Helper.insertCommas;
import static com.gananidevs.followersmanager.UsersRecyclerAdapter.hideProgressShowButtonText;
import static com.gananidevs.followersmanager.UsersRecyclerAdapter.showProgressHideButtonText;

public class UserProfileActivity extends AppCompatActivity {

    private AdView mAdView;
    boolean isAdLoaded = false;
    private ImageView profileImage;
    private ImageView verifiedIcon;
    private TextView nameTv;
    private TextView screenNameTv;
    private TextView followersCountTv;
    private TextView followingCountTv;
    private MaterialButton followUnfollowBtn;
    private MaterialButton followStatusBtn;
    private TextView followersTv;
    private TextView followingTv;
    private TextInputEditText descriptionEt;
    private TextInputEditText locationEt;
    private TextInputEditText dateCreatedEt;
    private TextInputEditText userLinkEt;
    private TextView twitterLinkTv;
    private ImageView linkIcon;
    private ProgressBar btnProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        initializeViews();
        loadDataIntoViews();
        loadAds();

    }

    private void loadDataIntoViews() {
        final UserItem user = getIntent().getParcelableExtra(USER_PARCELABLE);

        // Check if a user is verified
        if(user.isVerified){
            verifiedIcon.setVisibility(View.VISIBLE);
        }else{
            verifiedIcon.setVisibility(View.GONE);
        }

        TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();

        if(session.getUserId() == user.id) {
            followStatusBtn.setVisibility(View.GONE);
            followUnfollowBtn.setVisibility(View.GONE);
        }else {

            // check if the user is a follower for follow status button
            if (Helper.isFollower(user.id)) {
                changeButtonTextAndColor(this, followStatusBtn, R.string.follows_you, R.color.green);
            } else {
                changeButtonTextAndColor(this, followStatusBtn, R.string.doesnt_follow_you, R.color.mid_gray_777);
            }

        }

        // check if you are following the user or not for follow or unfollow button
        if(Helper.isFriend(user.id)){
            changeButtonTextAndColor(this,followUnfollowBtn,R.string.unfollow_all_lowercase,R.color.colorAccent);
        }else{
            changeButtonTextAndColor(this,followUnfollowBtn,R.string.follow_all_lowercase,R.color.follow_btn_back_color);
        }

        try{
            Glide.with(this).load(user.profileImageUrlHttps).into(profileImage);
        }catch (Exception e){
            e.printStackTrace();
        }

        nameTv.setText(user.name);
        String screenName = "@"+user.screenName;
        screenNameTv.setText(screenName);

        followersCountTv.setText(insertCommas(user.followersCount));
        followingCountTv.setText(insertCommas(user.friendsCount));

        descriptionEt.setText(user.description);
        locationEt.setText(user.location);
        userLinkEt.setText(user.url);
        dateCreatedEt.setText(user.createdAt);

        twitterLinkTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/"+user.screenName));
                    startActivity(intent);
                }catch (Exception e){
                    e.printStackTrace();
                    if( BuildConfig.DEBUG){
                        Toast.makeText(UserProfileActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                }
            }

        });

        linkIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(!user.url.isEmpty()) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(user.url));
                        startActivity(intent);
                    }
                }catch (Exception e){e.printStackTrace();
                    Toast.makeText(UserProfileActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        followUnfollowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Long userId = user.id;
                final String userScreenName = user.screenName;

                // follow or unfollow users as neccessary
                String btnText = followUnfollowBtn.getText().toString();

                if(btnText.equals(getString(R.string.follow_all_lowercase))){
                    // follow the specific user
                    showProgressHideButtonText(btnProgressBar,followUnfollowBtn,UserProfileActivity.this);

                    int delay = new Random().nextInt(1000)+1000;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            followUser(userId);
                        }
                    },delay);

                }else if(btnText.equals(getString(R.string.unfollow_all_lowercase))){

                    // unfollow the specified user
                    new AlertDialog.Builder(UserProfileActivity.this).setTitle("confirm action")
                            .setMessage(getString(R.string.unfollow_all_lowercase)+" "+userScreenName+"?")
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    showProgressHideButtonText(btnProgressBar,followUnfollowBtn, UserProfileActivity.this);

                                    int delay = new Random().nextInt(1000)+1000;
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            unfollowUser(userId);
                                        }
                                    },delay);

                                }
                            }).setNegativeButton(getString(R.string.cancel),null)
                            .show();

                }
            }
        });

    }

    private void followUser(final Long userId) {
        MyTwitterApiClient twitterApiClient = new MyTwitterApiClient(TwitterCore.getInstance().getSessionManager().getActiveSession());

        twitterApiClient.getFriendshipsCreateCustomService().post(userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void success(Result<ResponseBody> result) {
                hideProgressShowButtonText(btnProgressBar,followUnfollowBtn,UserProfileActivity.this);
                followUnfollowBtn.setText(getString(R.string.unfollow_all_lowercase));
                changeButtonTextAndColor(UserProfileActivity.this,followUnfollowBtn,R.string.unfollow_all_lowercase,R.color.colorAccent);
                MainActivity.friendsIdsList.add(userId);
            }

            @Override
            public void failure(TwitterException exception) {
                hideProgressShowButtonText(btnProgressBar,followUnfollowBtn,UserProfileActivity.this);
                Toast.makeText(UserProfileActivity.this,exception.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void unfollowUser(final Long userId){
        MyTwitterApiClient twitterApiClient = new MyTwitterApiClient(TwitterCore.getInstance().getSessionManager().getActiveSession());

        twitterApiClient.getFriendshipsDestroyCustomService().post(userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void success(Result<ResponseBody> result) {
                hideProgressShowButtonText(btnProgressBar,followUnfollowBtn,UserProfileActivity.this);
                followUnfollowBtn.setText(getString(R.string.unfollow_all_lowercase));
                changeButtonTextAndColor(UserProfileActivity.this,followUnfollowBtn,R.string.follow_all_lowercase,R.color.follow_btn_back_color);
                MainActivity.friendsIdsList.remove(userId);
            }

            @Override
            public void failure(TwitterException exception) {
                hideProgressShowButtonText(btnProgressBar,followUnfollowBtn,UserProfileActivity.this);
                Toast.makeText(UserProfileActivity.this,exception.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.profile_image);
        verifiedIcon = findViewById(R.id.verified_icon);
        nameTv = findViewById(R.id.name_tv);
        screenNameTv = findViewById(R.id.screen_name_tv);
        followersCountTv = findViewById(R.id.followers_count_tv);
        followingCountTv = findViewById(R.id.following_count_tv);
        followUnfollowBtn = findViewById(R.id.follow_unfollow_button);
        followStatusBtn = findViewById(R.id.follow_status_button);
        followersTv = findViewById(R.id.followers_tv);
        followingTv = findViewById(R.id.following_tv);
        descriptionEt = findViewById(R.id.description_et);
        locationEt = findViewById(R.id.location_et);
        dateCreatedEt = findViewById(R.id.date_et);
        userLinkEt = findViewById(R.id.link_et);
        twitterLinkTv = findViewById(R.id.twitter_link_tv);
        linkIcon = findViewById(R.id.link_icon);
        btnProgressBar = findViewById(R.id.btn_progress_bar);
    }

    private void loadAds() {
        mAdView = findViewById(R.id.banner_adview);

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                mAdView.loadAd(new AdRequest.Builder().build());
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                mAdView.loadAd(new AdRequest.Builder().build());
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                isAdLoaded = true;
            }

        });

        mAdView.loadAd(new AdRequest.Builder().build());


    }
}
