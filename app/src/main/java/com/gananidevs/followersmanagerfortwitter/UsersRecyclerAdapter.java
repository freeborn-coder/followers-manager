package com.gananidevs.followersmanagerfortwitter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class UsersRecyclerAdapter extends RecyclerView.Adapter<UsersViewHolder> {

    ArrayList<UserItem> userItemsList;
    Context context;

    public UsersRecyclerAdapter(Context context,ArrayList<UserItem> userItems) {
        this.userItemsList = userItems;
        this.context = context;
    }

    @NonNull
    @Override
    public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item_layout,null,false);
        return new UsersViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersViewHolder holder, int position) {
        UserItem userItem = userItemsList.get(position);

        try{
            Glide.with(context).load(userItem.profileImageUrlHttps).into(holder.profileImage);
        }catch (Exception e){
            e.printStackTrace();
        }
        holder.nameTv.setText(userItem.name);
        holder.screenNameTv.setText(userItem.screenName);
    }

    @Override
    public int getItemCount() {
        return userItemsList.size();
    }
}

class UsersViewHolder extends RecyclerView.ViewHolder{

    public final ImageView profileImage;
    public final Button button;
    public final TextView nameTv;
    public final TextView screenNameTv;

    public UsersViewHolder(@NonNull View itemView) {
        super(itemView);
        profileImage =  itemView.findViewById(R.id.profile_image);
        button = itemView.findViewById(R.id.follow_unfollow_button);
        nameTv = itemView.findViewById(R.id.name_tv);
        screenNameTv = itemView.findViewById(R.id.screen_name_tv);
    }
}
