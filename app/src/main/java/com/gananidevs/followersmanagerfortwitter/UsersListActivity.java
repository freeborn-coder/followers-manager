package com.gananidevs.followersmanagerfortwitter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;

import java.util.ArrayList;

import okhttp3.ResponseBody;

import static com.gananidevs.followersmanagerfortwitter.Helper.*;

public class UsersListActivity extends AppCompatActivity {

    ArrayList<UserItem> userItemsList; // itemslist is for the fully hydrated user itsms
    private LinearLayoutManager linearLayoutManager;
    public static Boolean isLoading = false;
    ProgressBar progressBar;
    private UsersRecyclerAdapter recyclerAdapter;
    private RecyclerView recyclerView;
    ArrayList<Long> userIdsList; // idslist is for user ids

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_list);

        userItemsList = new ArrayList<>();
        chooseList();

        progressBar = findViewById(R.id.users_list_progress_bar);
        recyclerView = findViewById(R.id.user_items_rv);
        recyclerView.setHasFixedSize(true);

        recyclerAdapter = new UsersRecyclerAdapter(this,userItemsList);

        linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerAdapter);

        MyRvScrollListener scrollListener = new MyRvScrollListener();
        recyclerView.addOnScrollListener(scrollListener);


        if(userIdsList.size() > 20){
            loadRecyclerViewData(0,20,progressBar,recyclerAdapter,userIdsList);
        }else{
            loadRecyclerViewData(0,userIdsList.size()-1,progressBar,recyclerAdapter,userIdsList);
        }

    }

    private void chooseList() {
        String listName = getIntent().getStringExtra(LIST_NAME);

        if(!listName.isEmpty()){

            switch(listName){
                case NON_FOLLOWERS_LIST: userIdsList = MainActivity.nonFollowersIdsList;
                    break;
                case MUTUAL_FOLLOWERS_LIST: userIdsList = MainActivity.mutualFriendsIdsList;
                    break;
                case FANS_LIST: userIdsList = MainActivity.fansIdsList;
                    break;
                case WHITELISTED_USERS_LIST: userIdsList = MainActivity.whitelistedIdsList;
                    break;
                default:
                    //userIdsList = new ArrayList<>();
            }

        }else{
            Toast.makeText(this,this.getString(R.string.no_valid_list),Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    class MyRvScrollListener extends RecyclerView.OnScrollListener{

        private int previous_total = 0, page_number = 0;

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            int visibleItemCount = linearLayoutManager.getChildCount();
            int totalItemCount = linearLayoutManager.getItemCount();
            int firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();

            if (dy > 0) {
                if (isLoading) {
                    if (totalItemCount > previous_total) {
                        isLoading = false;
                        previous_total = totalItemCount;
                    }

                }

                if (!isLoading && (totalItemCount - visibleItemCount) <= (firstVisibleItemPosition + VIEW_THRESHOLD)) {

                    ++page_number;
                    int startIndex = page_number * ITEM_COUNT;
                    int endIndex = startIndex + ITEM_COUNT;

                    loadRecyclerViewData(startIndex,endIndex,progressBar,recyclerAdapter,userIdsList);
                }

            }
        }

    }


    public void loadRecyclerViewData(int startIndex, int endIndex, final ProgressBar pb, final UsersRecyclerAdapter recyclerAdapter, ArrayList<Long> userIdsList) {
        String userIdsStr = getIdsStr(startIndex,endIndex,userIdsList);

        if(!userIdsStr.isEmpty()) {
            pb.setVisibility(View.VISIBLE);
            isLoading = true;

            MyTwitterApiClient twitterApiClient = new MyTwitterApiClient();
            twitterApiClient.getUsersLookupCustomService().get(userIdsStr).enqueue(new Callback<ResponseBody>() {

                @Override
                public void success(Result<ResponseBody> result) {

                    String resultString = getResultString(result);

                    if(loadUserItemsIntoAdapter(resultString,recyclerAdapter)){
                        isLoading = false;
                        pb.setVisibility(View.GONE);
                    }

                }

                @Override
                public void failure(TwitterException exception) {
                    exception.printStackTrace();
                    isLoading = false;
                }

            });

        }


    }


}
