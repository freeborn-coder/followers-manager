package com.gananidevs.followersmanager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.List;

public class RemoveAdsActivity extends AppCompatActivity implements PurchasesUpdatedListener, BillingClientStateListener, View.OnClickListener {

    Button threeMonthsBtn, sixMonthsBtn, twelveMonthsBtn;
    private static final String THREE_MONTHS_SKU = "remove_ads_3_months";
    private static final String SIX_MONTHS_SKU = "remove_ads_6_months";
    private static final String TWELVE_MONTHS_SKU = "remove_ads_12_months";
    private SkuDetails threeMonthsSkuDetails, sixMonthsSkuDetails, twelveMonthsSkuDetails;
    long THREE_MONTHS = 1000*86400*90;
    long SIX_MONTHS = 1000*86400*180;
    long TWELVE_MONTHS = 1000*86400*365;

    BillingClient billingClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remove_ads);

        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle(getString(R.string.remove_ads));

        threeMonthsBtn = findViewById(R.id.three_months_btn);
        sixMonthsBtn = findViewById(R.id.six_months_btn);
        twelveMonthsBtn = findViewById(R.id.twelve_months_btn);


        billingClient = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(this);

        threeMonthsBtn.setOnClickListener(this);
        sixMonthsBtn.setOnClickListener(this);
        twelveMonthsBtn.setOnClickListener(this);
    }


    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
            if (list != null) {
                for(Purchase purchase:list){
                    handlePurchase(purchase);
                }
            }
        }else if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED){
            Toast.makeText(this,"User canceled purchase",Toast.LENGTH_LONG).show();

        }else if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED){
            Toast.makeText(this,"Item is already owned",Toast.LENGTH_LONG).show();
            consumeProduct(list.get(0).getPurchaseToken());
            // consume the item
        }
    }

    private void handlePurchase(Purchase purchase) {
        // Consume the item, and give user the reward of the purchase
        consumeProduct(purchase.getPurchaseToken());

        long expiryDate = 0;
        String time = "";
        String purchaseSku = purchase.getSku();

        if(purchaseSku.equals(THREE_MONTHS_SKU)){

            expiryDate = System.currentTimeMillis() + THREE_MONTHS;
            time = "3 months";

        }else if(purchaseSku.equals(SIX_MONTHS_SKU)){

            expiryDate = System.currentTimeMillis() + SIX_MONTHS;
            time = "6 months";

        }else if(purchaseSku.equals(TWELVE_MONTHS_SKU)){

            expiryDate = System.currentTimeMillis() + TWELVE_MONTHS;
            time = "12 months";

        }

        Toast.makeText(this,"Purchase Complete. You will not be seeing any Ads for the next "+time,Toast.LENGTH_LONG).show();

        removeAdsTill(expiryDate);


    }

    private void removeAdsTill(long expiryDate) {
        MainActivity.sp.edit().putBoolean(Helper.ADS_REMOVAL_ACTIVE,true).apply();
        MainActivity.sp.edit().putLong(Helper.ADS_REMOVAL_EXPIRY_DATE,expiryDate).apply();
        MainActivity.adsRemovalActive = true;
        MainActivity.isShowingAds = false;
        // Set showingAds variable to false.
        // Set expiry date to expirydate and save to shared preferences. check it each time the user loads
        // Start main activity
    }

    private void consumeProduct(String tokenOfPurchase) {
        final ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(tokenOfPurchase)
                .build();

        ConsumeResponseListener listener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // Handle the success of the consume operation.
                }
            }
        };

        billingClient.consumeAsync(consumeParams, listener);
    }


    @Override
    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {

        if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
            ArrayList<String> skuList = new ArrayList<>();
            skuList.add(THREE_MONTHS_SKU);
            skuList.add(SIX_MONTHS_SKU);
            skuList.add(TWELVE_MONTHS_SKU);

            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            params.setSkusList(skuList);

            billingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
                @Override
                public void onSkuDetailsResponse(@NonNull BillingResult billingResult, @Nullable List<SkuDetails> list) {
                    if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null){
                        //list.get(0).getPrice()
                        String sku = null;
                        for(SkuDetails skuDetail: list){
                            sku = skuDetail.getSku();
                            if(sku.equals(THREE_MONTHS_SKU)){
                                threeMonthsBtn.setText(skuDetail.getSku());
                                threeMonthsSkuDetails = skuDetail;
                            }else if(sku.equals(SIX_MONTHS_SKU)){
                                sixMonthsBtn.setText(sku);
                                sixMonthsSkuDetails = skuDetail;
                            }else if(sku.equals(TWELVE_MONTHS_SKU)){
                                twelveMonthsBtn.setText(sku);
                                twelveMonthsSkuDetails = skuDetail;
                            }
                        }


                    }
                }
            });

        }

    }

    @Override
    public void onBillingServiceDisconnected() {
        billingClient.startConnection(this);
    }

    @Override
    public void onClick(View v) {
        SkuDetails skuDetail = null;
        if(v.getId() == R.id.three_months_btn){
            skuDetail = threeMonthsSkuDetails;
        }else if(v.getId() == R.id.six_months_btn){
            skuDetail = sixMonthsSkuDetails;
        }else if(v.getId() == R.id.twelve_months_btn){
            skuDetail = twelveMonthsSkuDetails;
        }

        if(skuDetail != null) {
            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetail)
                    .build();

            billingClient.launchBillingFlow(this,billingFlowParams);


        }else{
            Toast.makeText(RemoveAdsActivity.this,"SKuDetail is null!",Toast.LENGTH_LONG).show();
        }
    }
}
