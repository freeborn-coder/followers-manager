package com.gananidevs.followersmanager;

import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.User;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class MyTwitterApiClient extends TwitterApiClient {

    public MyTwitterApiClient(TwitterSession session){
        super(session);
    }

    public UsersShowCustomService getUsersShowService(){
        return getService(UsersShowCustomService.class);
    }

    public FollowersIdsCustomService getFollowersIdsCustomService(){
        return getService(FollowersIdsCustomService.class);
    }

    public FriendsIdsCustomService getFriendsIdsCustomService(){
        return getService(FriendsIdsCustomService.class);
    }

    public UsersLookupCustomService getUsersLookupCustomService(){
        return getService(UsersLookupCustomService.class);
    }

    public FriendshipsCreateCustomService getFriendshipsCreateCustomService(){
        return getService(FriendshipsCreateCustomService.class);
    }

    public FriendshipsDestroyCustomService getFriendshipsDestroyCustomService(){
        return getService(FriendshipsDestroyCustomService.class);
    }

    public StatusUpdateCustomService getStatusUpdateCustomService(){
        return getService(StatusUpdateCustomService.class);
    }

    public SearchTweetsCustomService getSearchTweetsCustomService(){
        return getService(SearchTweetsCustomService.class);
    }

    public SearchUsersCustomService getSearchUsersCustomService(){
        return getService(SearchUsersCustomService.class);
    }


}

interface UsersShowCustomService{
    @GET("/1.1/users/show.json")
    Call<User> get(@Query("user_id") long user_id);
}

interface FollowersIdsCustomService {
    @GET("/1.1/followers/ids.json")
    Call<ResponseBody> get(@Query("user_id") long user_id, @Query("next_cursor") long cursor, @Query("count") int count);
}

interface FriendsIdsCustomService{
    @GET("/1.1/friends/ids.json")
    Call<ResponseBody> get(@Query("user_id") long user_id,@Query("next_cursor") long cursor, @Query("count") int count);
}

interface UsersLookupCustomService{
    @GET("/1.1/users/lookup.json")
    Call<ResponseBody> get(@Query("user_id") String user_ids,@Query("include_entities") boolean include_entities);
}

interface FriendshipsCreateCustomService{
    @POST("1.1/friendships/create.json")
    Call<ResponseBody> post(@Query("user_id") long user_id);
}

interface FriendshipsDestroyCustomService{
    @POST("1.1/friendships/destroy.json")
    Call<ResponseBody> post(@Query("user_id") long user_id);
}

interface StatusUpdateCustomService{
    @POST("1.1/statuses/update.json")
    Call<ResponseBody> post(@Query("status") String status, @Query("trim_user") int trim_user);
}

interface SearchTweetsCustomService{
    @GET("1.1/search/tweets.json")
    Call<ResponseBody> get(@Query("q") String query, @Query("count") int count, @Query("include_entities") boolean include_entities); // q should be urlencoded
}

interface SearchUsersCustomService{
    @GET("1.1/users/search.json")
    Call<ResponseBody> search(@Query("q") String query, @Query("count") int count, @Query("include_entities") boolean include_entities); // q should be urlencodedjkm,
}
