package com.gananidevs.followersmanager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static com.gananidevs.followersmanager.Helper.DATABASE_URL;

public class FirebaseUtil {

    private static FirebaseUtil fbUtil;
    private static FirebaseDatabase firebaseDatabase;
    private FirebaseUtil(){}

    public static DatabaseReference openDbReference(String ref){
        if(fbUtil == null){
            fbUtil = new FirebaseUtil();
            firebaseDatabase = FirebaseDatabase.getInstance(DATABASE_URL);
        }
        return firebaseDatabase.getReference().child(ref);
    }
}
