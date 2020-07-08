package com.gananidevs.followersmanager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.util.ProcessUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.concurrent.ThreadFactory;

import okhttp3.ResponseBody;
import retrofit2.Call;

public class MainActivity extends AppCompatActivity {

    TextView nameTv, screenNameTv, followingCountTv, followersCountTv;
    ImageView profileImage;
    ProgressBar progressBar;
    Button newFollowersBtn, newUnfollowersBtn, nonFollowersBtn, fansBtn, mutualFollowersBtn, whitelistedUsersBtn, searchUsersBtn;

    public static ArrayList<Long> followersIdsList, friendsIdsList, nonFollowersIdsList, mutualFriendsIdsList, fansIdsList, whitelistedIdsList, lastSavedFollowersIdsList, newFollowersIdsList, newUnfollowersIdsList;

    private boolean isFollowersAndFriendsListReady = false;
    private TwitterSession activeSession;
    Boolean isLoading = true, hasFinishedBackgroundTask = false, waitingForBackgroundTask = false;
    private UserItem user;

    // REQUESTS LIMIT
    public static int apiRequestCount;
    public static SharedPreferences sp;
    public static long last15MinTimeStamp;
    String followersIdsFileName;
    private File followersIdsFile;
    Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileAds.initialize(this);

        setContentView(R.layout.activity_main);

        if(BuildConfig.DEBUG) {
            //StrictMode.ThreadPolicy threadPolicy = new StrictMode.ThreadPolicy.Builder().detectNetwork().detectDiskReads().detectDiskWrites().penaltyLog().penaltyDialog().build();
            //StrictMode.setThreadPolicy(threadPolicy);
        }

        // load shardPrefs
        Thread t = new Thread(){
            @Override
            public void run() {
                loadSharedPrefs();
            }
        };
        t.setPriority(4);
        t.start();

        // Get active twitter session
        activeSession = TwitterCore.getInstance().getSessionManager().getActiveSession();

        if(activeSession != null) {

            setUpUI(activeSession);

            // Sign in to firebase with twitter session if there is no active firebase session
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if(firebaseUser == null) {
                signIntoFirebaseUsingTwitter(activeSession);
            }else{
                getWhitelistedIds();
            }

        }else{
            startActivity(new Intent(this, WelcomeScreenActivity.class));
            finish();
        }

    }

    private void loadSharedPrefs() {
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        apiRequestCount = sp.getInt(REQUEST_COUNT_KEY,0);
        last15MinTimeStamp = sp.getLong(LAST_TIMESTAMP_KEY,0);
    }

    private void setUpUI(final TwitterSession activeSession) {

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog_view);
        loadingDialog.setCancelable(false);
        loadingDialog.show();

        nameTv = findViewById(R.id.name_tv);
        screenNameTv = findViewById(R.id.screen_name_tv);
        followersCountTv = findViewById(R.id.followers_count_tv);
        followingCountTv = findViewById(R.id.following_count_tv);
        profileImage = findViewById(R.id.main_activity_profile_iv);
        //progressBar = findViewById(R.id.users_list_progress_bar);

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

        Long currentUserId = activeSession.getUserId();

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
        startActivity(intent);
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
        nameTv.setText(user.name);
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
                    task.getException().printStackTrace();

                    if (BuildConfig.DEBUG) {
                        Log.i("Firebase:", "Sign in not successful");
                        Toast.makeText(MainActivity.this, "firebase sign in failed " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    signIntoFirebaseUsingTwitter(session);

                }else if(task.isSuccessful()) {
                    getWhitelistedIds();
                }

            }
        });

    }

    private void getWhitelistedIds() {
        setWhitelistButtonText();
        FirebaseDatabase fbDb = FirebaseDatabase.getInstance(DATABASE_URL);
        // get whitelisted users count and ids
        DatabaseReference dbRef = fbDb.getReference().child("whitelist/" + activeSession.getUserId());

        dbRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Long id = Long.valueOf(dataSnapshot.getKey());
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
                whitelistedIdsList.remove(Long.valueOf(dataSnapshot.getKey()));
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
        String whitelistedUsersText = "Whitelisted Users (" + insertCommas(whitelistedIdsList.size()) + ")";
        whitelistedUsersBtn.setText(whitelistedUsersText);
        if(!isLoading){
            nonFollowersIdsList.removeAll(whitelistedIdsList);
            nonFollowersIdsList.trimToSize();
            String nonFollowersBtnTxt = "Non-Followers ("+nonFollowersIdsList.size()+")";
            nonFollowersBtn.setText(nonFollowersBtnTxt);
        }
    }


    public void getFollowersIds(final Long userId, final MyTwitterApiClient myTwitterApiClient,Long cursor){
        Call<ResponseBody> call = myTwitterApiClient.getFollowersIdsCustomService().get(userId,cursor,Helper.IDS_COUNT_TO_RETRIEVE);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void success(Result<ResponseBody> result) {

                ListAndCursorObject listAndCursorObject = Helper.getIDsList(result);
                followersIdsList.addAll(listAndCursorObject.list);

                if(listAndCursorObject.next_cursor == 0) {


                    new FollowersAsyncTask().execute(); // get those users who have unfollowed the currentUser|will do this later

                    // means we have gotten to the last page
                    if (followersIdsList.size() > 0 && friendsIdsList.size() > 0 && !isFollowersAndFriendsListReady && hasFinishedBackgroundTask) {
                        Toast.makeText(MainActivity.this,"Loaded from followers ids method",Toast.LENGTH_LONG).show();
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

    private  void getNewUnfollowersAndFollowers() {

            try {

                followersIdsFileName = activeSession.getUserName() + "_saved_followers_ids";
                followersIdsFile = new File(getFilesDir() + File.pathSeparator + followersIdsFileName);

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

    class FollowersAsyncTask extends AsyncTask<Void, Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            getNewUnfollowersAndFollowers();
            return null;
        }

    }

    class SaveFollowersIdsTask extends AsyncTask<Void,Void,Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            saveFollowersIdsToFile();
            return null;
        }
    }

    private void saveFollowersIdsToFile() {

        try {

            if(!followersIdsFile.exists()){
                followersIdsFile.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(followersIdsFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(followersIdsList);

            fos.close();
            oos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

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
                //progressBar.setVisibility(View.GONE);
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
                new SaveFollowersIdsTask().execute();
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
                        startActivity(intent);
                    }else{
                        showEmptyListToast(getString(R.string.nothing_to_view));
                    }
                    break;
                case R.id.mutual_followers_btn:
                    if(mutualFriendsIdsList.size() > 0) {
                        intent = new Intent(MainActivity.this, UsersListActivity.class);
                        intent.putExtra(LIST_NAME, MUTUAL_FOLLOWERS);
                        startActivity(intent);
                    }else{
                        showEmptyListToast(getString(R.string.nothing_to_view));
                    }
                    break;
                case R.id.fans_btn:
                    if(fansIdsList.size() > 0) {
                        intent = new Intent(MainActivity.this, UsersListActivity.class);
                        intent.putExtra(LIST_NAME, FANS);
                        startActivity(intent);
                    }else{
                        showEmptyListToast(getString(R.string.nothing_to_view));
                    }
                    break;
                case R.id.new_followers_btn:
                    if(newFollowersIdsList.size() > 0) {
                        intent = new Intent(MainActivity.this, UsersListActivity.class);
                        intent.putExtra(LIST_NAME, NEW_FOLLOWERS);
                        startActivity(intent);
                    }else{
                        showEmptyListToast(getString(R.string.nothing_to_view));
                    }
                    break;
                case R.id.whitelisted_users_btn:
                    if(whitelistedIdsList.size() > 0) {
                        intent = new Intent(MainActivity.this, UsersListActivity.class);
                        intent.putExtra(LIST_NAME, WHITELISTED_USERS);
                        startActivity(intent);
                    }else{
                        showEmptyListToast(getString(R.string.nothing_to_view));
                    }
                    break;
                case R.id.search_users_btn:
                    // open users list activity to search for users
                    intent = new Intent(MainActivity.this, UsersListActivity.class);
                    intent.putExtra(LIST_NAME, SEARCH_USERS);
                    startActivity(intent);
                    break;
                case R.id.new_unfollowers_btn:
                    if(newUnfollowersIdsList.size() > 0) {
                        intent = new Intent(MainActivity.this, UsersListActivity.class);
                        intent.putExtra(LIST_NAME, NEW_UNFOLLOWERS);
                        startActivity(intent);
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
        startActivity(intent);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_refresh){
            if(!isLoading) {
                //getWhitelistedIds();
                loadTwitterData(activeSession.getUserId());
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
        new AlertDialog.Builder(this)
                .setMessage("Sign out before exit?")
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CookieManager.getInstance().removeAllCookies(null);
                        CookieManager.getInstance().flush();
                        FirebaseAuth.getInstance().signOut();
                        TwitterCore.getInstance().getSessionManager().clearActiveSession();
                        finish();
                    }
                }).setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        }).show();


    }
}