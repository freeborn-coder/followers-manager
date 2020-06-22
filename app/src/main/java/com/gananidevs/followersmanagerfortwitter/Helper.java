package com.gananidevs.followersmanagerfortwitter;

import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.models.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import okhttp3.ResponseBody;

// this class contains frequently or commonly used functions, and will be available for all
public class Helper {
    private Helper(){}

    public static final int IDS_COUNT_TO_RETRIEVE = 5000;

    // Recycler view pagination constants
    public static final int ITEM_COUNT = 20;
    public static final int VIEW_THRESHOLD = 2; // made this 15 temp. change to 10 if anything is wrong

    //Names for list to help activity identify which list to use
    public static final String NON_FOLLOWERS_LIST = "non_followers_list";
    public static final String LIST_NAME = "list_name";
    public static final String MUTUAL_FOLLOWERS_LIST = "mutual_followers_list";
    public static final String FANS_LIST = "fans_list";
    public static final String NEW_FOLLOWERS_LIST = "new_followers_list";
    public static final String WHITELISTED_USERS_LIST = "whitelisted_users_list";
    public static final String NEW_UNFOLLOWERS_LIST = "new_unfollowers_list";

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

            int idsCount = list.size();
            if(idsCount == 1){
                idsStr += list.get(0);

            }else {
                for (int i = startIndex; i < endIndex; i++) {
                    if (i < idsCount) {
                        idsStr += list.get(i) + ",";
                    }
                }
                if (idsStr.length() > 0) {
                    idsStr = idsStr.substring(0, idsStr.length() - 1);
                }
            }
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

}

class ListAndCursorObject{
    public ArrayList<Long> list;
    public Long next_cursor;

    public void ListAndCursorObject(ArrayList<Long>list, Long cursor){
        this.list = list;
        this.next_cursor = cursor;
    }
}

