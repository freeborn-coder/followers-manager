package com.gananidevs.followersmanager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;

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
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import okhttp3.ResponseBody;

import static com.gananidevs.followersmanager.Helper.AD_RELOAD_DELAY;
import static com.gananidevs.followersmanager.Helper.A_USERS_FOLLOWERS;
import static com.gananidevs.followersmanager.Helper.A_USERS_FOLLOWING;
import static com.gananidevs.followersmanager.Helper.CURRENT_USER_INDEX;
import static com.gananidevs.followersmanager.Helper.LIST_NAME;
import static com.gananidevs.followersmanager.Helper.SCREEN_NAME_OF_USER;
import static com.gananidevs.followersmanager.Helper.TECNO_LB7_TEST_ID;
import static com.gananidevs.followersmanager.Helper.USERS_PARCELABLE_ARRAYLIST;
import static com.gananidevs.followersmanager.Helper.USER_ID;
import static com.gananidevs.followersmanager.Helper.changeButtonTextAndColor;
import static com.gananidevs.followersmanager.Helper.checkWhetherToAskUserToRateApp;
import static com.gananidevs.followersmanager.Helper.getMinutesLeft;
import static com.gananidevs.followersmanager.Helper.incrementApiRequestCount;
import static com.gananidevs.followersmanager.Helper.insertCommas;
import static com.gananidevs.followersmanager.Helper.isNetworkConnected;
import static com.gananidevs.followersmanager.Helper.proceedWithApiCall;
import static com.gananidevs.followersmanager.Helper.shouldShowInterstitial;
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
    public static ProgressBar progressBar;
    private DatabaseReference databaseReference;
    private AdLoader adLoader;
    private AdRequest adRequest;
    private ProfilePagerAdapter profilePagerAdapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setActionBarTitle(getString(R.string.profile));

        if(MainActivity.isShowingAds) {
            loadAds();
        }

        //initializeViews();
        try {
            twitterSession = TwitterCore.getInstance().getSessionManager().getActiveSession();
            twitterApiClient = new MyTwitterApiClient(twitterSession);

            userItemArrayList = getIntent().getParcelableArrayListExtra(USERS_PARCELABLE_ARRAYLIST);
            currentUserIndex = getIntent().getIntExtra(CURRENT_USER_INDEX, 0);
            currentItem = userItemArrayList.get(currentUserIndex);

            viewPager = findViewById(R.id.view_pager);
            progressBar = findViewById(R.id.progress_bar);
            profilePagerAdapter = new ProfilePagerAdapter(getSupportFragmentManager(), userItemArrayList);
            viewPager.setAdapter(profilePagerAdapter);
            viewPager.setCurrentItem(currentUserIndex);
            viewPager.setPageMargin(10);

            //loadDataIntoViews(currentItem);
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_SHORT).show();
        }

    }

    private void setActionBarTitle(String actionBarTitle) {

        try{
            getSupportActionBar().setTitle(actionBarTitle);
        }catch (Exception e){
            e.printStackTrace();
        }
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

    private void loadAds() {
        adRequest = MainActivity.adRequest;

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                finish();
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(isNetworkConnected(UserProfileActivity.this)) mInterstitialAd.loadAd(adRequest);
                    }
                },AD_RELOAD_DELAY);
            }
        });

        if(shouldShowInterstitial()) {
            mInterstitialAd.loadAd(adRequest);
        }

    }

    public static void mapNativeAdToLayout(UnifiedNativeAd adFromGoogle, UnifiedNativeAdView adView) {

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
            ((TextView)adView.getAdvertiserView()).setText(adFromGoogle.getAdvertiser());
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
            viewPager.setCurrentItem(viewPager.getCurrentItem()-1);

        }else if(itemId == R.id.action_next){
            viewPager.setCurrentItem(viewPager.getCurrentItem()+1);

        }else if(item.getItemId() == android.R.id.home){
            onBackPressed();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if(MainActivity.isShowingAds && shouldShowInterstitial() && mInterstitialAd != null && mInterstitialAd.isLoaded()){

            MainActivity.lastTimeShownInterstitial = System.currentTimeMillis();

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

    @Override
    protected void onResume() {
        super.onResume();
        if(MainActivity.isShowingAds && mInterstitialAd != null && !mInterstitialAd.isLoaded()){
            if(isNetworkConnected(this)){
                mInterstitialAd.loadAd(adRequest);
            }
        }
    }
}
