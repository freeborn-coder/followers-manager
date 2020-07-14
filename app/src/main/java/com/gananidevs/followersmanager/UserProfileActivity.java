package com.gananidevs.followersmanager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.User;

import java.util.ArrayList;
import java.util.Random;

import okhttp3.ResponseBody;

import static com.gananidevs.followersmanager.Helper.A_USERS_FOLLOWERS;
import static com.gananidevs.followersmanager.Helper.A_USERS_FOLLOWING;
import static com.gananidevs.followersmanager.Helper.CURRENT_USER_INDEX;
import static com.gananidevs.followersmanager.Helper.DATABASE_URL;
import static com.gananidevs.followersmanager.Helper.LIST_NAME;
import static com.gananidevs.followersmanager.Helper.SCREEN_NAME_OF_USER;
import static com.gananidevs.followersmanager.Helper.USERS_PARCELABLE_ARRAYLIST;
import static com.gananidevs.followersmanager.Helper.USER_ID;
import static com.gananidevs.followersmanager.Helper.changeButtonTextAndColor;
import static com.gananidevs.followersmanager.Helper.checkWhetherToAskUserToRateApp;
import static com.gananidevs.followersmanager.Helper.getMinutesLeft;
import static com.gananidevs.followersmanager.Helper.incrementApiRequestCount;
import static com.gananidevs.followersmanager.Helper.insertCommas;
import static com.gananidevs.followersmanager.Helper.isNetworkConnected;
import static com.gananidevs.followersmanager.Helper.proceedWithApiCall;
import static com.gananidevs.followersmanager.Helper.showRequestFollowBackBottomSheetDialog;
import static com.gananidevs.followersmanager.Helper.showSnackBar;
import static com.gananidevs.followersmanager.UsersRecyclerAdapter.hideProgressShowButtonText;
import static com.gananidevs.followersmanager.UsersRecyclerAdapter.showProgressHideButtonText;

public class UserProfileActivity extends AppCompatActivity {

    private static final int WRITE_STORAGE_REQUEST = 111;
    private static final long ANIM_START_DELAY = 100;

    boolean isAdLoaded = false;
    private ImageView profileImage, verifiedIcon, linkIcon, popupIcon;
    private TextView nameTv, screenNameTv,followersCountTv,followingCountTv, followersTv, followingTv, twitterLinkTv, whitelistStatusTv;
    private MaterialButton followUnfollowBtn, followStatusBtn;
    private TextInputEditText descriptionEt, locationEt, dateCreatedEt, userLinkEt;
    private ProgressBar btnProgressBar;
    private TwitterSession twitterSession;
    private ArrayList<UserItem> userItemArrayList;
    private boolean isImageLoaded = false;
    private Bitmap bitmap;
    private String imageTitle, imageDescription;
    private ConstraintLayout constraintLayout;
    UserItem currentItem;
    private int currentUserIndex;
    private UnifiedNativeAdView nativeAdView;
    private InterstitialAd mInterstitialAd;
    private BottomSheetDialog dialog;
    private MyTwitterApiClient twitterApiClient;
    private ProgressBar progressBar;
    private DatabaseReference databaseReference;
    private AdLoader adLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setActionBarTitle(getString(R.string.profile));

        if(MainActivity.isShowingAds)
            loadAds();


        initializeViews();
        twitterSession = TwitterCore.getInstance().getSessionManager().getActiveSession();
        twitterApiClient = new MyTwitterApiClient(twitterSession);

        userItemArrayList = getIntent().getParcelableArrayListExtra(USERS_PARCELABLE_ARRAYLIST);
        currentUserIndex = getIntent().getIntExtra(CURRENT_USER_INDEX,0);
        currentItem = userItemArrayList.get(currentUserIndex);
        loadDataIntoViews(currentItem);

    }

    private void setActionBarTitle(String actionBarTitle) {

        try{
            getSupportActionBar().setTitle(actionBarTitle);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void loadDataIntoViews(final UserItem userItem) {

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

            // check if the user is a follower for follow status button
            if (Helper.isFollower(userItem.id)) {
                changeButtonTextAndColor(this, followStatusBtn, R.string.follows_you, R.color.green);
            } else {
                changeButtonTextAndColor(this, followStatusBtn, R.string.doesnt_follow_you, R.color.mid_gray_777);
            }

        }

        // check if you are following the user or not for follow or unfollow button
        if(Helper.isFriend(userItem.id)){
            changeButtonTextAndColor(this,followUnfollowBtn,R.string.unfollow_all_lowercase,R.color.colorAccent);
        }else{
            changeButtonTextAndColor(this,followUnfollowBtn,R.string.follow_all_lowercase,R.color.follow_btn_back_color);
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
                        Toast.makeText(UserProfileActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
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
                Toast.makeText(UserProfileActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
            }
            }
        });

        popupIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(UserProfileActivity.this,v);
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
                popupMenu.setOnMenuItemClickListener(new MyPopMenuItemClickListener());
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
                        showProgressHideButtonText(btnProgressBar, followUnfollowBtn, UserProfileActivity.this);

                        int delay = new Random().nextInt(1000) + 1000;
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                followUser(userId);
                            }
                        }, delay);

                    } else if (btnText.equals(getString(R.string.unfollow_all_lowercase))) {

                        // unfollow the specified user
                        new AlertDialog.Builder(UserProfileActivity.this).setTitle("confirm action")
                                .setMessage(getString(R.string.unfollow_all_lowercase) + " " + userScreenName + "?")
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        showProgressHideButtonText(btnProgressBar, followUnfollowBtn, UserProfileActivity.this);

                                        int delay = new Random().nextInt(1000) + 1000;
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                unfollowUser(userId);
                                            }
                                        }, delay);

                                    }
                                }).setNegativeButton(getString(R.string.cancel), null)
                                .show();

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
                final Dialog photoDialog = new Dialog(UserProfileActivity.this,android.R.style.Theme_DeviceDefault_NoActionBar);
                View dialogView = getLayoutInflater().inflate(R.layout.user_photo_dialog,null,false);
                isImageLoaded = false;

                final ImageView dialogImage = dialogView.findViewById(R.id.user_profile_image);

                Glide.with(UserProfileActivity.this).load(userItem.profileImageUrlHttps.replace("_normal","")).listener(new RequestListener<Drawable>() {
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
                                Toast.makeText(UserProfileActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
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

        progressBar.setVisibility(View.GONE);

    }

    private void showDialogAndFeignWork(String userScreenName) {

        new AlertDialog.Builder(UserProfileActivity.this).setTitle("confirm action")
            .setMessage(getString(R.string.unfollow_all_lowercase) + " " + userScreenName + "?")
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    placeboAction();
                }
            }).setNegativeButton(getString(R.string.cancel), null)
            .show();

    }

    private void placeboAction() { // load like you are doing something, then show error message
        showProgressHideButtonText(btnProgressBar, followUnfollowBtn, UserProfileActivity.this);
        int delay = new Random().nextInt(1000) + 1000;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hideProgressShowButtonText(btnProgressBar, followUnfollowBtn, UserProfileActivity.this);
                int minutesLeft = getMinutesLeft(MainActivity.last15MinTimeStamp);
                showSnackBar(constraintLayout, minutesLeft);
            }
        }, delay);
    }

    private boolean checkWritePermission() {
        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }else{
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},WRITE_STORAGE_REQUEST);
            return false;
        }
    }



    private void saveImage(Bitmap bitmap, String title, String description) {
        try {
            if (MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, title, description) != null) {
                Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    class TextClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            int id = v.getId();

            if(id == followersCountTv.getId() || id == followersTv.getId()){
                // view user's followers
                viewUserFollowers();

            }else if(id == followingCountTv.getId() || id == followingTv.getId()){
                //view user's following
                viewUserFollowing();
            }

        }
    }

    private void viewUserFollowing() {
        Intent intent = new Intent(UserProfileActivity.this,UsersListActivity.class);
        intent.putExtra(LIST_NAME,A_USERS_FOLLOWING);
        intent.putExtra(USER_ID, currentItem.id);
        intent.putExtra(SCREEN_NAME_OF_USER, currentItem.screenName);
        startActivity(intent);
    }

    private void viewUserFollowers() {
        Intent intent = new Intent(UserProfileActivity.this,UsersListActivity.class);
        intent.putExtra(LIST_NAME,A_USERS_FOLLOWERS);
        intent.putExtra(USER_ID, currentItem.id);
        intent.putExtra(SCREEN_NAME_OF_USER, currentItem.screenName);
        startActivity(intent);
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
                incrementApiRequestCount(UserProfileActivity.this);
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
                incrementApiRequestCount(UserProfileActivity.this);
            }

            @Override
            public void failure(TwitterException exception) {
                hideProgressShowButtonText(btnProgressBar,followUnfollowBtn,UserProfileActivity.this);
                Toast.makeText(UserProfileActivity.this,exception.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeViews() {
        dialog = new BottomSheetDialog(UserProfileActivity.this);

        popupIcon = findViewById(R.id.popup_icon);
        profileImage = findViewById(R.id.profile_image);
        verifiedIcon = findViewById(R.id.verified_icon);
        nameTv = findViewById(R.id.name_tv);
        screenNameTv = findViewById(R.id.screen_name_tv);
        followersCountTv = findViewById(R.id.followers_count_tv);
        followingCountTv = findViewById(R.id.following_count_tv);
        whitelistStatusTv = findViewById(R.id.whitelisted_status_tv);
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
        followersTv = findViewById(R.id.followers_tv);
        followingTv = findViewById(R.id.following_tv);
        constraintLayout = findViewById(R.id.constraint_layout);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void loadAds() {
        adLoader = new AdLoader.Builder(this,getString(R.string.native_test_ad_unit_id))
                .forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                    @Override
                    public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                        nativeAdView = (UnifiedNativeAdView)getLayoutInflater().inflate(R.layout.native_ad_layout,null,false);
                        mapNativeAdToLayout(unifiedNativeAd, nativeAdView);

                        FrameLayout nativeAdLayout = findViewById(R.id.native_ad);
                        nativeAdLayout.removeAllViews();
                        nativeAdLayout.addView(nativeAdView);
                    }
                })
                .withAdListener(new AdListener(){
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        if(isDestroyed()){
                            if(nativeAdView != null) nativeAdView.destroy();
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(int i) {
                        super.onAdFailedToLoad(i);
                        if(isNetworkConnected(UserProfileActivity.this))
                            adLoader.loadAd(new AdRequest.Builder().build());

                    }
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_test_ad_unit_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                finish();
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                if(isNetworkConnected(UserProfileActivity.this)) mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        });

    }

    private void mapNativeAdToLayout(UnifiedNativeAd adFromGoogle, UnifiedNativeAdView adView) {

        adView.setMediaView((MediaView)adView.findViewById(R.id.ad_media));

        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
        adView.setIconView(adView.findViewById(R.id.ad_icon));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setCallToActionView(adView.findViewById(R.id.call_to_action));
        adView.setStarRatingView(adView.findViewById(R.id.ad_rating));

        ((TextView)adView.getHeadlineView()).setText(adFromGoogle.getHeadline());

        if(adFromGoogle.getBody() == null){
            adView.getBodyView().setVisibility(View.GONE);
        }else{
            ((TextView)adView.getBodyView()).setText(adFromGoogle.getBody());
        }

        if(adFromGoogle.getAdvertiser() == null){
            adView.getAdvertiserView().setVisibility(View.GONE);
        }else{
            ((TextView)adView.getAdvertiserView()).setText(adFromGoogle.getBody());
        }

        if(adFromGoogle.getPrice() == null){
            adView.getPriceView().setVisibility(View.GONE);
        }else{
            ((TextView)adView.getPriceView()).setText(adFromGoogle.getPrice());
        }

        if(adFromGoogle.getStore() == null){
            adView.getStoreView().setVisibility(View.GONE);
        }else{
            ((TextView)adView.getStoreView()).setText(adFromGoogle.getStore());
        }

        if(adFromGoogle.getCallToAction() == null){
            adView.getCallToActionView().setVisibility(View.GONE);
        }else{
            ((Button)adView.getCallToActionView()).setText(adFromGoogle.getCallToAction());
        }

        if(adFromGoogle.getIcon() == null){
            adView.getIconView().setVisibility(View.GONE);
        }else{
            ((ImageView)adView.getIconView()).setImageDrawable(adFromGoogle.getIcon().getDrawable());
        }

        if(adFromGoogle.getStarRating() == null){
            adView.getStarRatingView().setVisibility(View.GONE);
        }else{
            ((RatingBar)adView.getStarRatingView()).setRating(adFromGoogle.getStarRating().floatValue());
        }

        adView.setNativeAd(adFromGoogle);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(userItemArrayList.size() > 1) {
            getMenuInflater().inflate(R.menu.profile_page_menu, menu);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();

        if(itemId == R.id.action_previous){
            // move to the previous useritem in the list
            if(currentUserIndex > 0) {
                --currentUserIndex;
                currentItem = userItemArrayList.get(currentUserIndex);

                constraintLayout.animate().alpha(0f).setDuration(1000).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        constraintLayout.animate().alpha(1).setDuration(1000).setStartDelay(ANIM_START_DELAY).setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                loadDataIntoViews(currentItem);
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {

                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });
                    }
                });

            }else{
                // we have gotten to the end of the list, can't go any further
                Toast.makeText(this,getString(R.string.end_reached),Toast.LENGTH_SHORT).show();
            }

        }else if(itemId == R.id.action_next){

            if(currentUserIndex < userItemArrayList.size()-1) {
                // move to the next item in the list, if there is any
                ++currentUserIndex;
                currentItem = userItemArrayList.get(currentUserIndex);

                if(!currentItem.isSuspendedUser) {

                    constraintLayout.animate().alpha(0f).setDuration(1000).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            constraintLayout.animate().alpha(1).setDuration(1000).setStartDelay(ANIM_START_DELAY).setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {
                                    loadDataIntoViews(currentItem);
                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {

                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {

                                }
                            });
                        }
                    });

                }else{
                    --currentUserIndex;
                    Toast.makeText(this, getString(R.string.end_reached), Toast.LENGTH_SHORT).show();
                }

            }else {
                // we have gotten to the end of the list, can't go any further
                Toast.makeText(this, getString(R.string.end_reached), Toast.LENGTH_SHORT).show();
            }
        }else if(item.getItemId() == android.R.id.home){
            onBackPressed();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if(MainActivity.isShowingAds && mInterstitialAd.isLoaded()){
            mInterstitialAd.show();
        }else{
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == WRITE_STORAGE_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            saveImage(bitmap,imageTitle,imageDescription);
        }

    }

    private class MyPopMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {


        @Override
        public boolean onMenuItemClick(MenuItem item) {
            String title = item.getTitle().toString();

            if(title.equals(getString(R.string.add_to_whitelist))){
                // Add tow whitelist
                progressBar.setVisibility(View.VISIBLE);
                addCurrentItemToWhitelist();

            }else if(title.equals(getString(R.string.remove_from_whitelist))){
                // Remove current user from whitelist
                progressBar.setVisibility(View.VISIBLE);
                removeCurrentItemFromWhiteList();

            }else if(title.equals(getString(R.string.view_followers))){
                // view user's followers
                viewUserFollowers();

            }else if(title.equals(getString(R.string.view_following))){
                // view user's following
                viewUserFollowing();

            }else if(title.equals(getString(R.string.request_follow_back))){
                // Ask the user to follow back
                showRequestFollowBackBottomSheetDialog(UserProfileActivity.this,currentItem.screenName, dialog,twitterApiClient,progressBar);
            }else if(title.equals(getString(R.string.just_view_on_twitter))){
                //view profile on twitter
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/"+ currentItem.screenName));
                    startActivity(intent);
                }catch (Exception e){
                    e.printStackTrace();
                    if( BuildConfig.DEBUG){
                        Toast.makeText(UserProfileActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                }

            }
            return true;
        }

    }

    private void removeCurrentItemFromWhiteList() {

        databaseReference = FirebaseUtil.openDbReference("whitelist/" + twitterSession.getUserId()+"/"+currentItem.id);

        databaseReference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if(task.isSuccessful()){
                    ++MainActivity.keyActionsCount;
                    Toast.makeText(UserProfileActivity.this,currentItem.screenName+" removed successfully",Toast.LENGTH_SHORT).show();

                    if(MainActivity.remindUserToRateApp){
                        checkWhetherToAskUserToRateApp(UserProfileActivity.this);
                    }
                    whitelistStatusTv.setVisibility(View.GONE);

                }else{
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(UserProfileActivity.this,task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                }
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void addCurrentItemToWhitelist() {
        Long signedInUserId = twitterSession.getUserId();
        final Long whitelistedUserId = currentItem.id;
        databaseReference = FirebaseUtil.openDbReference("whitelist/"+signedInUserId+"/"+whitelistedUserId);
        databaseReference.setValue(currentItem.screenName).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if(task.isSuccessful()){
                    MainActivity.nonFollowersIdsList.remove(whitelistedUserId);
                    Toast.makeText(UserProfileActivity.this,currentItem.screenName+" added to whitelist",Toast.LENGTH_SHORT).show();
                    ++MainActivity.keyActionsCount;

                    if(MainActivity.remindUserToRateApp){
                        checkWhetherToAskUserToRateApp(UserProfileActivity.this);
                    }

                    whitelistStatusTv.setVisibility(View.VISIBLE);
                }else{
                    Toast.makeText(UserProfileActivity.this,task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                }

                progressBar.setVisibility(View.GONE);

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(MainActivity.isShowingAds && !mInterstitialAd.isLoaded()){
            if(isNetworkConnected(this)){
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        }
    }
}
