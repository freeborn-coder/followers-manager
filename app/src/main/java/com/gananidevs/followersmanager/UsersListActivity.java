package com.gananidevs.followersmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;

import java.util.ArrayList;

import okhttp3.ResponseBody;

import static com.gananidevs.followersmanager.Helper.*;

public class UsersListActivity extends AppCompatActivity {

    ArrayList<UserItem> userItemsList; // itemslist is for the fully hydrated user itsms
    private LinearLayoutManager linearLayoutManager;
    public static boolean isLoading = true;
    public static ProgressBar progressBar;
    private UsersRecyclerAdapter recyclerAdapter;
    private RecyclerView recyclerView;
    ArrayList<Long> userIdsList; // idslist is for user ids

    //variables for pagination
    int itemCount = 20, page_number = 0;
    int visibleItemCount, totalItemCount, previous_total = 0;
    int view_threshold = 8;
    boolean hasReachedEnd = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_list);

        try {
            chooseList();
        }catch (Exception e){e.printStackTrace();}

        userItemsList = new ArrayList<>();

        progressBar = findViewById(R.id.users_list_progress_bar);
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
            loadRecyclerViewData(0,userIdsList.size()-1,progressBar,recyclerAdapter,userIdsList);
        }

    }

    private void chooseList() {
        String listName = getIntent().getStringExtra(LIST_NAME);

        if(!listName.isEmpty()){

            try{ getSupportActionBar().setTitle(listName);}catch (Exception e){e.printStackTrace(); }

            switch(listName){
                case NON_FOLLOWERS: userIdsList = MainActivity.nonFollowersIdsList;
                    break;
                case MUTUAL_FOLLOWERS: userIdsList = MainActivity.mutualFriendsIdsList;
                    break;
                case FANS: userIdsList = MainActivity.fansIdsList;
                    break;
                case WHITELISTED_USERS: userIdsList = MainActivity.whitelistedIdsList;
                    break;
                case FOLLOWERS: userIdsList = MainActivity.followersIdsList;
                    break;
                case FOLLOWING: userIdsList = MainActivity.friendsIdsList;
                    break;
                default:
                    userIdsList = new ArrayList<>();
            }

        }else{
            Toast.makeText(this,this.getString(R.string.no_valid_list),Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    public void loadRecyclerViewData(int startIndex, int endIndex, final ProgressBar pb, final UsersRecyclerAdapter recyclerAdapter, ArrayList<Long> idsList) {

        if(endIndex >= idsList.size()){
            endIndex = idsList.size() - 1;
            hasReachedEnd = true;
        }

        String userIdsStr = getIdsStr(startIndex,endIndex,idsList);

        if(!userIdsStr.isEmpty()) {
            pb.setVisibility(View.VISIBLE);

            TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();
            MyTwitterApiClient twitterApiClient = new MyTwitterApiClient(session);
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


}
