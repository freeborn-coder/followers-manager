package com.gananidevs.followersmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import okhttp3.ResponseBody;

import static com.gananidevs.followersmanager.Helper.*;

public class UsersListActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    ArrayList<UserItem> userItemsList; // itemslist is for the fully hydrated user itsms
    private LinearLayoutManager linearLayoutManager;
    public static boolean isLoading = true;
    public static ProgressBar progressBar;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_list);

        loadAds();

        twitterSession = TwitterCore.getInstance().getSessionManager().getActiveSession();
        twitterApiClient = new MyTwitterApiClient(twitterSession);
        listDescriptionTv = findViewById(R.id.activity_description_tv);

        progressBar = findViewById(R.id.users_list_progress_bar);

        try {
            userItemsList = new ArrayList<>();
            chooseList();
        }catch (Exception e){e.printStackTrace();}

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
                            int startIndex = page_number * itemCount;
                            int endIndex = startIndex + itemCount;

                            if(!hasReachedEnd) {
                                loadRecyclerViewData(startIndex, endIndex, progressBar, recyclerAdapter, userIdsList);
                            }

                        }
                    }

                }

            }
        });

        if(userIdsList.size() > itemCount){
            loadRecyclerViewData(0,itemCount,progressBar,recyclerAdapter,userIdsList);
        }else if(userIdsList.size() > 0){
            loadRecyclerViewData(0,userIdsList.size(),progressBar,recyclerAdapter,userIdsList);
        }

    }

    private void loadAds() {
        adView = findViewById(R.id.banner_ad_view);

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                adView.loadAd(new AdRequest.Builder().build());
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                adView.loadAd(new AdRequest.Builder().build());
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                isAdLoaded = true;
            }

        });

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_test_ad_unit_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                //++MainActivity.numberOfAdsClosed;
                finish();
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        });

        adView.loadAd(new AdRequest.Builder().build());

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
            pb.setVisibility(View.VISIBLE);

            twitterApiClient.getUsersLookupCustomService().get(userIdsStr,false).enqueue(new Callback<ResponseBody>() {

                @Override
                public void success(Result<ResponseBody> result) {

                    String resultString = getResultString(result);

                    if(loadUserItemsIntoAdapter(resultString,recyclerAdapter)){
                        ++page_number;
                        isLoading = true;
                        pb.setVisibility(View.GONE);
                    }

                }

                @Override
                public void failure(TwitterException exception) {
                    Toast.makeText(UsersListActivity.this,exception.getMessage(),Toast.LENGTH_SHORT).show();
                    exception.printStackTrace();
                    isLoading = true;
                    pb.setVisibility(View.GONE);
                }

            });

        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String listName = getIntent().getStringExtra(LIST_NAME);

        if(item.getItemId() == R.id.action_clear){
            if(listName.equals(NEW_FOLLOWERS)){
                MainActivity.newFollowersIdsList.clear();
            }else if(listName.equals(NEW_UNFOLLOWERS)){
                MainActivity.newUnfollowersIdsList.clear();
            }
            userItemsList.clear();
            recyclerAdapter.notifyDataSetChanged();
            userIdsList.clear();
        }else if(item.getItemId() == android.R.id.home){
            onBackPressed();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if(mInterstitialAd.isLoaded()){
            mInterstitialAd.show();
        }else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        String listName = getIntent().getStringExtra(LIST_NAME);
        if(listName.equals(NEW_UNFOLLOWERS) || listName.equals(NEW_FOLLOWERS)){
            getMenuInflater().inflate(R.menu.clear_menu, menu);
            return true;
        }else if(listName.equals(SEARCH_USERS)){
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
