package com.gananidevs.followersmanager;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.Task;
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

    static final int IDS_COUNT_TO_RETRIEVE = 5000;
    private static final int MIN_KEY_ACTIONS = 4;
    public static final int ONE_DAY_IN_MILLISECONDS = 1000 * 60 * 60 * 24;
    static final int ONE_DAY_IN_SECONDS = 60 * 60 * 24;
    static final int ONE_MINUTE_IN_MILLISECONDS = 1000 * 60;

    static String TECNO_LB7_TEST_ID = "71346B0C1951E4CFD3A0C08DD218BB76";

    // for ads removal
    static final String ADS_REMOVAL_ACTIVE = "user purchased ads removal";
    static final String ADS_REMOVAL_EXPIRY_DATE = "ads removal expiry date";

    static final String USERS_PARCELABLE_ARRAYLIST = "users parcelable arraylist";
    static final String CURRENT_USER_INDEX = "current user index";

    public static long AD_RELOAD_DELAY = 5000;

    static final String DATABASE_URL = "https://followers-manager-for-twitter.firebaseio.com/";

    // Recycler view pagination constants
    public static final int VIEW_THRESHOLD = 6; // made this 15 temp. change to 10 if anything is wrong

    // popup menu constatns
    public static String VIEW_FOLLOWERS = "view followers";
    public static String VIEW_FOLLOWING = "view following";
    public static String ADD_TO_WHITELIST = "add user to whitelist";
    public static String REMOVE_FROM_WHITELIST = "remove user from whitelist";

    //Names for list to help activity identify which list to use

    static final String NON_FOLLOWERS = "Non Followers";
    static final String SCREEN_NAME_OF_USER = "screenNameOfUser";
    static final String LIST_NAME = "list_name";
    static final String MUTUAL_FOLLOWERS = "Mutual Followers";
    static final String FANS = "Fans";
    static final String NEW_FOLLOWERS = "New Followers";
    static final String WHITELISTED_USERS = "Whitelisted Users";
    static final String NEW_UNFOLLOWERS = "New Unfollowers";
    static final String FOLLOWERS = "Followers";
    static final String FOLLOWING = "Following";
    static final String A_USERS_FOLLOWERS = "a users followers";
    static final String A_USERS_FOLLOWING = "a users following";
    static final String USER_ID = "user_id";
    static final String SEARCH_USERS = "search users";
    static final String REMIND_USER_TO_RATE_APP = "remind user to rate app", LAST_RATE_APP_ASK_TIMESTAMP = "last time asked user to rate app";


    static boolean isNetworkConnected(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    // Twitter api Request Limits. check b4 following or unfollowing users
    static final int FIFTEEN_MINUTES = 1000*60*15;
    private static final int MAX_REQUEST_COUNT = 15;
    static final String REQUEST_COUNT_KEY = "currentRequestCount";
    static final String LAST_TIMESTAMP_KEY = "last15minuteTimestamp";

    public static void checkLocationPermission(Context context) {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ((AppCompatActivity)context).requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},110);

        }
    }

    public static boolean shouldShowInterstitial(){
        return System.currentTimeMillis() - MainActivity.lastTimeShownInterstitial > ONE_MINUTE_IN_MILLISECONDS;
    }

    static boolean proceedWithApiCall(Long interval){
        if(interval > FIFTEEN_MINUTES){
            long timeStamp = System.currentTimeMillis();
            MainActivity.sp.edit().putLong(LAST_TIMESTAMP_KEY,timeStamp).apply();
            MainActivity.last15MinTimeStamp = timeStamp;
            MainActivity.apiRequestCount = 0;
            return true;
        }else return MainActivity.apiRequestCount < MAX_REQUEST_COUNT;
    }

    static void checkWhetherToAskUserToRateApp(Context context) {
        long interval = System.currentTimeMillis() - MainActivity.lastTimeAskedUserToRateApp;

        if(interval > ONE_DAY_IN_MILLISECONDS){
            if(MainActivity.keyActionsCount > MIN_KEY_ACTIONS){
                MainActivity.lastTimeAskedUserToRateApp = System.currentTimeMillis();
                MainActivity.sp.edit().putLong(LAST_RATE_APP_ASK_TIMESTAMP,MainActivity.lastTimeAskedUserToRateApp).apply();
                askUserToRateApp(context);
            }
        }

    }

    static long currentTimeInSeconds(){
        return System.currentTimeMillis()/1000;
    }

    // Rate App Dialog
    private static void askUserToRateApp(final Context context) {

        final ReviewManager manager = ReviewManagerFactory.create(context);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(new OnCompleteListener<ReviewInfo>() {
            @Override
            public void onComplete(@NonNull Task<ReviewInfo> task) {
                if (task.isSuccessful()) {

                    // We can get the ReviewInfo object
                    ReviewInfo reviewInfo = task.getResult();
                    manager.launchReviewFlow((AppCompatActivity)context, reviewInfo);
                }
            }
        });

        /*

        final Dialog confirmDialog = new Dialog(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.confirm_dialog_layout,null);

        (dialogView.findViewById(R.id.rating_bar)).setVisibility(View.VISIBLE);

        TextView confirmMessage = dialogView.findViewById(R.id.message_tv);

        confirmMessage.setText(context.getString(R.string.rate_app_request_msg));
        Button positiveBtn = dialogView.findViewById(R.id.positive_btn);

        positiveBtn.setText(context.getString(R.string.okay));

        positiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDialog.dismiss();
                MainActivity.remindUserToRateApp = false;
                MainActivity.sp.edit().putBoolean(REMIND_USER_TO_RATE_APP,false).apply();
                goToAppStore(context);
            }
        });

        Button negativeBtn = dialogView.findViewById(R.id.negative_btn);
        negativeBtn.setText(context.getString(R.string.later));
        negativeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDialog.dismiss();
            }
        });

        Button neutralBtn = dialogView.findViewById(R.id.neutral_btn);
        neutralBtn.setVisibility(View.VISIBLE);
        neutralBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.remindUserToRateApp = false;
                MainActivity.sp.edit().putBoolean(REMIND_USER_TO_RATE_APP,false).apply();
            }
        });

        confirmDialog.setCancelable(false);

        confirmDialog.setContentView(dialogView);
        confirmDialog.show();

         */
    }

    static void goToAppStore(Context context) {
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
    static void incrementApiRequestCount(Context ctx){
        ++MainActivity.apiRequestCount;
        MainActivity.sp.edit().putInt(REQUEST_COUNT_KEY,MainActivity.apiRequestCount).apply();
        // also increment key actions count
        if(MainActivity.remindUserToRateApp){
            ++MainActivity.keyActionsCount;
            checkWhetherToAskUserToRateApp(ctx);
        }
    }

    static void showSnackBar(View v, int minutesLeft){
        Snackbar.make(v,v.getContext().getString(R.string.cannot_carry_out_action)+" "+minutesLeft+v.getContext().getString(R.string.minutes),Snackbar.LENGTH_LONG).show();
    }

    static int getMinutesLeft(long last15MinTimeStamp){
        return (int)(last15MinTimeStamp + FIFTEEN_MINUTES - System.currentTimeMillis())/(1000 * 60);
    }


    // this method returns a number that has been formatted to include commas, as a string
    static String insertCommas(int number){
        String numberString = Integer.toString(number);

        if(number < 1000){
            return numberString;
        }else{
            StringBuilder numberWithComma = new StringBuilder();

            String last3chars;
            int startIndex;
            while(numberString.length() > 3){
                // get the last 3 characters of number string and prepend to stringbuilder

                startIndex = numberString.length() - 3;
                last3chars = numberString.substring(startIndex);

                numberWithComma.insert(0, ',' + last3chars);

                numberString = numberString.substring(0,startIndex); // remove the last 3 characters

            }

            numberWithComma.insert(0, numberString);
            return numberWithComma.toString();
        }

    }

    // Get result as string
    static String getResultString(Result<ResponseBody> result){

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
    static ListAndCursorObject getIDsList(Result<ResponseBody> result){

        ArrayList<Long> idsList;
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

    static String getIdsStr(int startIndex, int endIndex, ArrayList<Long> list){
        StringBuilder idsStr = new StringBuilder();
        try {
            int count = list.size();
            if(count == 1){
                idsStr.append(list.get(0));

            }else {
                for (int i = startIndex; i <= endIndex; i++) {
                    if(i == endIndex){
                        idsStr.append(list.get(i));
                    }else{
                        idsStr.append(list.get(i)).append(",");
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        return idsStr.toString();

    }

    static boolean loadUserItemsIntoAdapter(String resultString, UsersRecyclerAdapter adapter){
        try {
            JSONArray jsonArray = new JSONArray(resultString);

            int start = adapter.userItemsList.size();

            JSONObject jsonObject;

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

    static void changeButtonTextAndColor(Context context, Button btn, int newText, int color){
        btn.setText(context.getString(newText));
        Drawable background = btn.getBackground();
        background.setTint(context.getColor(color));

        btn.setBackground(background);

        //TypedValue.COMPLEX_UNIT_SP
    }

    static void setFollowBtnAppearance(Context context, Long userId, MaterialButton followUnfollowBtn){
        if(Helper.isFriend(userId)){
            changeButtonTextAndColor(context,followUnfollowBtn,R.string.unfollow_all_lowercase,R.color.colorAccent);
        }else{
            changeButtonTextAndColor(context,followUnfollowBtn,R.string.follow_all_lowercase,R.color.colorPrimary);
        }
    }

    /*
     * Check if user with the given Id follows the currently signed in user
     */
    static boolean isFollower(Long user_id){
        return MainActivity.followersIdsList.contains(user_id);

    }

    /*
     * Check if the currently signed in user follows the user with the given id
     */
    static boolean isFriend(Long user_id){
        return MainActivity.friendsIdsList.contains(user_id);
    }

    static boolean isWhielisted(long id) {
        return MainActivity.whitelistedIdsList.contains(id);
    }



    static void showRequestFollowBackBottomSheetDialog(final Context ctx, final String userScreenName, final BottomSheetDialog dialog, final MyTwitterApiClient twitterApiClient, final ProgressBar pb) {
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
                if(pb != null) pb.setVisibility(View.VISIBLE);
                sendTweet(tweetTextTv.getText().toString(),twitterApiClient,ctx,pb);
                dialog.dismiss();
            }
        });

        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(view);
        dialog.show();
    }

    private static void sendTweet(String tweet, MyTwitterApiClient apiClient, final Context ctx, final ProgressBar pb) {

        apiClient.getStatusUpdateCustomService().post(tweet, 1).enqueue(new Callback<ResponseBody>() {
            @Override
            public void success(Result<ResponseBody> result) {
                Toast.makeText(ctx, "follow back request sent successfully", Toast.LENGTH_SHORT).show();
                if(pb != null) pb.setVisibility(View.GONE);
                incrementApiRequestCount(ctx);
            }

            @Override
            public void failure(TwitterException exception) {
                if(pb != null) pb.setVisibility(View.GONE);
                Toast.makeText(ctx, exception.getMessage(), Toast.LENGTH_SHORT).show();
                exception.printStackTrace();
            }

        });

    }


}

class ListAndCursorObject{
    ArrayList<Long> list;
    Long next_cursor;

    ListAndCursorObject(){}
}

