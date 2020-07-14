package com.gananidevs.followersmanager;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import okhttp3.ResponseBody;

// this class contains frequently or commonly used functions, and will be available for all
public class Helper {
    private Helper(){}

    public static final int IDS_COUNT_TO_RETRIEVE = 5000;
    public static final int MIN_KEY_ACTIONS = 4;
    public static final int ONE_DAY = 1000 * 60 * 60 * 24;

    public static final String FOLLOWERS_IDS_TYPE = "followers ids";
    public static final String OLD_AND_NEW_FOLLOWERS_TYPE = "old and new followers ids";

    public static final int SLEEP_BOUND = 1500;

    // for ads removal
    public static final String ADS_REMOVAL_ACTIVE = "user purchased ads removal";
    public static final String ADS_REMOVAL_EXPIRY_DATE = "ads removal expiry date";
    public static final String IS_SHOWING_ADS = "is showing ads";


    public static final String USERS_PARCELABLE_ARRAYLIST = "users parcelable arraylist";
    public static final String CURRENT_USER_INDEX = "current user index";


    public static final String DATABASE_URL = "https://followers-manager-for-twitter.firebaseio.com/";

    // Recycler view pagination constants
    public static final int VIEW_THRESHOLD = 6; // made this 15 temp. change to 10 if anything is wrong

    // popup menu constatns
    public static String VIEW_FOLLOWERS = "view followers";
    public static String VIEW_FOLLOWING = "view following";
    public static String ADD_TO_WHITELIST = "add user to whitelist";
    public static String REMOVE_FROM_WHITELIST = "remove user from whitelist";

    //Names for list to help activity identify which list to use

    public static final String NON_FOLLOWERS = "Non Followers";
    public static final String SCREEN_NAME_OF_USER = "screenNameOfUser";
    public static final String LIST_NAME = "list_name";
    public static final String MUTUAL_FOLLOWERS = "Mutual Followers";
    public static final String FANS = "Fans";
    public static final String NEW_FOLLOWERS = "New Followers";
    public static final String WHITELISTED_USERS = "Whitelisted Users";
    public static final String NEW_UNFOLLOWERS = "New Unfollowers";
    public static final String FOLLOWERS = "Followers";
    public static final String FOLLOWING = "Following";
    public static final String A_USERS_FOLLOWERS = "a users followers";
    public static final String A_USERS_FOLLOWING = "a users following";
    public static final String USER_ID = "user_id";
    public static final String SEARCH_USERS = "search users";
    public static final String REMIND_USER_TO_RATE_APP = "remind user to rate app", LAST_RATE_APP_ASK_TIMESTAMP = "last time asked user to rate app";


    public static boolean isNetworkConnected(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    // Twitter api Request Limits. check b4 following or unfollowing users
    public static final int FIFTEEN_MINUTES = 1000*60*15;
    public static final int MAX_REQUEST_COUNT = 15;
    public static final String REQUEST_COUNT_KEY = "currentRequestCount";
    public static final String LAST_TIMESTAMP_KEY = "last15minuteTimestamp";

    public static boolean proceedWithApiCall(Long interval){
        if(interval > FIFTEEN_MINUTES){
            long timeStamp = System.currentTimeMillis();
            MainActivity.sp.edit().putLong(LAST_TIMESTAMP_KEY,timeStamp).apply();
            MainActivity.last15MinTimeStamp = timeStamp;
            MainActivity.apiRequestCount = 0;
            return true;
        }else if(MainActivity.apiRequestCount < MAX_REQUEST_COUNT){
            return true;
        }else{
            return false;
        }
    }

    public static void checkWhetherToAskUserToRateApp(Context context) {
        long interval = System.currentTimeMillis() - MainActivity.lastTimeAskedUserToRateApp;

        if(interval > ONE_DAY){
            if(MainActivity.keyActionsCount > MIN_KEY_ACTIONS){
                MainActivity.lastTimeAskedUserToRateApp = System.currentTimeMillis();
                MainActivity.sp.edit().putLong(LAST_RATE_APP_ASK_TIMESTAMP,MainActivity.lastTimeAskedUserToRateApp).apply();
                askUserToRateApp(context);
            }
        }

    }

    // Rate App Dialog
    private static void askUserToRateApp(final Context context) {
        new AlertDialog.Builder(context).setMessage("If you are enjoying this app, please take a minute of your time to give us a 5 star rating.")
            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MainActivity.remindUserToRateApp = false;
                    MainActivity.sp.edit().putBoolean(REMIND_USER_TO_RATE_APP,false).apply();
                    goToAppStore(context);

                }
            })
            .setNegativeButton("later",null)
            .setNeutralButton("Never", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MainActivity.remindUserToRateApp = false;
                    MainActivity.sp.edit().putBoolean(REMIND_USER_TO_RATE_APP,false).apply();
                }
            })
            .show();

    }

    public static void goToAppStore(Context context) {
        Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
        Intent goToAppListing = new Intent(Intent.ACTION_VIEW, uri);

        // After pressing back button,to be taken back to our application, we need to add following flags to intent.
        goToAppListing.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            context.startActivity(goToAppListing);
        } catch (ActivityNotFoundException e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
        }
    }


    /*
     * increment request count and save to sharedpreferences, and also remind user to rate app, if possible
     */
    public static void incrementApiRequestCount(Context ctx){
        ++MainActivity.apiRequestCount;
        MainActivity.sp.edit().putInt(REQUEST_COUNT_KEY,MainActivity.apiRequestCount).apply();
        // also increment key actions count
        if(MainActivity.remindUserToRateApp){
            ++MainActivity.keyActionsCount;
            checkWhetherToAskUserToRateApp(ctx);
        }
    }

    public static void showSnackBar(View v, int minutesLeft){
        Snackbar.make(v,v.getContext().getString(R.string.cannot_carry_out_action)+" "+minutesLeft+v.getContext().getString(R.string.minutes),Snackbar.LENGTH_LONG).show();
    }

    public static int getMinutesLeft(long last15MinTimeStamp){
        return (int)(last15MinTimeStamp + FIFTEEN_MINUTES - System.currentTimeMillis())/(1000 * 60);
    }


    // this method returns a number that has been formatted to include commas, as a string
    public static String insertCommas(int number){
        String numberString = Integer.toString(number);

        if(number < 1000){
            return numberString;
        }else{
            String numberWithComma = "";

            while(numberString.length() > 3){
                // get the last 3 characters of number string and prepend to stringbuilder

                int startIndex = numberString.length() - 3;
                String last3chars = numberString.substring(startIndex);

                numberWithComma = ',' + last3chars + numberWithComma;

                numberString = numberString.substring(0,startIndex); // remove the last 3 characters

            }

            numberWithComma = numberString + numberWithComma;
            return numberWithComma;
        }

    }

    // Get result as string
    public static String getResultString(Result<ResponseBody> result){

        String resultStr = "";
        try{
            resultStr = result.data.string();
        }catch (Exception e){
            e.printStackTrace();
        }

        return resultStr;
    }

    /*
     * Load array list of user followers ids or friends ids from result object
     */
    public static ListAndCursorObject getIDsList(Result<ResponseBody> result){

        ArrayList<Long> idsList = null;
        ListAndCursorObject obj = new ListAndCursorObject();

        try {
            String response = result.data.string();

            JSONObject jsonObject = new JSONObject(response);
            JSONArray idsJsonArray = jsonObject.getJSONArray("ids");
            obj.next_cursor = jsonObject.getLong("next_cursor");

            idsList = new ArrayList<>();

            if(idsJsonArray != null){
                for(int i=0;i<idsJsonArray.length(); i++){
                    idsList.add(idsJsonArray.getLong(i));
                }
                obj.list = idsList;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static String getIdsStr(int startIndex, int endIndex, ArrayList<Long> list){
        String idsStr = "";
        try {
            int count = list.size();
            if(count == 1){
                idsStr += list.get(0);

            }else {
                for (int i = startIndex; i <= endIndex; i++) {
                    if(i == endIndex){
                        idsStr += list.get(i);
                    }else{
                        idsStr += list.get(i) + ",";
                    }
                }

            }
            //if(endIndex == count - 1) idsStr += "," + list.get(endIndex); // if it's last item of list, add the final id
        }catch(Exception e){
            e.printStackTrace();
        }

        return idsStr;

    }

    public static boolean loadUserItemsIntoAdapter(String resultString,UsersRecyclerAdapter adapter){
        try {
            JSONArray jsonArray = new JSONArray(resultString);

            int start = adapter.userItemsList.size();

            JSONObject jsonObject = null;

            for(int i = 0; i < jsonArray.length(); i++){

                jsonObject = jsonArray.getJSONObject(i);

                UserItem userItem = new UserItem(jsonObject);

                adapter.userItemsList.add(adapter.userItemsList.size(), userItem);

            }

            adapter.notifyItemRangeInserted(start, jsonArray.length());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;

    }

    public static void changeButtonTextAndColor(Context context, MaterialButton btn, int newText, int color){
        btn.setText(context.getString(newText));
        Drawable background = btn.getBackground();
        background.setTint(context.getColor(color));
        btn.setBackgroundDrawable(background);

        //TypedValue.COMPLEX_UNIT_SP
    }

    public static void setFollowBtnAppearance(Context context, Long userId, MaterialButton followUnfollowBtn){
        if(Helper.isFriend(userId)){
            changeButtonTextAndColor(context,followUnfollowBtn,R.string.unfollow_all_lowercase,R.color.colorAccent);
        }else{
            changeButtonTextAndColor(context,followUnfollowBtn,R.string.follow_all_lowercase,R.color.colorPrimary);
        }
    }

    /*
     * Check if user with the given Id follows the currently signed in user
     */
    public static boolean isFollower(Long user_id){
        return MainActivity.followersIdsList.contains(user_id);

    }

    /*
     * Check if the currently signed in user follows the user with the given id
     */
    public static boolean isFriend(Long user_id){
        return MainActivity.friendsIdsList.contains(user_id);
    }

    public static boolean isWhielisted(long id) {
        return MainActivity.whitelistedIdsList.contains(id);
    }



    public static void showRequestFollowBackBottomSheetDialog(final Context ctx, final String userScreenName, final BottomSheetDialog dialog, final MyTwitterApiClient twitterApiClient, final ProgressBar pb) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.request_followback_dialog,null);
        RadioGroup radioGroup = view.findViewById(R.id.radio_group);
        MaterialButton sendButton = view.findViewById(R.id.send_button);

        final TextView tweetTextTv = view.findViewById(R.id.request_tweet_tv);
        String requestTweet = "@"+userScreenName+" "+ctx.getString(R.string.kindly_follow_back);
        tweetTextTv.setText(requestTweet);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                String requestTweet = "@"+userScreenName+" ";
                if(checkedId == R.id.pls_follow_back_radio){

                    requestTweet += ctx.getString(R.string.please_follow_back);

                }else if(checkedId == R.id.kindly_follow_back_radio){
                    requestTweet += ctx.getString(R.string.kindly_follow_back);
                }
                tweetTextTv.setText(requestTweet);
            }

        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pb.setVisibility(View.VISIBLE);
                sendTweet(tweetTextTv.getText().toString(),twitterApiClient,ctx,pb);
                dialog.dismiss();
            }
        });

        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(view);
        dialog.show();
    }

    public static void sendTweet(String tweet, MyTwitterApiClient apiClient, final Context ctx, final ProgressBar pb) {

        apiClient.getStatusUpdateCustomService().post(tweet, 1).enqueue(new Callback<ResponseBody>() {
            @Override
            public void success(Result<ResponseBody> result) {
                Toast.makeText(ctx, "follow back request sent successfully", Toast.LENGTH_SHORT).show();
                pb.setVisibility(View.GONE);
                incrementApiRequestCount(ctx);
            }

            @Override
            public void failure(TwitterException exception) {
                pb.setVisibility(View.GONE);
                Toast.makeText(ctx, exception.getMessage(), Toast.LENGTH_SHORT).show();
                exception.printStackTrace();
            }

        });

    }


}

class ListAndCursorObject{
    public ArrayList<Long> list;
    public Long next_cursor;

    public void ListAndCursorObject(ArrayList<Long>list, Long cursor){
        this.list = list;
        this.next_cursor = cursor;
    }
}

