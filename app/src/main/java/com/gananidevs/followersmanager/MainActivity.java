package com.gananidevs.followersmanager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.TwitterAuthProvider;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.User;
import static com.gananidevs.followersmanager.Helper.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import okhttp3.ResponseBody;
import retrofit2.Call;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {


    public static boolean isShowingAds, adsRemovalActive;
    TextView nameTv, screenNameTv, followingCountTv, followersCountTv;
    ImageView profileImage;
    Button newFollowersBtn, newUnfollowersBtn, nonFollowersBtn, fansBtn, mutualFollowersBtn, whitelistedUsersBtn, searchUsersBtn;

    public static ArrayList<Long> followersIdsList, friendsIdsList, nonFollowersIdsList, mutualFriendsIdsList, fansIdsList, whitelistedIdsList, lastSavedFollowersIdsList, newFollowersIdsList, newUnfollowersIdsList;

    private boolean isFollowersAndFriendsListReady = false;
    private TwitterSession activeSession;
    Boolean isLoading = true, hasFinishedBackgroundTask = false, waitingForBackgroundTask = false;
    private UserItem user;

    // REQUESTS LIMIT
    public static int apiRequestCount, keyActionsCount = 0;

    public static boolean remindUserToRateApp = true;

    public static SharedPreferences sp;
    public static long last15MinTimeStamp, lastTimeAskedUserToRateApp, lastTimeShownInterstitial;
    static String followersIdsFileName;
    private static File followersIdsFile;
    Dialog loadingDialog;

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    androidx.appcompat.widget.Toolbar toolbar;
    public static AdRequest adRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileAds.initialize(this);

        /*
        if(BuildConfig.DEBUG){
            List<String> testDeviceIds = Arrays.asList(TECNO_LB7_TEST_ID);
            RequestConfiguration configuration = new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build();
            MobileAds.setRequestConfiguration(configuration);
            adRequest = new AdRequest.Builder().addTestDevice(TECNO_LB7_TEST_ID).build();
        }else{
            adRequest = new AdRequest.Builder().build();
            lastTimeShownInterstitial = 0;
        }

         */

        adRequest = new AdRequest.Builder().build();
        lastTimeShownInterstitial = 0;

        setContentView(R.layout.navigation_drawer);

        // load shardPrefs manager
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        // load settings (preferences) using separate thread
        new Thread(new Runnable(){
            @Override
            public void run() {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                loadSharedPrefs();
            }
        }).start();


        // Get active twitter session
        try {
            activeSession = TwitterCore.getInstance().getSessionManager().getActiveSession();
        }catch (Exception e){
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_SHORT).show();
        }

        if(activeSession != null) {
            followersIdsFileName = activeSession.getUserName() + "_saved_followers_ids";
            followersIdsFile = new File(getFilesDir() + File.pathSeparator + followersIdsFileName);

            setUpUI(activeSession);

            // Sign in to firebase with twitter session if there is no active firebase session
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if(firebaseUser == null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        signIntoFirebaseUsingTwitter(activeSession);
                    }
                }).start();

            }else{
                getWhitelistedIds();
            }

        }else{
            startActivityWithAnimation(new Intent(this, WelcomeScreenActivity.class));
            finish();
        }

    }

    private void loadSharedPrefs() {
        remindUserToRateApp = sp.getBoolean(REMIND_USER_TO_RATE_APP,true);
        apiRequestCount = sp.getInt(REQUEST_COUNT_KEY,0);
        last15MinTimeStamp = sp.getLong(LAST_TIMESTAMP_KEY,0);
        lastTimeAskedUserToRateApp = sp.getLong(LAST_RATE_APP_ASK_TIMESTAMP,0);
        adsRemovalActive = sp.getBoolean(ADS_REMOVAL_ACTIVE,false);

        if(adsRemovalActive){
            // check if it has expired
            long adsRemovalExpiryDate = sp.getLong(ADS_REMOVAL_EXPIRY_DATE,0);
            if(currentTimeInSeconds() > adsRemovalExpiryDate){
                // The ads removal has expired, so the user will continue to see Ads
                adsRemovalActive = false;
                sp.edit().putBoolean(ADS_REMOVAL_ACTIVE, adsRemovalActive).apply();
                isShowingAds = true;
            }else{
                isShowingAds = false;
            }
        }else{
            isShowingAds = true;
        }
    }

    private void setUpUI(final TwitterSession activeSession) {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.open_drawer,R.string.close_drawer);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog_view);
        loadingDialog.setCancelable(false);

        loadingDialog.show();


        nameTv = findViewById(R.id.name_tv);
        screenNameTv = findViewById(R.id.screen_name_tv);
        followersCountTv = findViewById(R.id.followers_count_tv);
        followingCountTv = findViewById(R.id.following_count_tv);
        profileImage = findViewById(R.id.main_activity_profile_iv);

        //get buttons
        newFollowersBtn = findViewById(R.id.new_followers_btn);
        newUnfollowersBtn = findViewById(R.id.new_unfollowers_btn);
        nonFollowersBtn = findViewById(R.id.non_followers_btn);
        fansBtn = findViewById(R.id.fans_btn);
        mutualFollowersBtn = findViewById(R.id.mutual_followers_btn);
        whitelistedUsersBtn = findViewById(R.id.whitelisted_users_btn);
        searchUsersBtn = findViewById(R.id.search_users_btn);

        // set up handler for the buttons
        MyButtonClickListener myButtonClickListener = new MyButtonClickListener();

        // set click listener for all the buttons
        newFollowersBtn.setOnClickListener(myButtonClickListener);
        newUnfollowersBtn.setOnClickListener(myButtonClickListener);
        nonFollowersBtn.setOnClickListener(myButtonClickListener);
        fansBtn.setOnClickListener(myButtonClickListener);
        mutualFollowersBtn.setOnClickListener(myButtonClickListener);
        whitelistedUsersBtn.setOnClickListener(myButtonClickListener);
        searchUsersBtn.setOnClickListener(myButtonClickListener);

        // set click listener for the textviews: followers count, following count, followers text, following text
        followersCountTv.setOnClickListener(myButtonClickListener);
        followingCountTv.setOnClickListener(myButtonClickListener);
        findViewById(R.id.followers_tv).setOnClickListener(myButtonClickListener);
        findViewById(R.id.following_tv).setOnClickListener(myButtonClickListener);

        // Initialize all ids lists
        followersIdsList = new ArrayList<>();
        fansIdsList = new ArrayList<>();
        friendsIdsList = new ArrayList<>();
        mutualFriendsIdsList = new ArrayList<>();
        whitelistedIdsList = new ArrayList<>();
        nonFollowersIdsList = new ArrayList<>();
        newFollowersIdsList = new ArrayList<>();
        newUnfollowersIdsList = new ArrayList<>();
        lastSavedFollowersIdsList = new ArrayList<>();

        long currentUserId = activeSession.getUserId();

        // load twitter data for this user
        loadTwitterData(currentUserId);

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(user != null) {
                    viewProfile();
                }
            }
        });

        nameTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewProfile();
            }
        });

        screenNameTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewProfile();
            }
        });
    }

    private void viewProfile() {
        Intent intent = new Intent(this,UserProfileActivity.class);
        ArrayList<UserItem> userItemsArrayList = new ArrayList<>();
        userItemsArrayList.add(user);
        intent.putParcelableArrayListExtra(USERS_PARCELABLE_ARRAYLIST,userItemsArrayList);
        intent.putExtra(CURRENT_USER_INDEX,0);
        startActivityWithAnimation(intent);
        
    }

    private void clearLists(){
        followersIdsList.clear();
        friendsIdsList.clear();
        //whitelistedIdsList.clear();
        nonFollowersIdsList.clear();
        fansIdsList.clear();
        mutualFriendsIdsList.clear();
        newFollowersIdsList.clear();
        newUnfollowersIdsList.clear();
        lastSavedFollowersIdsList.clear();

        followersCountTv.setText("...");
        followingCountTv.setText("...");
        newFollowersBtn.setText("New Followers (...)");
        newUnfollowersBtn.setText("New Unfollowers (...)");
        nonFollowersBtn.setText("Non Followers (...)");
        fansBtn.setText("Fans (...)");
        mutualFollowersBtn.setText("Mutual Followers (...)");
    }

    private void loadTwitterData(final long userId) {
        //progressBar.setVisibility(View.VISIBLE);
        loadingDialog.show();
        isLoading = true;
        isFollowersAndFriendsListReady = false;
        hasFinishedBackgroundTask = false;
        waitingForBackgroundTask = false;
        clearLists();

        // Get user data from api using user id
        final MyTwitterApiClient twitterApiClient = new MyTwitterApiClient(activeSession);
        twitterApiClient.getUsersShowService().get(userId).enqueue(new Callback<User>() {
            @Override
            public void success(Result<User> result) {
                user = new UserItem(result.data);
                loadDataIntoUI(user);

                getFollowersIds(userId, twitterApiClient,-1L);
                getFriendsIds(userId, twitterApiClient, -1L);

            }

            @Override
            public void failure(TwitterException exception) {
                isLoading = false;
                //progressBar.setVisibility(View.GONE);
                loadingDialog.dismiss();
                Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDataIntoUI(UserItem user) {
        TextView navHeaderTv = findViewById(R.id.nav_header_username_tv);
        navHeaderTv.setText("@" + user.screenName);

        //nameTv.setText(user.name);
        toolbar.setTitle(user.name);
        nameTv.setVisibility(View.GONE);
        screenNameTv.setText(user.screenName);
        followingCountTv.setText(Helper.insertCommas(user.friendsCount));
        followersCountTv.setText(Helper.insertCommas(user.followersCount));

        try {
            Glide.with(this).load(user.profileImageUrlHttps).into(profileImage);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /*
     * Sign in to firebase using the currently active twitter session token or object
     */
    private void signIntoFirebaseUsingTwitter(final TwitterSession session) {
        AuthCredential credential = TwitterAuthProvider.getCredential(session.getAuthToken().token, session.getAuthToken().secret);

        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if (!task.isSuccessful()) {
                    Objects.requireNonNull(task.getException()).printStackTrace();

                    signIntoFirebaseUsingTwitter(session);

                }else {
                    task.isSuccessful();
                    getWhitelistedIds();
                }

            }
        });

    }

    private void getWhitelistedIds() {
        setWhitelistButtonText();

        // get whitelisted users count and ids
        DatabaseReference dbRef = FirebaseUtil.openDbReference("whitelist/" + activeSession.getUserId());

        dbRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Long id = Long.valueOf(Objects.requireNonNull(dataSnapshot.getKey()));
                if (!whitelistedIdsList.contains(id)) {
                    whitelistedIdsList.add(id);
                    nonFollowersIdsList.remove(id);
                }
                setWhitelistButtonText();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                whitelistedIdsList.remove(Long.valueOf(Objects.requireNonNull(dataSnapshot.getKey())));
                setWhitelistButtonText();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setWhitelistButtonText() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String whitelistedUsersText = "Whitelisted Users (" + insertCommas(whitelistedIdsList.size()) + ")";
                whitelistedUsersBtn.setText(whitelistedUsersText);
                if(!isLoading){
                    nonFollowersIdsList.removeAll(whitelistedIdsList);
                    nonFollowersIdsList.trimToSize();
                    String nonFollowersBtnTxt = "Non-Followers ("+nonFollowersIdsList.size()+")";
                    nonFollowersBtn.setText(nonFollowersBtnTxt);
                }
            }
        });
    }


    public void getFollowersIds(final Long userId, final MyTwitterApiClient myTwitterApiClient,Long cursor){
        Call<ResponseBody> call = myTwitterApiClient.getFollowersIdsCustomService().get(userId,cursor,Helper.IDS_COUNT_TO_RETRIEVE);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void success(Result<ResponseBody> result) {

                ListAndCursorObject listAndCursorObject = Helper.getIDsList(result);
                followersIdsList.addAll(listAndCursorObject.list);

                if(listAndCursorObject.next_cursor == 0) {

                    // get those users who have unfollowed the currentUser, making use of weak reference
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                            getNewUnfollowersAndFollowers();
                        }
                    }).start();

                    // means we have gotten to the last page
                    if (followersIdsList.size() > 0 && friendsIdsList.size() > 0 && !isFollowersAndFriendsListReady && hasFinishedBackgroundTask) {
                        //Toast.makeText(MainActivity.this,"Loaded from followers ids method",Toast.LENGTH_LONG).show();
                        isFollowersAndFriendsListReady = true;
                        getAndLoadCounts();

                    }else{
                        if(followersIdsList.size() > 0 && friendsIdsList.size() > 0) waitingForBackgroundTask = true;
                    }

                }else if(listAndCursorObject.next_cursor > 0){
                    getFollowersIds(userId, myTwitterApiClient,listAndCursorObject.next_cursor);
                }

            }

            @Override
            public void failure(TwitterException exception) {
                //progressBar.setVisibility(View.GONE);
                loadingDialog.dismiss();
                isLoading = false;
                Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            }

        });

    }

    private void getNewUnfollowersAndFollowers() {

            try {

                if (followersIdsFile.exists()) {
                    // file exists, so get new unfollowers
                    FileInputStream fis = new FileInputStream(followersIdsFile);
                    ObjectInputStream ois = new ObjectInputStream(fis);

                    lastSavedFollowersIdsList = (ArrayList<Long>)ois.readObject();

                    if(lastSavedFollowersIdsList != null){

                        // get new unfollowers
                        for(Long id:lastSavedFollowersIdsList){

                            if(!followersIdsList.contains(id)){
                                newUnfollowersIdsList.add(id); // it is a new unfollower
                            }
                        }
                    }

                    fis.close();
                    ois.close();

                } else {
                    // the file does not exist, so make a new one
                    if(followersIdsFile.createNewFile()) {
                        saveFollowersIdsToFile();
                    }

                }

                hasFinishedBackgroundTask = true;
                if(waitingForBackgroundTask){

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            waitingForBackgroundTask = false;
                            getAndLoadCounts();
                        }
                    });
                }

            }catch (Exception e){
                e.printStackTrace();
            }
    }

    private void saveFollowersIdsToFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                try {

                    if(!followersIdsFile.exists()){
                        followersIdsFile.createNewFile();
                    }

                    FileOutputStream fileOutputStream = new FileOutputStream(followersIdsFile);
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

                    objectOutputStream.writeObject(followersIdsList);

                    fileOutputStream.close();
                    objectOutputStream.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();

    }

    public void getFriendsIds(final Long userId, final MyTwitterApiClient myTwitterApiClient, Long cursor) {
        // get following ids
        Call<ResponseBody> call = myTwitterApiClient.getFriendsIdsCustomService().get(userId,cursor,Helper.IDS_COUNT_TO_RETRIEVE);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void success(Result<ResponseBody> result) {
                ListAndCursorObject listAndCursorObject = getIDsList(result);
                friendsIdsList.addAll(listAndCursorObject.list);

                if(listAndCursorObject.next_cursor == 0){ // we're at the last page of response

                    if (followersIdsList.size() > 0 && friendsIdsList.size() > 0 && !isFollowersAndFriendsListReady && hasFinishedBackgroundTask) {
                        isFollowersAndFriendsListReady = true;
                        getAndLoadCounts();

                    }else{
                        if(followersIdsList.size() > 0 && friendsIdsList.size() > 0) waitingForBackgroundTask = true;
                    }


                }else if(listAndCursorObject.next_cursor > 0){

                    getFriendsIds(userId,myTwitterApiClient,listAndCursorObject.next_cursor);
                }

            }

            @Override
            public void failure(TwitterException exception) {
                loadingDialog.dismiss();
                if(loadingDialog.isShowing()) loadingDialog.dismiss();
                isLoading = false;
            }
        });
    }

    public void getAndLoadCounts(){
        try {

            int mutualFriendsCount = 0;

            // Loop through people you follow and find non-followers
            for(Long friendId: friendsIdsList){
                if(followersIdsList.contains(friendId)){
                    mutualFriendsIdsList.add(friendId);
                }else{
                    // that means the user with this ID does not follow me (he/she) is a non-follower, add them to non-followers list
                    if(!whitelistedIdsList.contains(friendId)){
                        nonFollowersIdsList.add(friendId);
                    }
                }
            }

            int numLastSavedIds = lastSavedFollowersIdsList.size();

            // find fans: any follower that is not a mutual follower is a fan
            for(Long followerId:followersIdsList){
                if(!mutualFriendsIdsList.contains(followerId)){
                    fansIdsList.add(followerId);
                }

                if(numLastSavedIds > 0 && !lastSavedFollowersIdsList.contains(followerId)){
                    // if the id is not in the last saved followers ids, then it is a new follower
                    newFollowersIdsList.add(followerId);
                }

            }

            // Add the new followers ids to last save followers ids and save it to file
            if(newFollowersIdsList.size() > 0 || newUnfollowersIdsList.size() > 0){
                saveFollowersIdsToFile();
            }

            mutualFriendsCount = mutualFriendsIdsList.size();

            nonFollowersIdsList.removeAll(whitelistedIdsList);
            nonFollowersIdsList.trimToSize();

            int nonFollowersCount = nonFollowersIdsList.size(); /*friendsCount - mutualFriendsCount - numWhitelist;*/

            int fansCount = fansIdsList.size(); /*followersCount - mutualFriendsCount;*/

            String nonFollowersBtnText = "Non-Followers (" + insertCommas(nonFollowersCount) +")";
            String mutualFollowersBtnText = "Mutual Followers (" + insertCommas(mutualFriendsCount) + ")";
            String fansBtnText = "Fans (" + insertCommas(fansCount) + ")";
            String whitelistedUsersText = "Whitelisted Users (" + insertCommas(whitelistedIdsList.size()) + ")";
            String newFollowersBtnText = "New Followers ("+newFollowersIdsList.size()+")";
            String newUnfollowersBtnText = "New Unfollowers ("+newUnfollowersIdsList.size()+")";

            // Set all the followUnfollowButton texts
            newFollowersBtn.setText(newFollowersBtnText);
            newUnfollowersBtn.setText(newUnfollowersBtnText);
            nonFollowersBtn.setText(nonFollowersBtnText);
            fansBtn.setText(fansBtnText);
            mutualFollowersBtn.setText(mutualFollowersBtnText);
            whitelistedUsersBtn.setText(whitelistedUsersText);


            //progressBar.setVisibility(View.GONE);
            loadingDialog.dismiss();
            isLoading = false;
            Toast.makeText(MainActivity.this,"Data loaded",Toast.LENGTH_SHORT).show();

        }catch(Exception e){
            isLoading = false;
            e.printStackTrace();
        }
    }

    class MyButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Intent intent;
            switch(v.getId()){
                case R.id.non_followers_btn:
                    if(nonFollowersIdsList.size() > 0) {
                        intent = new Intent(MainActivity.this, UsersListActivity.class);
                        intent.putExtra(LIST_NAME, NON_FOLLOWERS);
                        startActivityWithAnimation(intent);
                        
                    }else{
                        showEmptyListToast(getString(R.string.nothing_to_view));
                    }
                    break;
                case R.id.mutual_followers_btn:
                    if(mutualFriendsIdsList.size() > 0) {
                        intent = new Intent(MainActivity.this, UsersListActivity.class);
                        intent.putExtra(LIST_NAME, MUTUAL_FOLLOWERS);
                        startActivityWithAnimation(intent);
                        
                    }else{
                        showEmptyListToast(getString(R.string.nothing_to_view));
                    }
                    break;
                case R.id.fans_btn:
                    if(fansIdsList.size() > 0) {
                        intent = new Intent(MainActivity.this, UsersListActivity.class);
                        intent.putExtra(LIST_NAME, FANS);
                        startActivityWithAnimation(intent);
                        
                    }else{
                        showEmptyListToast(getString(R.string.nothing_to_view));
                    }
                    break;
                case R.id.new_followers_btn:
                    if(newFollowersIdsList.size() > 0) {
                        intent = new Intent(MainActivity.this, UsersListActivity.class);
                        intent.putExtra(LIST_NAME, NEW_FOLLOWERS);
                        startActivityWithAnimation(intent);
                        
                    }else{
                        showEmptyListToast(getString(R.string.nothing_to_view));
                    }
                    break;
                case R.id.whitelisted_users_btn:
                    if(whitelistedIdsList.size() > 0) {
                        intent = new Intent(MainActivity.this, UsersListActivity.class);
                        intent.putExtra(LIST_NAME, WHITELISTED_USERS);
                        startActivityWithAnimation(intent);
                    }else{
                        showEmptyListToast(getString(R.string.nothing_to_view));
                    }
                    break;
                case R.id.search_users_btn:
                    // open users list activity to search for users
                    intent = new Intent(MainActivity.this, UsersListActivity.class);
                    intent.putExtra(LIST_NAME, SEARCH_USERS);
                    startActivityWithAnimation(intent);
                    
                    break;
                case R.id.new_unfollowers_btn:
                    /*
                    newUnfollowersIdsList.add(93346325870731056L);
                    newUnfollowersIdsList.add(749137505340833284L);
                    newUnfollowersIdsList.add(749137505340833284L);
                    newUnfollowersIdsList.add(1050272786944389120L);
                    newUnfollowersIdsList.add(1219744441176592386L);
                    newUnfollowersIdsList.add(4819204047L);*/
                    if(newUnfollowersIdsList.size() > 0) {
                        intent = new Intent(MainActivity.this, UsersListActivity.class);
                        intent.putExtra(LIST_NAME, NEW_UNFOLLOWERS);
                        startActivityWithAnimation(intent);
                        
                    }else{
                        showEmptyListToast(getString(R.string.nothing_to_view));
                    }
                    break;
                case R.id.followers_tv:
                case R.id.followers_count_tv:
                    viewUsers(FOLLOWERS); break;
                case R.id.following_count_tv:
                case R.id.following_tv:
                    viewUsers(FOLLOWING); break;
                default:
            }
        }

    }

    private void showEmptyListToast(String message) {
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }

    private void viewUsers(String followersOrFollowing) {
        Intent intent = new Intent(this, UsersListActivity.class);
        intent.putExtra(LIST_NAME,followersOrFollowing);
        startActivityWithAnimation(intent);
        
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_refresh){
            if(!isLoading) {
                if(isNetworkConnected(this))
                    loadTwitterData(activeSession.getUserId());
                else
                    Toast.makeText(this,"No network connectivity.",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this,this.getString(R.string.still_loading),Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {

        final Dialog confirmDialog = new Dialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.confirm_dialog_layout,null,false);
        TextView confirmMessage = dialogView.findViewById(R.id.message_tv);
        confirmMessage.setText(R.string.exit_confirm_dialog_msg);
        Button positiveBtn = dialogView.findViewById(R.id.positive_btn);
        positiveBtn.setText(R.string.yes);
        positiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDialog.dismiss();
                finish();
                
            }
        });

        Button negativeBtn = dialogView.findViewById(R.id.negative_btn);
        negativeBtn.setText(R.string.no);
        negativeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDialog.dismiss();
            }
        });
        confirmDialog.setContentView(dialogView);
        confirmDialog.show();

    }

    private void startActivityWithAnimation(Intent intent){
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.slide_out_right);
    }


    @Override
    protected void onResume() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        super.onResume();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();

        switch(itemId){
            case R.id.action_log_out:{
                logout();
                return true;
            }
            case R.id.action_remove_ads:{
                startActivityWithAnimation(new Intent(this,RemoveAdsActivity.class));
                
                return true;
            }
            case R.id.action_share_app:{
                shareApp();
                return true;
            }
            case R.id.action_rate_app:{
                goToAppStore();
                return true;
            }
            default:
                return false;
        }

    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, R.string.app_share_intent_subject);
        shareIntent.putExtra(Intent.EXTRA_TEXT,"https://play.google.com/store/apps/details?id="+getPackageName());
        startActivityWithAnimation(Intent.createChooser(shareIntent, "Share via"));
        
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void logout() {
        // Sign out the user and start login activity
        if(activeSession != null) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
            TwitterCore.getInstance().getSessionManager().clearActiveSession();

            FirebaseAuth.getInstance().signOut();
            startActivityWithAnimation(new Intent(this, WelcomeScreenActivity.class));

            finish();
            

        }else{
            Toast.makeText(MainActivity.this,R.string.still_loading,Toast.LENGTH_SHORT).show();
        }
    }

    private void goToAppStore() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent goToAppListing = new Intent(Intent.ACTION_VIEW, uri);

        // After pressing back button,to be taken back to our application, we need to add following flags to intent.
        goToAppListing.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivityWithAnimation(goToAppListing);
            
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
            
        }
    }

}