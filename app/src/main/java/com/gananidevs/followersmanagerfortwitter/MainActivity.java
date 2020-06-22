package com.gananidevs.followersmanagerfortwitter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.TwitterAuthProvider;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.User;
import static com.gananidevs.followersmanagerfortwitter.Helper.*;
import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.Call;

public class MainActivity extends AppCompatActivity {

    TextView nameTv, screenNameTv, followingCountTv, followersCountTv;
    ImageView profileImage;
    ProgressBar progressBar;
    Button newFollowersBtn, newUnfollowersBtn, nonFollowersBtn, fansBtn, mutualFollowersBtn, whitelistedUsersBtn, searchUsersBtn;

    public static ArrayList<Long> followersIdsList, friendsIdsList, nonFollowersIdsList, mutualFriendsIdsList, fansIdsList, whitelistedIdsList, followersIdsClone;

    private boolean isDataRefreshed = false;
    private TwitterSession activeSession;
    private MyButtonClickHandler buttonClickHandler;
    Boolean isLoading = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Get active twitter session
        activeSession = TwitterCore.getInstance().getSessionManager().getActiveSession();

        if(activeSession != null)
            setUpUI(activeSession);
        else{
            startActivity(new Intent(this, WelcomeScreenActivity.class));
            finish();
        }

        // Sign in to firebase with twitter session if there is no active firebase session
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if(firebaseUser == null) {
            signIntoFirebaseUsingTwitter(activeSession);
        }

    }

    private void setUpUI(TwitterSession activeSession) {

        nameTv = findViewById(R.id.name_tv);
        screenNameTv = findViewById(R.id.screen_name_tv);
        followersCountTv = findViewById(R.id.followers_count_tv);
        followingCountTv = findViewById(R.id.following_count_tv);
        profileImage = findViewById(R.id.main_activity_profile_iv);
        progressBar = findViewById(R.id.users_list_progress_bar);

        //get buttons
        newFollowersBtn = findViewById(R.id.new_followers_btn);
        newUnfollowersBtn = findViewById(R.id.new_unfollowers_btn);
        nonFollowersBtn = findViewById(R.id.non_followers_btn);
        fansBtn = findViewById(R.id.fans_btn);
        mutualFollowersBtn = findViewById(R.id.mutual_followers_btn);
        whitelistedUsersBtn = findViewById(R.id.whitelisted_users_btn);
        searchUsersBtn = findViewById(R.id.search_users_btn);

        // set up handler for the buttons
        buttonClickHandler = new MyButtonClickHandler();

        // set click listener for all the buttons
        newFollowersBtn.setOnClickListener(buttonClickHandler);
        newUnfollowersBtn.setOnClickListener(buttonClickHandler);
        nonFollowersBtn.setOnClickListener(buttonClickHandler);
        fansBtn.setOnClickListener(buttonClickHandler);
        mutualFollowersBtn.setOnClickListener(buttonClickHandler);
        whitelistedUsersBtn.setOnClickListener(buttonClickHandler);
        searchUsersBtn.setOnClickListener(buttonClickHandler);

        // Initialize all ids lists
        followersIdsList = new ArrayList<>();
        fansIdsList = new ArrayList<>();
        friendsIdsList = new ArrayList<>();
        mutualFriendsIdsList = new ArrayList<>();
        whitelistedIdsList = new ArrayList<>();
        nonFollowersIdsList = new ArrayList<>();

        Long currentUserId = activeSession.getUserId();

        // load twitter data for this user
        loadTwitterData(currentUserId);

    }

    private void clearLists(){
        followersIdsList.clear();
        friendsIdsList.clear();
        whitelistedIdsList.clear();
        nonFollowersIdsList.clear();
        fansIdsList.clear();
        mutualFriendsIdsList.clear();
    }

    private void loadTwitterData(final long userId) {
        progressBar.setVisibility(View.VISIBLE);
        isLoading = true;
        isDataRefreshed = false;
        clearLists();

        // Get user data from api using user id
        final MyTwitterApiClient twitterApiClient = new MyTwitterApiClient();
        twitterApiClient.getUsersShowService().get(userId).enqueue(new Callback<User>() {
            @Override
            public void success(Result<User> result) {
                //User user = result.data;
                UserItem user = new UserItem(result.data);
                loadDataIntoUI(user);

                getFollowersIds(userId, twitterApiClient,-1l);
                getFriendsIds(userId, twitterApiClient, -1l);

            }

            @Override
            public void failure(TwitterException exception) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
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
    private void signIntoFirebaseUsingTwitter(TwitterSession session){
        AuthCredential credential = TwitterAuthProvider.getCredential(session.getAuthToken().token, session.getAuthToken().secret);

        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if(!task.isSuccessful()){
                    task.getException().printStackTrace();
                    Log.i("Firebase:","Sign in not successful");
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

                    // after getting all followers ids, clone the list
                    followersIdsClone = (ArrayList<Long>) followersIdsList.clone();

                    //getNewUnfollows(); // get those users who have unfollowed the currentUser

                    // means we have gotten to the last page
                    if (followersIdsList.size() > 0 && friendsIdsList.size() > 0 && !isDataRefreshed) {
                        //getWhitelistedUsersIds(userIdStr);
                        isDataRefreshed = true;
                        getAndLoadCounts();
                        //Toast.makeText(MainActivity.this,"Data refreshed from followers ids req",Toast.LENGTH_SHORT).show();

                    }

                }else if(listAndCursorObject.next_cursor > 0){
                    getFollowersIds(userId, myTwitterApiClient,listAndCursorObject.next_cursor);
                }

            }

            @Override
            public void failure(TwitterException exception) {
                progressBar.setVisibility(View.GONE);
                isLoading = false;
                Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            }

        });

    }

    public void getFriendsIds(final Long userId, final MyTwitterApiClient myTwitterApiClient, Long cursor) {
        // get following ids
        Call<ResponseBody> call = myTwitterApiClient.getFriendsIdsCustomService().get(userId,cursor,Helper.IDS_COUNT_TO_RETRIEVE);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void success(Result<ResponseBody> result) {
                ListAndCursorObject listAndCursorObject = Helper.getIDsList(result);
                friendsIdsList.addAll(listAndCursorObject.list);

                if(listAndCursorObject.next_cursor == 0){ // we're at the last page of response

                    if (followersIdsList.size() > 0 && friendsIdsList.size() > 0 && !isDataRefreshed) {
                        //getWhitelistedUsersIds(userIdStr);
                        isDataRefreshed = true;
                        //Toast.makeText(MainActivity.this,"Data refreshed from friends ids req",Toast.LENGTH_SHORT).show();
                        getAndLoadCounts();
                    }
                }else if(listAndCursorObject.next_cursor > 0){
                    //Log.i("Next page","Getting next page of next_cursor result for friends ids. next next_cursor: "+listAndCursorObject.next_cursor);
                    getFriendsIds(userId,myTwitterApiClient,listAndCursorObject.next_cursor);
                }

            }

            @Override
            public void failure(TwitterException exception) {
                progressBar.setVisibility(View.GONE);
                isLoading = false;
            }
        });
    }

    public void getAndLoadCounts(){
        try {

            int mutualFriendsCount = 0;
            int followersCount = followersIdsList.size();
            int friendsCount = friendsIdsList.size();
            int whitelistCount = whitelistedIdsList.size();

            for (int i = 0; i < friendsCount; i++) {

                Long friendId = friendsIdsList.get(i);
                if (followersIdsList.contains(friendId)) {
                    mutualFriendsIdsList.add(friendId);

                }else{
                    // that means the user with this ID does not follow me (he/she) is a non-follower
                    // add them to non-followers list
                    if(!whitelistedIdsList.contains(friendId)){
                        nonFollowersIdsList.add(friendId);
                    }

                }

            }

            mutualFriendsCount = mutualFriendsIdsList.size();
            // get all fans
            followersIdsList.removeAll(mutualFriendsIdsList);
            followersIdsList.trimToSize();

            fansIdsList = followersIdsList;

            int nonFollowersCount = nonFollowersIdsList.size(); /*friendsCount - mutualFriendsCount - numWhitelist;*/
            nonFollowersCount = (nonFollowersCount >= 0)? nonFollowersCount:0; // check if non-followers count is more than zero, if yes, change nothing, else set it as zero

            int fansCount = fansIdsList.size(); /*followersCount - mutualFriendsCount;*/
            fansCount = (fansCount >= 0)? fansCount:0;

            String nonFollowersBtnText = "Non-Followers (" + Helper.insertCommas(nonFollowersCount) +")";
            String mutualFollowersBtnText = "Mutual Followers (" + Helper.insertCommas(mutualFriendsCount) + ")";
            String fansBtnText = "Fans (" + Helper.insertCommas(fansCount) + ")";
            String whitelistedusersBtnText = "Whitelisted Users (" + Helper.insertCommas(whitelistCount) + ")";
            String newFollowersBtnText = "New Followers (0)";
            String newUnfollowersBtnText = "New Unfollowers (0)";

            // Set all the button texts
            newFollowersBtn.setText(newFollowersBtnText);
            newUnfollowersBtn.setText(newUnfollowersBtnText);
            nonFollowersBtn.setText(nonFollowersBtnText);
            fansBtn.setText(fansBtnText);
            mutualFollowersBtn.setText(mutualFollowersBtnText);
            whitelistedUsersBtn.setText(whitelistedusersBtnText);

            progressBar.setVisibility(View.GONE);
            isLoading = false;
            Toast.makeText(MainActivity.this,"Data loaded",Toast.LENGTH_SHORT).show();

        }catch(Exception e){
            isLoading = false;
            e.printStackTrace();
        }
    }

    class MyButtonClickHandler implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Intent intent;
            switch(v.getId()){
                case R.id.non_followers_btn:
                    intent = new Intent(MainActivity.this,UsersListActivity.class);
                    intent.putExtra(LIST_NAME, NON_FOLLOWERS_LIST);
                    startActivity(intent);
                    break;
                case R.id.mutual_followers_btn:
                    intent = new Intent(MainActivity.this,UsersListActivity.class);
                    intent.putExtra(LIST_NAME, MUTUAL_FOLLOWERS_LIST);
                    startActivity(intent);
                    break;
                case R.id.fans_btn:
                    intent = new Intent(MainActivity.this,UsersListActivity.class);
                    intent.putExtra(LIST_NAME, FANS_LIST);
                    startActivity(intent);
                    break;
                case R.id.new_followers_btn:
                    intent = new Intent(MainActivity.this,UsersListActivity.class);
                    intent.putExtra(LIST_NAME, NEW_FOLLOWERS_LIST);
                    startActivity(intent);
                    break;
                case R.id.whitelisted_users_btn:
                    intent = new Intent(MainActivity.this,UsersListActivity.class);
                    intent.putExtra(LIST_NAME, WHITELISTED_USERS_LIST);
                    startActivity(intent);
                    break;
                case R.id.search_users_btn:
                    // open search activity
                    break;
                case R.id.new_unfollowers_btn:
                    intent = new Intent(MainActivity.this,UsersListActivity.class);
                    intent.putExtra(LIST_NAME, NEW_UNFOLLOWERS_LIST);
                    startActivity(intent);
                    break;
                default:

            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_refresh){
            if(!isLoading) {
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
                .setPositiveButton("yes", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FirebaseAuth.getInstance().signOut();
                        TwitterCore.getInstance().getSessionManager().clearActiveSession();
                        finish();
                    }
                }).setNegativeButton("no", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        }).show();


    }
}