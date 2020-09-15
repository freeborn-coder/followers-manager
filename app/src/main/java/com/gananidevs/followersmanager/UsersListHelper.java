package com.gananidevs.followersmanager;


import java.io.Serializable;

public interface UsersListHelper extends Serializable {

    UserItem getNextItem(int currentIndex);
    UserItem getPreviousItem(int currentIndex);

}
