package com.gananidevs.followersmanagerfortwitter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.TwitterAuthProvider;

import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterSession;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Get active twitter session
        TwitterSession activeSession = TwitterCore.getInstance().getSessionManager().getActiveSession();

        // Sign in to firebase with twitter session if there is no active firebase session
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if(firebaseUser == null) {
            signIntoFirebaseUsingTwitter(activeSession);
        }

        setUpUI(activeSession);

    }

    private void setUpUI(TwitterSession activeSession) {
        Long userId = activeSession.getUserId();

        // Get user data from api using user id

    }

    /*
     * Sign in to firebase using the currently active twitter session token or object
     */
    private void signIntoFirebaseUsingTwitter(TwitterSession session){
        AuthCredential credential = TwitterAuthProvider.getCredential(session.getAuthToken().token, session.getAuthToken().secret);

        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if(!task.isSuccessful()){
                    task.getException().printStackTrace();
                    Log.i("Firebase:","Sign in not successful");
                }
            }

        });

    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Sign out before exit?")
                .setPositiveButton("yes", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FirebaseAuth.getInstance().signOut();
                        TwitterCore.getInstance().getSessionManager().clearActiveSession();
                        finish();
                    }
                }).setNegativeButton("no", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        }).show();


    }
}