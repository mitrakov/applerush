package ru.mitrakov.self.rush;

import java.util.*;
import java.util.concurrent.*;

import android.app.Activity;

import com.android.billingclient.api.*;

import static com.android.billingclient.api.BillingClient.SkuType.*;
import static com.android.billingclient.api.BillingClient.BillingResponse.*;
import static ru.mitrakov.self.rush.utils.SimpleLogger.log;

/**
 * Billing Provider for Google Play Market
 */
class GooglePlayBillingProvider implements IBillingProvider, PurchasesUpdatedListener {
    private final Activity activity;
    private final BillingClient client;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Sku> products = new ArrayList<>(3);
    private final Object productsLocker = new Object();
    private /*final*/ BillingListener listener;

    GooglePlayBillingProvider(Activity activity) {
        assert activity != null;
        this.activity = activity;
        client = new BillingClient.Builder(activity).setListener(this).build();
    }

    @Override
    public void startService(BillingListener listener) {
        assert listener != null;
        this.listener = listener;
        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(int resultCode) {
                if (resultCode == OK) {
                    // 1. Query all SKU details (in a SEPARATE thread!)
                    executor.submit(new Runnable() {
                        @Override
                        public void run() {
                            client.querySkuDetailsAsync(INAPP, SKU_LIST, new SkuDetailsResponseListener() {
                                @Override
                                public void onSkuDetailsResponse(SkuDetails.SkuDetailsResult result) {
                                    if (result.getResponseCode() == OK) {
                                        synchronized (productsLocker) { // just in case (Async method on outside API)
                                            products.clear();
                                            for (SkuDetails d : result.getSkuDetailsList()) {
                                                products.add(new Sku(d.getSku(), d.getDescription(), d.getPrice()));
                                            }
                                        }
                                    } else log("Sku details error! Code:", result.getResponseCode());
                                }
                            });
                        }
                    });
                    // 2. Consume possibly outstanding purchases (recommendation from Google)
                    // @see: https://developer.android.com/google/play/billing/api.html#managingconsumables
                    Purchase.PurchasesResult result = client.queryPurchases(INAPP);
                    onPurchasesUpdated(result.getResponseCode(), result.getPurchasesList());
                } else log("Billing connection error! Code:", resultCode);
            }

            @Override
            public void onBillingServiceDisconnected() {
                log("Billing", "Disconnected");
            }
        });
    }

    @Override
    public List<Sku> getProducts() {
        return products;
    }

    @Override
    public void purchaseProduct(Sku sku, String payload) {
        if (client.isReady()) {
            BillingFlowParams params = new BillingFlowParams.Builder()
                    .setSku(sku.id).setType(INAPP).setAccountId(payload).build();
            client.launchBillingFlow(activity, params);
        } else log("", "Billing system is not ready!");
    }

    @Override
    public void onPurchasesUpdated(int responseCode, List<Purchase> purchases) {
        assert listener != null;
        if (responseCode == OK) {
            for (final Purchase purchase : purchases) {
                log("OrigJSON: ", purchase.getOriginalJson());
                log("Signature: ", purchase.getSignature());
                client.consumeAsync(purchase.getPurchaseToken(), new ConsumeResponseListener() {
                    @Override
                    public void onConsumeResponse(String purchaseToken, int resultCode) {
                        log("Consuming done; code = ", resultCode);
                        listener.onResponse(purchase.getOriginalJson(), purchase.getSignature());
                    }
                });
            }
        }
    }
}
