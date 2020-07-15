package com.gananidevs.followersmanager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;

import java.util.Objects;

public class WelcomeScreenActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    TwitterAuthClient twitterAuthClient;
    private String apiKey;
    private String apiSecretKey;
    FirebaseRemoteConfig remoteConfig;
    static final String API_KEY = "twitter_api_key";
    static final String API_SECRET = "twitter_api_secret";
    private boolean isSignedIn = false;
    ProgressBar progressBar;

    int currentAppVersionCode = 2313101; // Remember to update or increment this whenever you update your app.
    int latestAppVersionCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout file
        setContentView(R.layout.activity_welcome_screen);

        progressBar = findViewById(R.id.progressbar);

        // Setup firebase remote config
        remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().build();
        remoteConfig.setConfigSettingsAsync(configSettings);

        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);

        try {

            fetchKeysFromConfig();
            assert getSupportActionBar() != null;
            getSupportActionBar().setTitle(getResources().getString(R.string.welcome));
            getSupportActionBar().setElevation(0f);

        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this,e.getMessage(), Toast.LENGTH_LONG).show();
        } // set the title bar text

        firebaseAuth = FirebaseAuth.getInstance();

        Button loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    if(!isSignedIn) {
                        if(apiKey != null && !apiKey.isEmpty() && apiSecretKey != null && !apiSecretKey.isEmpty()) {

                            twitterAuthClient = new TwitterAuthClient();
                            twitterAuthClient.authorize(WelcomeScreenActivity.this, new Callback<TwitterSession>() {
                                @Override
                                public void success(Result<TwitterSession> result) {
                                    Toast.makeText(WelcomeScreenActivity.this, "log in successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(WelcomeScreenActivity.this, MainActivity.class));
                                    finish();
                                }

                                @Override
                                public void failure(TwitterException exception) {
                                    Toast.makeText(WelcomeScreenActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                                    exception.printStackTrace();
                                    twitterAuthClient.cancelAuthorize();
                                }
                            });
                        }
                    }

                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    Toast.makeText(WelcomeScreenActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }

            }

        });

    }

    private void fetchKeysFromConfig() {

        remoteConfig.fetchAndActivate().addOnCompleteListener(new OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull Task<Boolean> task) {
                if(task.isSuccessful()){
                    initializeTwitter();
                }else{
                    progressBar.setVisibility(View.GONE);
                    if(BuildConfig.DEBUG){
                        Objects.requireNonNull(task.getException()).printStackTrace();
                        Toast.makeText(WelcomeScreenActivity.this,task.getException().getMessage(),Toast.LENGTH_LONG).show();
                    }
                }

            }
        });
    }

    private void initializeTwitter() {
        apiKey = remoteConfig.getString(API_KEY);
        apiSecretKey = remoteConfig.getString(API_SECRET);
        latestAppVersionCode = (int)remoteConfig.getLong("latest_app_version_code");

        if(latestAppVersionCode > currentAppVersionCode){
            askUserToUpdateApp();
        }else {
            setUpTwitterAndGoToMainActivity();
        }

    }

    private void setUpTwitterAndGoToMainActivity() {
        TwitterConfig config = new TwitterConfig.Builder(this)
                .logger(new DefaultLogger(Log.DEBUG))
                .twitterAuthConfig(new TwitterAuthConfig(apiKey, apiSecretKey))
                .debug(true)
                .build();
        Twitter.initialize(config);
        TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();

        if (session != null || firebaseAuth.getCurrentUser() != null) {
            isSignedIn = true;
            goToMainActivity();
        }else{
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this,getString(R.string.sign_in_to_continue),Toast.LENGTH_SHORT).show();
        }

    }

    private void askUserToUpdateApp() {
        final Dialog confirmDialog = new Dialog(this);
        View view = getLayoutInflater().inflate(R.layout.confirm_dialog_layout,null);
        TextView msgTv = view.findViewById(R.id.message_tv);
        msgTv.setText(getString(R.string.new_version_dialog_message));
        Button positiveBtn, negativeBtn;
        positiveBtn = view.findViewById(R.id.positive_btn);
        negativeBtn = view.findViewById(R.id.negative_btn);
        positiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.goToAppStore(WelcomeScreenActivity.this);
                confirmDialog.dismiss();
            }
        });

        negativeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDialog.dismiss();
            }
        });

        confirmDialog.setCancelable(false);
        confirmDialog.show();

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
    }

    private void goToMainActivity() {

        Toast.makeText(WelcomeScreenActivity.this, "You are logged in", Toast.LENGTH_LONG).show();

        //Sending user to main activity screen after successful login
        Intent intent = new Intent(WelcomeScreenActivity.this, MainActivity.class);
        startActivity(intent);
        finish();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(latestAppVersionCode > currentAppVersionCode){
            askUserToUpdateApp();
        }else if(apiSecretKey != null && apiKey != null){
            if(!apiKey.isEmpty() && !apiSecretKey.isEmpty()){
                setUpTwitterAndGoToMainActivity();
            }
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}
