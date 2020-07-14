package com.gananidevs.followersmanager;

import android.os.Parcel;
import android.os.Parcelable;

import com.twitter.sdk.android.core.models.User;

import org.json.JSONObject;

public class UserItem implements Parcelable {

    String profileImageUrlHttps;
    String idStr;
    long id;
    String description;
    String createdAt;
    int followersCount;
    int friendsCount;
    String location;
    String name;
    String screenName;
    boolean isVerified = false;
    String url;
    boolean followbackRequested;
    boolean isSuspendedUser = false;

    public UserItem(){}

    public UserItem(String name, boolean isSuspendedUser){
        this.name = name;
        this.isSuspendedUser = isSuspendedUser;
    }

    public UserItem(User user){
        profileImageUrlHttps = user.profileImageUrlHttps.replace("_normal","");
        idStr = user.idStr;
        id = user.id;
        createdAt = user.createdAt;
        description = user.description;
        followersCount = user.followersCount;
        friendsCount = user.friendsCount;
        location = user.location;
        name = user.name;
        screenName = user.screenName;
        isVerified = user.verified;
        url = user.url;
        isSuspendedUser = false;
    }

    // create a user item from json object
    public UserItem(JSONObject object){
        try {
            profileImageUrlHttps = object.getString("profile_image_url_https");
            idStr = object.getString("id_str");
            id = object.getLong("id");
            createdAt = object.getString("created_at");
            description = object.getString("description");
            followersCount = object.getInt("followers_count");
            friendsCount = object.getInt("friends_count");
            location = object.getString("location");
            name = object.getString("name");
            screenName = object.getString("screen_name");
            isVerified = object.getBoolean("verified");
            url = object.getString("url");
            url = (url == null || url.isEmpty() || url.equals("null"))? "":url;
            description = (description == null || description.isEmpty())? "":description;
        }catch (Exception e){e.printStackTrace();}
        isSuspendedUser = false;
    }

    protected UserItem(Parcel in) {
        profileImageUrlHttps = in.readString();
        idStr = in.readString();
        id = in.readLong();
        description = in.readString();
        createdAt = in.readString();
        followersCount = in.readInt();
        friendsCount = in.readInt();
        location = in.readString();
        name = in.readString();
        screenName = in.readString();
        isVerified = in.readByte() != 0;
        url = in.readString();
        followbackRequested = in.readByte() != 0;
        isSuspendedUser = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(profileImageUrlHttps);
        dest.writeString(idStr);
        dest.writeLong(id);
        dest.writeString(description);
        dest.writeString(createdAt);
        dest.writeInt(followersCount);
        dest.writeInt(friendsCount);
        dest.writeString(location);
        dest.writeString(name);
        dest.writeString(screenName);
        dest.writeByte((byte) (isVerified ? 1 : 0));
        dest.writeString(url);
        dest.writeByte((byte) (followbackRequested ? 1 : 0));
        dest.writeByte((byte) (isSuspendedUser ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<UserItem> CREATOR = new Creator<UserItem>() {
        @Override
        public UserItem createFromParcel(Parcel in) {
            return new UserItem(in);
        }

        @Override
        public UserItem[] newArray(int size) {
            return new UserItem[size];
        }
    };
}
