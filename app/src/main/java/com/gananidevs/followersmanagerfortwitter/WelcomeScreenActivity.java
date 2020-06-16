package com.gananidevs.followersmanagerfortwitter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;

public class WelcomeScreenActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    TwitterAuthClient twitterAuthClient;
    private String consumerKey;
    private String consumerSecret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        consumerKey = getString(R.string.CONSUMER_KEY);
        consumerSecret = getString(R.string.CONSUMER_SECRET);

        TwitterConfig config = new TwitterConfig.Builder(this)
                .logger(new DefaultLogger(Log.DEBUG))
                .twitterAuthConfig(new TwitterAuthConfig(consumerKey, consumerSecret))
                .debug(true)
                .build();
        Twitter.initialize(config);
        twitterAuthClient = new TwitterAuthClient();

        // Set the layout file
        setContentView(R.layout.activity_welcome_screen);

        try { getSupportActionBar().setTitle(getResources().getString(R.string.welcome)); }catch (Exception e){ e.printStackTrace(); } // set the title bar text

        firebaseAuth = FirebaseAuth.getInstance();

        Button loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                twitterAuthClient = new TwitterAuthClient();
                twitterAuthClient.authorize(WelcomeScreenActivity.this, new Callback<TwitterSession>() {
                    @Override
                    public void success(Result<TwitterSession> result) {
                        Toast.makeText(WelcomeScreenActivity.this,"log in successful",Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(WelcomeScreenActivity.this, MainActivity.class));
                        finish();

                    }

                    @Override
                    public void failure(TwitterException exception) {
                        Toast.makeText(WelcomeScreenActivity.this,exception.getMessage(),Toast.LENGTH_SHORT).show();
                        exception.printStackTrace();
                        twitterAuthClient.cancelAuthorize();
                    }
                });
            }

        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (twitterAuthClient != null && data != null) {
            twitterAuthClient.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            goToMainActivity();
        }

    }

    private void goToMainActivity() {

        Toast.makeText(WelcomeScreenActivity.this, "You are logged in", Toast.LENGTH_LONG).show();

        //Sending user to main activity screen after successful login
        Intent intent = new Intent(WelcomeScreenActivity.this, MainActivity.class);
        startActivity(intent);
        finish();

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }


}
