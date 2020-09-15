package com.gananidevs.followersmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.ResponseBody;

import static com.gananidevs.followersmanager.Helper.*;

public class UsersListActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    ArrayList<UserItem> userItemsList; // itemslist is for the fully hydrated user items
    private LinearLayoutManager linearLayoutManager;
    public static boolean isLoading = true;
    public static ProgressBar progressBar;
    public static ConstraintLayout constraintLayout;
    private UsersRecyclerAdapter recyclerAdapter;
    public static RecyclerView recyclerView;
    ArrayList<Long> userIdsList; // idslist is for user ids

    //variables for pagination
    int itemCount = 20, page_number = 0;
    int visibleItemCount, totalItemCount, previous_total = 0;
    int view_threshold = 8;
    boolean hasReachedEnd = false;
    private TwitterSession twitterSession;
    private MyTwitterApiClient twitterApiClient;
    long twitterCursor = -1;
    final int USER_IDS_COUNT = 500;
    TextView listDescriptionTv;
    private SearchView searchView;
    private AdView adView;
    private boolean isAdLoaded = false;
    private InterstitialAd mInterstitialAd;

    //Helper object for moving between profiles in profile activity
    UsersListHelper usersListHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_list);

        // get the Ad view
        adView = findViewById(R.id.banner_ad_view);
        if(MainActivity.isShowingAds)
            loadAds();
        else
            adView.setVisibility(View.GONE);

        twitterSession = TwitterCore.getInstance().getSessionManager().getActiveSession();
        twitterApiClient = new MyTwitterApiClient(twitterSession);
        listDescriptionTv = findViewById(R.id.activity_description_tv);

        progressBar = findViewById(R.id.users_list_progress_bar);

        try {
            userItemsList = new ArrayList<>();
            chooseList();
        }catch (Exception e){e.printStackTrace();}

        constraintLayout = findViewById(R.id.constraint_layout);
        recyclerView = findViewById(R.id.user_items_rv);
        recyclerView.setHasFixedSize(true);

        recyclerAdapter = new UsersRecyclerAdapter(this,userItemsList);

        linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerAdapter);

        // SO that whitelsited users will show the remove text in the button
        String listName = getIntent().getStringExtra(LIST_NAME);

        if(listName.equals(WHITELISTED_USERS)){
            recyclerAdapter.isViewingWhitelist = true;
        }else if(listName.equals(NON_FOLLOWERS) || listName.equals(NEW_UNFOLLOWERS)){
            recyclerAdapter.showDropdown = true; // show dropdown arrow in user item layout
        }

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0) {
                    visibleItemCount = linearLayoutManager.getChildCount();
                    totalItemCount = linearLayoutManager.getItemCount();
                    int firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();

                    if (isLoading) {
                        if ((visibleItemCount + firstVisibleItemPosition + view_threshold) >= totalItemCount) {

                            isLoading = false;

                            // Do pagination.. i.e. fetch new data
                            final int startIndex = page_number * itemCount;
                            final int endIndex = startIndex + itemCount;

                            if(!hasReachedEnd) {
                                if(isNetworkConnected(getBaseContext())) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                                            loadRecyclerViewData(startIndex, endIndex, progressBar, recyclerAdapter, userIdsList);
                                        }
                                    }).start();

                                }else{
                                    Toast.makeText(getBaseContext(),"No network connectivity!",Toast.LENGTH_LONG).show();
                                }
                            }

                        }
                    }

                }

            }
        });

        if(userIdsList.size() > itemCount  || getIntent().getStringExtra(LIST_NAME).equals(NEW_UNFOLLOWERS) || getIntent().getStringExtra(LIST_NAME).equals(NEW_FOLLOWERS)){
            loadRecyclerViewData(0,itemCount,progressBar,recyclerAdapter,userIdsList);
        }else if(userIdsList.size() > 0){
            loadRecyclerViewData(0,userIdsList.size(),progressBar,recyclerAdapter,userIdsList);
        }

        usersListHelper = new UsersListHelper() {
            @Override
            public UserItem getNextItem(int currentIndex) {
                return userItemsList.get(currentIndex+1);
            }

            @Override
            public UserItem getPreviousItem(int currentIndex) {
                return userItemsList.get(currentIndex-1);
            }
        };


    }

    private void loadAds() {


        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                //adView.loadAd(new AdRequest.Builder().build());
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(isNetworkConnected(UsersListActivity.this)) adView.loadAd(MainActivity.adRequest);
                        }
                    },AD_RELOAD_DELAY);

            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                isAdLoaded = true;
            }

        });

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));

        if(shouldShowInterstitial()) {
            mInterstitialAd.loadAd(MainActivity.adRequest);
        }

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                finish();
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(isNetworkConnected(UsersListActivity.this)) mInterstitialAd.loadAd(MainActivity.adRequest);
                    }
                },AD_RELOAD_DELAY);

            }
        });
        adView.loadAd(MainActivity.adRequest);
    }

    private void chooseList() {
        String listName = getIntent().getStringExtra(LIST_NAME);

        if(!listName.isEmpty()){

            assert getSupportActionBar() != null;
            ActionBar actionBar = getSupportActionBar();
            actionBar.setTitle(listName);
            actionBar.setDisplayHomeAsUpEnabled(true);

            switch(listName){
                case NON_FOLLOWERS: userIdsList = MainActivity.nonFollowersIdsList; break;
                case MUTUAL_FOLLOWERS:
                        userIdsList = MainActivity.mutualFriendsIdsList;
                        setListDescription(getString(R.string.people_who_follow_you_back));
                        break;
                case FANS:
                    userIdsList = MainActivity.fansIdsList;
                    setListDescription(getString(R.string.people_you_dont_follow_back));
                    break;
                case WHITELISTED_USERS:
                    userIdsList = MainActivity.whitelistedIdsList;
                    setListDescription(getString(R.string.people_you_dont_wanna_unfollow));
                    break;
                case FOLLOWERS:
                    userIdsList = MainActivity.followersIdsList;
                    setListDescription(getString(R.string.people_who_follow_you));
                    break;
                case FOLLOWING: userIdsList = MainActivity.friendsIdsList;
                    setListDescription(getString(R.string.people_you_are_following));
                    break;
                case A_USERS_FOLLOWERS:{
                    String screenNameOfUser = getIntent().getStringExtra(SCREEN_NAME_OF_USER);
                    getSupportActionBar().setTitle(FOLLOWERS);
                    setListDescription(getString(R.string.people_who_follow)+" @"+screenNameOfUser);
                    userIdsList = new ArrayList<>();
                    Long userId = getIntent().getLongExtra(USER_ID,0);
                    getMoreFollowersIds(userId,true);
                    break;
                }
                case A_USERS_FOLLOWING:{
                    String screenNameOfUser = getIntent().getStringExtra(SCREEN_NAME_OF_USER);
                    getSupportActionBar().setTitle(FOLLOWING);
                    setListDescription("People @"+screenNameOfUser+" is following");
                    userIdsList = new ArrayList<>();
                    Long userId = getIntent().getLongExtra(USER_ID,0);
                    getMoreFriendsIds(userId,true);
                    break;
                }
                case NEW_FOLLOWERS:
                    userIdsList = MainActivity.newFollowersIdsList;
                    setListDescription(getString(R.string.who_followed_you_recently));
                    break;
                case NEW_UNFOLLOWERS:
                    userIdsList = MainActivity.newUnfollowersIdsList;
                    setListDescription(getString(R.string.who_unfollowed_recently));
                    break;
                case SEARCH_USERS:
                    userIdsList = new ArrayList<>();
                    listDescriptionTv.setText(getString(R.string.search_results));
                    progressBar.setVisibility(View.GONE);
                    break;
                default:
                    userIdsList = new ArrayList<>();
            }

        }else{
            Toast.makeText(this,this.getString(R.string.no_valid_list),Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private void setListDescription(String description) {
        listDescriptionTv.setText(description);
    }

    private void getMoreFriendsIds(Long userId, final boolean isFirstLoad) {

        twitterApiClient.getFriendsIdsCustomService().get(userId,twitterCursor,USER_IDS_COUNT)
        .enqueue(new Callback<ResponseBody>() {
            @Override
            public void success(Result<ResponseBody> result) {
                addIdsToIdsList(result);
                if(isFirstLoad){
                    loadRecyclerViewData(0, itemCount, progressBar, recyclerAdapter, userIdsList);
                }
            }

            @Override
            public void failure(TwitterException exception) {
                Toast.makeText(UsersListActivity.this,exception.getMessage(),Toast.LENGTH_SHORT).show();
            }

        });

    }

    private void addIdsToIdsList(Result<ResponseBody> result) {
        ListAndCursorObject listAndCursorObject = getIDsList(result);
        userIdsList.addAll(listAndCursorObject.list);
        twitterCursor = listAndCursorObject.next_cursor;
    }

    private void getMoreFollowersIds(Long userId, final boolean isFirstLoad) {
        twitterApiClient.getFollowersIdsCustomService().get(userId,twitterCursor,USER_IDS_COUNT)
            .enqueue(new Callback<ResponseBody>() {
                @Override
                public void success(Result<ResponseBody> result) {
                    addIdsToIdsList(result);
                    if(isFirstLoad) {
                        loadRecyclerViewData(0, itemCount, progressBar, recyclerAdapter, userIdsList);
                    }
                }

                @Override
                public void failure(TwitterException exception) {
                    Toast.makeText(UsersListActivity.this,exception.getMessage(),Toast.LENGTH_SHORT).show();
                }

            });
    }

    public void loadRecyclerViewData(int startIndex, int endIndex, final ProgressBar pb, final UsersRecyclerAdapter recyclerAdapter, ArrayList<Long> idsList) {
        endIndex = endIndex -1; // 0 - 19, 20 - 39, 40 - 59 startindex remains the same

        if(endIndex >= idsList.size()){
            endIndex = idsList.size() - 1;

            if(twitterCursor > 0) {
                String listName = getIntent().getStringExtra(LIST_NAME);
                long userId = getIntent().getLongExtra(USER_ID,0);

                if(userId > 0){
                    if(listName.equals(A_USERS_FOLLOWERS)){

                        getMoreFollowersIds(userId,false);

                    }else if(listName.equals(A_USERS_FOLLOWING)){
                        getMoreFriendsIds(userId,false);
                    }
                }

            }else{
                hasReachedEnd = true;
            }
        }

        String userIdsStr = getIdsStr(startIndex,endIndex,idsList);

        if(!userIdsStr.isEmpty()) {
            showOrHideProgressBar(pb,View.VISIBLE);

            twitterApiClient.getUsersLookupCustomService().get(userIdsStr,false).enqueue(new Callback<ResponseBody>() {

                @Override
                public void success(final Result<ResponseBody> result) {

                    String resultString = getResultString(result);

                    loadUserItemsIntoAdapter(resultString,recyclerAdapter);

                    if(getIntent().getStringExtra(LIST_NAME).equals(NEW_UNFOLLOWERS)) {
                        ArrayList<Long> suspendedUserIds = getSuspendedUserIds();
                        if (suspendedUserIds.size() > 0) addSuspendedUsers(suspendedUserIds);
                    }
                    ++page_number;
                    isLoading = true;
                    showOrHideProgressBar(pb,View.GONE);
                    //pb.setVisibility(View.GONE);

                }

                @Override
                public void failure(TwitterException exception) {

                    if(exception.getMessage().toLowerCase().contains("status: 404")){ // that means there are some suspended users

                        if(getIntent().getStringExtra(LIST_NAME).equals(NEW_UNFOLLOWERS)) {
                            ArrayList<Long> suspendedUserIds = getSuspendedUserIds();
                            // add each item to indicate a disabled user
                            addSuspendedUsers(suspendedUserIds);
                            showOrHideProgressBar(pb,View.GONE);
                            //pb.setVisibility(View.GONE);
                        }

                    }else{

                        Toast.makeText(UsersListActivity.this,exception.getMessage(),Toast.LENGTH_SHORT).show();
                        exception.printStackTrace();
                        isLoading = true;
                        showOrHideProgressBar(pb,View.GONE);
                        //pb.setVisibility(View.GONE);
                    }
                }

                private void addSuspendedUsers(ArrayList<Long> disabledIdsList) {
                   int start = userItemsList.size();
                    for(Long id:disabledIdsList){
                        UserItem item = new UserItem(""+id,true);
                        userItemsList.add(item);
                    }
                    recyclerAdapter.notifyItemRangeInserted(start,disabledIdsList.size());
                }

            });

        }

    }

    private void showOrHideProgressBar(final ProgressBar pb, final int visibility) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pb.setVisibility(visibility);
            }
        });
    }

    private ArrayList<Long> getSuspendedUserIds() {
        int disableUserCount = userIdsList.size() - userItemsList.size();
        ArrayList<Long> disabledUserIdsList = new ArrayList<>();
        if(disableUserCount > 0) {
            ArrayList<Long> enabledUserIdsList = new ArrayList<>();

            // get enabled user ids
            for (UserItem i : userItemsList) {
                enabledUserIdsList.add(i.id);
            }

            for (Long id : userIdsList) {
                if (!enabledUserIdsList.contains(id)) {
                    disabledUserIdsList.add(id);
                }
            }
        }
        return disabledUserIdsList;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //String listName = getIntent().getStringExtra(LIST_NAME);

        if(item.getItemId() == android.R.id.home){
            onBackPressed();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if(MainActivity.isShowingAds && mInterstitialAd.isLoaded() && shouldShowInterstitial()){
            mInterstitialAd.show();
            MainActivity.lastTimeShownInterstitial = System.currentTimeMillis();
        }else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        String listName = getIntent().getStringExtra(LIST_NAME);
        if(listName.equals(SEARCH_USERS)){
            getMenuInflater().inflate(R.menu.users_search_menu, menu);
            MenuItem searchMenuItem = menu.findItem(R.id.action_search);
            searchView = (SearchView) searchMenuItem.getActionView();
            searchView.setOnQueryTextListener(this);
            searchView.setQueryHint(getString(R.string.search));
            searchMenuItem.expandActionView();
            return true;
        }else{
            return super.onCreateOptionsMenu(menu);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if(newText.trim().length() > 0){
            userItemsList.clear();
            recyclerAdapter.notifyDataSetChanged();
            searchUsers(newText);
        }
        return true;
    }

    private void searchUsers(String query) {
        progressBar.setVisibility(View.VISIBLE);

        try{
            query = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        }catch (Exception e){e.printStackTrace(); }

        twitterApiClient.getSearchUsersCustomService().search(query,15,false).enqueue(new Callback<ResponseBody>() {
            @Override
            public void success(Result<ResponseBody> result) {
                String resultString = getResultString(result);
                loadUserItemsIntoAdapter(resultString,recyclerAdapter);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void failure(TwitterException exception) {
                Toast.makeText(UsersListActivity.this,exception.getMessage(),Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onStart() {
        if(searchView != null){
            searchView.requestFocus();
        }
        super.onStart();
    }

}
