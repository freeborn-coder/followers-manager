package com.gananidevs.followersmanager;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;

import java.util.ArrayList;
import java.util.Random;

import okhttp3.ResponseBody;

import static com.gananidevs.followersmanager.Helper.CURRENT_USER_INDEX;
import static com.gananidevs.followersmanager.Helper.DATABASE_URL;
import static com.gananidevs.followersmanager.Helper.USERS_PARCELABLE_ARRAYLIST;
import static com.gananidevs.followersmanager.Helper.changeButtonTextAndColor;
import static com.gananidevs.followersmanager.Helper.checkWhetherToAskUserToRateApp;
import static com.gananidevs.followersmanager.Helper.getMinutesLeft;
import static com.gananidevs.followersmanager.Helper.incrementApiRequestCount;
import static com.gananidevs.followersmanager.Helper.proceedWithApiCall;
import static com.gananidevs.followersmanager.Helper.setFollowBtnAppearance;
import static com.gananidevs.followersmanager.Helper.showSnackBar;

public class UsersRecyclerAdapter extends RecyclerView.Adapter<UsersRecyclerAdapter.UsersViewHolder> {

    ArrayList<UserItem> userItemsList;
    Context context;
    boolean isApiRequestActive = false;
    public boolean isViewingWhitelist = false, showDropdown = false;
    private final TwitterSession activeSession;
    MyTwitterApiClient twitterApiClient;
    private BottomSheetDialog bottomSheetDialog;

    public UsersRecyclerAdapter(Context context,ArrayList<UserItem> userItems) {
        this.userItemsList = userItems;
        this.context = context;
        activeSession = TwitterCore.getInstance().getSessionManager().getActiveSession();
        twitterApiClient = new MyTwitterApiClient(activeSession);
        bottomSheetDialog = new BottomSheetDialog(context);
    }

    @NonNull
    @Override
    public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item_layout,null,false);

        ImageView dropdownArrow = view.findViewById(R.id.dropDownArrow);
        if(showDropdown){
            dropdownArrow.setVisibility(View.VISIBLE);
        }else{
            dropdownArrow.setVisibility(View.GONE);
        }

        if(isViewingWhitelist){
            MaterialButton followUnfollowBtn = view.findViewById(R.id.follow_unfollow_button);
            followUnfollowBtn.setText(context.getString(R.string.remove));
        }
        return new UsersViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersViewHolder holder, int position) {
        UserItem userItem = userItemsList.get(position);

        // Check if a user is verified
        if(userItem.isVerified){
            holder.verifiedIcon.setVisibility(View.VISIBLE);
        }else{
            holder.verifiedIcon.setVisibility(View.GONE);
        }

        // check if user is a follower
        if(Helper.isFollower(userItem.id)){
            Helper.changeButtonTextAndColor(context,holder.followStatusButton,R.string.follows_you,R.color.green);
        }else{
            Helper.changeButtonTextAndColor(context,holder.followStatusButton,R.string.doesnt_follow_you,R.color.mid_gray_777);
        }

        if (!isViewingWhitelist) {
            setFollowBtnAppearance(context, userItem.id, holder.followUnfollowButton);
        }

        if(userItem.id == activeSession.getUserId()){
            holder.followStatusButton.setVisibility(View.GONE);
            holder.followUnfollowButton.setVisibility(View.GONE);
        }

        holder.nameTv.setText(userItem.name);

        if(userItem.isSuspendedUser){
            Glide.with(context).load(R.drawable.suspended_user).into(holder.profileImage);
            holder.profileImage.setClickable(false);
            holder.followUnfollowButton.setVisibility(View.GONE);
            holder.followStatusButton.setVisibility(View.INVISIBLE);
            holder.dropDownArrow.setVisibility(View.GONE);
            holder.screenNameTv.setText(R.string.suspended_user);
            holder.screenNameTv.setTextColor(context.getColor(R.color.colorAccent));
        }else {
            try {
                Glide.with(context).load(userItem.profileImageUrlHttps).into(holder.profileImage);
            } catch (Exception e) {
                e.printStackTrace();
            }

            String screenName = "@" + userItem.screenName;
            holder.screenNameTv.setText(screenName);
        }

        if(Helper.isWhielisted(userItem.id)){
            holder.whitelistStatusTv.setVisibility(View.VISIBLE);
        }else{
            holder.whitelistStatusTv.setVisibility(View.GONE);
        }


    }

    @Override
    public int getItemCount() {
        return userItemsList.size();
    }

    class UsersViewHolder extends RecyclerView.ViewHolder{

        public final ImageView profileImage, verifiedIcon, dropDownArrow;
        public final MaterialButton followUnfollowButton, followStatusButton;
        public final TextView nameTv;
        public final TextView screenNameTv, whitelistStatusTv;
        public final ProgressBar btnProgressBar;

        public UsersViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage =  itemView.findViewById(R.id.profile_image);
            followUnfollowButton = itemView.findViewById(R.id.follow_unfollow_button);
            followStatusButton = itemView.findViewById(R.id.follow_status_button);
            nameTv = itemView.findViewById(R.id.name_tv);
            screenNameTv = itemView.findViewById(R.id.screen_name_tv);
            verifiedIcon = itemView.findViewById(R.id.verified_icon);
            btnProgressBar = itemView.findViewById(R.id.btn_progress_bar);
            dropDownArrow = itemView.findViewById(R.id.dropDownArrow);
            whitelistStatusTv = itemView.findViewById(R.id.whitelisted_status_tv);

            ClickListener listener = new ClickListener();

            nameTv.setOnClickListener(listener);
            screenNameTv.setOnClickListener(listener);
            profileImage.setOnClickListener(listener);
            followUnfollowButton.setOnClickListener(listener);
            dropDownArrow.setOnClickListener(listener);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!userItemsList.get(getAdapterPosition()).isSuspendedUser) {
                        viewProfile(context, getAdapterPosition());
                    }
                }
            });

        }

        class ClickListener implements View.OnClickListener{

            @Override
            public void onClick(View v) {
                int viewId = v.getId();

                final UserItem currentUserItem = userItemsList.get(getAdapterPosition());
                final Long userId = currentUserItem.id;
                final String userScreenName = currentUserItem.screenName;

                if(viewId == nameTv.getId() || viewId == screenNameTv.getId() || viewId == profileImage.getId()){
                    if(!currentUserItem.isSuspendedUser){
                        viewProfile(context,getAdapterPosition());
                    }

                }else if(viewId == followUnfollowButton.getId()){

                    String btnText = followUnfollowButton.getText().toString();

                    // to remove a user from whitelist does not need to check api request, cos that does not need twitter
                    if(btnText.equals(context.getString(R.string.remove))){
                        showProgressHideButtonText(btnProgressBar, followUnfollowButton, context);
                        // remove the specified user from whitelist
                        removeFromWhitelist(userId,userScreenName);

                    }else if(!isApiRequestActive){  // follow or unfollow user

                        //showProgressHideButtonText(btnProgressBar, followUnfollowButton, context);
                        long interval = System.currentTimeMillis() - MainActivity.last15MinTimeStamp;

                        if (proceedWithApiCall(interval)) {
                            // follow or unfollow users as neccessary

                            if (btnText.equals(context.getString(R.string.follow_all_lowercase))) {

                                // follow the specific user
                                isApiRequestActive = true;
                                showProgressHideButtonText(btnProgressBar, followUnfollowButton, context);

                                int delay = new Random().nextInt(1000) + 1000;
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        followUser(userId);
                                    }
                                }, delay);

                            } else if (btnText.equals(context.getString(R.string.unfollow_all_lowercase))) {

                                // unfollow the specified user
                                final Dialog confirmDialog = new Dialog(context);
                                View dialogView = LayoutInflater.from(context).inflate(R.layout.confirm_dialog_layout,null,false);
                                TextView confirmMessage = dialogView.findViewById(R.id.message_tv);
                                confirmMessage.setText("you want to unfollow "+userScreenName+"?");
                                Button positiveBtn = dialogView.findViewById(R.id.positive_btn);
                                positiveBtn.setText(R.string.yes);
                                positiveBtn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        confirmDialog.dismiss();
                                        isApiRequestActive = true;
                                        showProgressHideButtonText(btnProgressBar, followUnfollowButton, context);

                                        int delay = new Random().nextInt(1000) + 1500;
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                unfollowUser(userId);
                                            }
                                        }, delay);
                                    }
                                });

                                Button negativeBtn = dialogView.findViewById(R.id.negative_btn);
                                negativeBtn.setText(R.string.cancel);
                                negativeBtn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        confirmDialog.dismiss();
                                    }
                                });
                                confirmDialog.setContentView(dialogView);
                                confirmDialog.show();

                                /*
                                new AlertDialog.Builder(context).setTitle("confirm action")
                                    .setMessage(context.getString(R.string.unfollow_all_lowercase) + " " + userScreenName + "?")
                                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            isApiRequestActive = true;
                                            showProgressHideButtonText(btnProgressBar, followUnfollowButton, context);

                                            int delay = new Random().nextInt(1000) + 1500;
                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    unfollowUser(userId);
                                                }
                                            }, delay);

                                        }
                                    }).setNegativeButton(context.getString(R.string.cancel), null)
                                    .show();

                                 */
                            }

                        } else { // request limit has been reached, so show appropriate message

                            if(btnText.equals(context.getString(R.string.unfollow_all_lowercase))){
                                showDialogAndFeignWork(userScreenName);
                            }else{
                                placeboAction();
                            }

                        }

                    } // api request is currently active endif

                }else if(viewId == dropDownArrow.getId()){ // if dropdown arrow was clicked
                    final String REQUEST_FOLLOW_BACK = context.getString(R.string.request_follow_back);
                    final String ADD_TO_WHITELIST = context.getString(R.string.add_to_whitelist);
                    PopupMenu popupMenu = new PopupMenu(context,v);
                    popupMenu.getMenu().add(REQUEST_FOLLOW_BACK);
                    popupMenu.getMenu().add(ADD_TO_WHITELIST);

                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if(item.getTitle() == ADD_TO_WHITELIST){

                                // whitelist the particular user
                                showProgressHideButtonText(btnProgressBar, followUnfollowButton, context);
                                whitelistUser(activeSession.getUserId(),String.valueOf(currentUserItem.id),currentUserItem.screenName);

                            }else if(item.getTitle() == REQUEST_FOLLOW_BACK){
                                Helper.showRequestFollowBackBottomSheetDialog(context,currentUserItem.screenName,bottomSheetDialog,twitterApiClient,UsersListActivity.progressBar);
                            }
                        return true;
                        }
                    });
                    popupMenu.show();

                }

            }

            private void followUser(final Long userId) {
                twitterApiClient.getFriendshipsCreateCustomService().post(userId).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void success(Result<ResponseBody> result) {
                        hideProgressShowButtonText(btnProgressBar,followUnfollowButton,context);
                        followUnfollowButton.setText(context.getString(R.string.unfollow_all_lowercase));
                        changeButtonTextAndColor(context,followUnfollowButton,R.string.unfollow_all_lowercase,R.color.colorAccent);
                        MainActivity.friendsIdsList.add(userId);
                        isApiRequestActive = false;
                        incrementApiRequestCount(context);

                    }

                    @Override
                    public void failure(TwitterException exception) {
                        hideProgressShowButtonText(btnProgressBar,followUnfollowButton,context);
                        Toast.makeText(context,exception.getMessage(),Toast.LENGTH_SHORT).show();
                        isApiRequestActive = false;
                    }
                });
            }

            private void unfollowUser(final Long userId){

                twitterApiClient.getFriendshipsDestroyCustomService().post(userId).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void success(Result<ResponseBody> result) {
                        hideProgressShowButtonText(btnProgressBar,followUnfollowButton,context);
                        followUnfollowButton.setText(context.getString(R.string.unfollow_all_lowercase));
                        changeButtonTextAndColor(context,followUnfollowButton,R.string.follow_all_lowercase,R.color.colorPrimary);
                        MainActivity.friendsIdsList.remove(userId);
                        isApiRequestActive = false;
                        incrementApiRequestCount(context);
                    }

                    @Override
                    public void failure(TwitterException exception) {
                        hideProgressShowButtonText(btnProgressBar,followUnfollowButton,context);
                        Toast.makeText(context,exception.getMessage(),Toast.LENGTH_SHORT).show();
                        isApiRequestActive = false;
                    }
                });
            }
        }


        private void showDialogAndFeignWork(String screenName) {
            // unfollow the specified user
            new AlertDialog.Builder(context).setTitle(context.getString(R.string.confirm_action))
                    .setMessage(context.getString(R.string.unfollow_all_lowercase) + " " + screenName + "?")
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        placeboAction();

                        }
                    }).setNegativeButton(context.getString(R.string.cancel), null)
                    .show();
        }

        private void placeboAction() {
            showProgressHideButtonText(btnProgressBar, followUnfollowButton, context);
            int delay = new Random().nextInt(1000) + 1000;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    int minutesLeft = getMinutesLeft(MainActivity.last15MinTimeStamp);
                    showSnackBar(UsersListActivity.constraintLayout, minutesLeft);
                    hideProgressShowButtonText(btnProgressBar, followUnfollowButton, context);
                }
            }, delay);
        }


        private void removeFromWhitelist(final Long userIdToRemove, final String screenName) {
            // remove the speicified user id from whitelisted users of the signed in user
            FirebaseDatabase fbDb = FirebaseDatabase.getInstance(DATABASE_URL);
            DatabaseReference dbRef = fbDb.getReference().child("whitelist/" + activeSession.getUserId()+"/"+userIdToRemove);

            dbRef.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if(task.isSuccessful()){
                        hideProgressShowButtonText(btnProgressBar,followUnfollowButton,context);
                        removeCurrentItem();
                        ++MainActivity.keyActionsCount;
                        Toast.makeText(context,screenName+" removed successfully",Toast.LENGTH_SHORT).show();

                        if(MainActivity.remindUserToRateApp){
                            checkWhetherToAskUserToRateApp(context);
                        }

                    }else{
                        hideProgressShowButtonText(btnProgressBar,followUnfollowButton,context);
                        Toast.makeText(context,task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }

        private void whitelistUser(Long signedInUserId, final String whitelistedUserId, final String whitelistedUserName) {

            DatabaseReference fbDbRef = FirebaseUtil.openDbReference("whitelist/"+signedInUserId+"/"+whitelistedUserId);

            fbDbRef.setValue(whitelistedUserName).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        hideProgressShowButtonText(btnProgressBar,followUnfollowButton,context);
                        removeCurrentItem();
                        MainActivity.nonFollowersIdsList.remove(Long.valueOf(whitelistedUserId));
                        Toast.makeText(context,whitelistedUserName+" added to whitelist",Toast.LENGTH_SHORT).show();
                        ++MainActivity.keyActionsCount;
                        if(MainActivity.remindUserToRateApp){
                            checkWhetherToAskUserToRateApp(context);
                        }

                    }else{
                        Toast.makeText(context,task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                        hideProgressShowButtonText(btnProgressBar,followUnfollowButton,context);
                    }
                }
            });
        }


        private void viewProfile(Context ctx, int userIndex) {
            Intent intent = new Intent(ctx,UserProfileActivity.class);
            intent.putParcelableArrayListExtra(USERS_PARCELABLE_ARRAYLIST,userItemsList);
            intent.putExtra(CURRENT_USER_INDEX,userIndex);
            ctx.startActivity(intent);
        }

        public void removeCurrentItem(){
            int currentPos = getAdapterPosition();
            userItemsList.remove(userItemsList.get(currentPos));
            notifyItemRemoved(currentPos);
        }


    }


    public static void showProgressHideButtonText(ProgressBar progressBar, Button btn,Context context){
        btn.setTextColor(context.getColor(android.R.color.transparent));
        progressBar.setVisibility(View.VISIBLE);
    }

    public static void hideProgressShowButtonText(ProgressBar progressBar, Button btn,Context context){
        btn.setTextColor(context.getColor(R.color.tw__solid_white));
        progressBar.setVisibility(View.GONE);
    }
}
