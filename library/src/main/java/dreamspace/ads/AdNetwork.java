package dreamspace.ads;

import static com.facebook.ads.AdSettings.IntegrationErrorMode.INTEGRATION_ERROR_CALLBACK_MODE;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.InterstitialAdListener;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import dreamspace.ads.data.AdNetworkType;
import dreamspace.ads.data.SharedPref;
import dreamspace.ads.gdpr.LegacyGDPR;
import dreamspace.ads.listener.ActivityListener;
import dreamspace.ads.listener.AdOpenListener;

public class AdNetwork {

    private static final String TAG = AdNetwork.class.getSimpleName();

    private final Activity activity;
    private final SharedPref sharedPref;
    private static int last_interstitial_index = 0;
    private static int banner_retry_from_start = 0;
    private static int interstitial_retry_from_start = 0;
    private static long openApploadTime = 0;

    //Interstitial
    private InterstitialAd adMobInterstitialAd;
    private com.facebook.ads.InterstitialAd fanInterstitialAd;

    // Open app admob
    public static AppOpenAd appOpenAd = null;
    private static boolean appOpenAdLoading = false;
    private static ActivityListener activityListener = null;
    private static List<AdNetworkType> ad_networks = new ArrayList<>();

    public AdNetwork(Activity activity) {
        this.activity = activity;
        sharedPref = new SharedPref(activity);
        //activityListener = new ActivityListener(activity.getApplication());
    }

    public static void initActivityListener(Application application){
        activityListener = new ActivityListener(application);
    }

    public void init() {
        if (!AdConfig.ad_enable) return;

        // check if using single networks
        if (AdConfig.ad_networks.length == 0) {
            AdConfig.ad_networks = new AdNetworkType[]{
                    AdConfig.ad_network
            };
        }

       ad_networks = Arrays.asList(AdConfig.ad_networks);
        // init admob
        if (ad_networks.contains(AdNetworkType.ADMOB)) {
            Log.d(TAG, "ADMOB init");
            MobileAds.initialize(this.activity);
        }

        // init fan
        if (ad_networks.contains(AdNetworkType.FAN)) {
            Log.d(TAG, "FAN init");
            AudienceNetworkAds.initialize(this.activity);
            AdSettings.setIntegrationErrorMode(INTEGRATION_ERROR_CALLBACK_MODE);
        }

        // save to shared pref
        sharedPref.setOpenAppUnitId(AdConfig.ad_admob_open_app_unit_id);
    }

    public static void init(Context context) {
        if (!AdConfig.ad_enable) return;

        // check if using single networks
        if (AdConfig.ad_networks.length == 0) {
            AdConfig.ad_networks = new AdNetworkType[]{
                    AdConfig.ad_network
            };
        }

        ad_networks = Arrays.asList(AdConfig.ad_networks);
        // init admob
        if (ad_networks.contains(AdNetworkType.ADMOB)) {
            Log.d(TAG, "ADMOB init");
            MobileAds.initialize(context);
        }

        // init fan
        if (ad_networks.contains(AdNetworkType.FAN)) {
            Log.d(TAG, "FAN init");
            AudienceNetworkAds.initialize(context);
            AdSettings.setIntegrationErrorMode(INTEGRATION_ERROR_CALLBACK_MODE);
        }
        // save to shared pref
        new SharedPref(context).setOpenAppUnitId(AdConfig.ad_admob_open_app_unit_id);
    }

    public void loadBannerAd(boolean enable, LinearLayout ad_container) {
        banner_retry_from_start = 0;
        loadBannerAdMain(enable, 0, 0, ad_container);
    }

    private void loadBannerAdMain(boolean enable, int ad_index, int retry_count, LinearLayout ad_container) {
        if (!AdConfig.ad_enable || !enable) return;

        // check if index reach end
        if (ad_index >= AdConfig.ad_networks.length - 1 && retry_count >= AdConfig.retry_every_ad_networks - 1) {
            // check if retry from start enabled
            if (AdConfig.retry_from_start && banner_retry_from_start < AdConfig.retry_from_start_max) {
                banner_retry_from_start++;
                ad_index = 0;
                retry_count = 0;
            } else {
                return;
            }
        }

        retry_count = retry_count + 1;
        // when retry reach continue next ad network
        if (retry_count >= AdConfig.retry_every_ad_networks) {
            retry_count = 0;
            ad_index = ad_index + 1;
        }

        int finalRetry = retry_count;
        if (ad_index >= AdConfig.ad_networks.length) ad_index = 0;
        int finalIndex = ad_index;

        ad_container.setVisibility(View.GONE);
        ad_container.removeAllViews();

        ad_container.post(() -> {
            if (AdConfig.ad_networks[finalIndex] == AdNetworkType.ADMOB) {
                AdRequest adRequest = new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, LegacyGDPR.getBundleAd(activity)).build();
                AdView adView = new AdView(activity);
                adView.setAdUnitId(AdConfig.ad_admob_banner_unit_id);
                ad_container.addView(adView);
                adView.setAdSize(getAdmobBannerSize());
                adView.loadAd(adRequest);
                adView.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        ad_container.setVisibility(View.VISIBLE);
                        Log.d(TAG, "ADMOB banner onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        ad_container.setVisibility(View.GONE);
                        Log.d(TAG, "ADMOB banner onAdFailedToLoad : " + adError.getMessage());
                        delayAndLoadBanner(true, finalIndex, finalRetry, ad_container);
                    }
                });
            } else if (AdConfig.ad_networks[finalIndex] == AdNetworkType.FAN) {
                com.facebook.ads.AdView adView = new com.facebook.ads.AdView(activity, AdConfig.ad_fan_banner_unit_id, com.facebook.ads.AdSize.BANNER_HEIGHT_50);
                // Add the ad view to your activity layout
                ad_container.addView(adView);
                com.facebook.ads.AdListener adListener = new com.facebook.ads.AdListener() {
                    @Override
                    public void onError(Ad ad, AdError adError) {
                        ad_container.setVisibility(View.GONE);
                        Log.d(TAG, "FAN banner onAdFailedToLoad : " + adError.getErrorMessage());
                        delayAndLoadBanner(true, finalIndex, finalRetry, ad_container);
                    }

                    @Override
                    public void onAdLoaded(Ad ad) {
                        ad_container.setVisibility(View.VISIBLE);
                        Log.d(TAG, "FAN banner onAdLoaded");
                    }

                    @Override
                    public void onAdClicked(Ad ad) {

                    }

                    @Override
                    public void onLoggingImpression(Ad ad) {

                    }
                };
                com.facebook.ads.AdView.AdViewLoadConfig loadAdConfig = adView.buildLoadAdConfig().withAdListener(adListener).build();
                adView.loadAd(loadAdConfig);

            }
        });
    }

    private void delayAndLoadBanner(boolean enable, int ad_index, int retry_count, LinearLayout ad_container) {
        Log.d(TAG, "delayAndLoadBanner ad_index : " + ad_index + " retry_count : " + retry_count);
        new Handler(activity.getMainLooper()).postDelayed(() -> {
            loadBannerAdMain(enable, ad_index, retry_count, ad_container);
        }, 1500);
    }

    public void loadInterstitialAd(boolean enable) {
        interstitial_retry_from_start = 0;
        loadInterstitialAd(enable, 0, 0);
    }

    private void loadInterstitialAd(boolean enable, int ad_index, int retry_count) {
        if (!AdConfig.ad_enable || !enable) return;

        // check if index reach end
        if (ad_index >= AdConfig.ad_networks.length - 1 && retry_count >= AdConfig.retry_every_ad_networks - 1) {
            // check if retry from start enabled
            if (AdConfig.retry_from_start && interstitial_retry_from_start < AdConfig.retry_from_start_max) {
                interstitial_retry_from_start++;
                ad_index = 0;
                retry_count = 0;
            } else {
                return;
            }
        }

        last_interstitial_index = ad_index;
        retry_count = retry_count + 1;
        if (retry_count >= AdConfig.retry_every_ad_networks) {
            retry_count = 0;
            ad_index = ad_index + 1;
        }

        int finalRetry = retry_count;
        if (ad_index >= AdConfig.ad_networks.length) ad_index = 0;
        int finalIndex = ad_index;

        if (AdConfig.ad_networks[finalIndex] == AdNetworkType.ADMOB) {
            InterstitialAd.load(activity, AdConfig.ad_admob_interstitial_unit_id, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    adMobInterstitialAd = interstitialAd;
                    Log.i(TAG, "ADMOB interstitial onAdLoaded");
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    adMobInterstitialAd = null;
                    Log.i(TAG, "ADMOB interstitial onAdFailedToLoad");
                    delayAndInterstitial(true, finalIndex, finalRetry);
                }
            });
        } else if (AdConfig.ad_networks[finalIndex] == AdNetworkType.FAN) {
            fanInterstitialAd = new com.facebook.ads.InterstitialAd(activity, AdConfig.ad_fan_interstitial_unit_id);
            InterstitialAdListener interstitialAdListener = new InterstitialAdListener() {
                @Override
                public void onInterstitialDisplayed(Ad ad) {

                }

                @Override
                public void onInterstitialDismissed(Ad ad) {
                    sharedPref.setIntersCounter(0);
                    loadInterstitialAd(true);
                }

                @Override
                public void onError(Ad ad, AdError adError) {
                    adMobInterstitialAd = null;
                    Log.i(TAG, "FAN interstitial onError");
                    delayAndInterstitial(true, finalIndex, finalRetry);
                }

                @Override
                public void onAdLoaded(Ad ad) {
                    Log.i(TAG, "FAN interstitial onAdLoaded");
                }

                @Override
                public void onAdClicked(Ad ad) {
                }

                @Override
                public void onLoggingImpression(Ad ad) {
                }
            };

            // load ads
            fanInterstitialAd.loadAd(fanInterstitialAd.buildLoadAdConfig().withAdListener(interstitialAdListener).build());
        }
    }

    private void delayAndInterstitial(boolean enable, int ad_index, int retry_count) {
        Log.d(TAG, "delayAndInterstitial ad_index : " + ad_index + " retry_count : " + retry_count);
        new Handler(activity.getMainLooper()).postDelayed(() -> {
            loadInterstitialAd(enable, ad_index, retry_count);
        }, 2000);
    }

    public boolean showInterstitialAd(boolean enable) {
        if (!AdConfig.ad_enable || !enable) return false;
        int counter = sharedPref.getIntersCounter();
        Log.i(TAG, "COUNTER " + counter);
        if (counter > AdConfig.ad_inters_interval) {
            Log.i(TAG, "COUNTER reach attempt");
            if (AdConfig.ad_networks[last_interstitial_index] == AdNetworkType.ADMOB) {
                if (adMobInterstitialAd == null) {
                    loadInterstitialAd(true);
                    return false;
                }
                adMobInterstitialAd.show(activity);
                adMobInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent();
                        sharedPref.setIntersCounter(0);
                        loadInterstitialAd(true);
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent();
                        adMobInterstitialAd = null;
                    }
                });
            } else if (AdConfig.ad_networks[last_interstitial_index] == AdNetworkType.FAN) {
                if (fanInterstitialAd == null) {
                    loadInterstitialAd(true);
                    return false;
                }
                if (!fanInterstitialAd.isAdLoaded()) return false;
                fanInterstitialAd.show();
            }
            return true;
        } else {
            Log.i(TAG, "COUNTER not-reach attempt");
            sharedPref.setIntersCounter(sharedPref.getIntersCounter() + 1);
        }
        return false;
    }

    public static void loadAndShowOpenAppAd(Context context, boolean enable, AdOpenListener listener) {
        if (!AdConfig.ad_enable || !enable) {
            if(listener != null) listener.onFinish();
            return;
        }
        if (ad_networks == null || !ad_networks.contains(AdNetworkType.ADMOB)) {
            if(listener != null) listener.onFinish();
            return;
        }
        AdRequest request = new AdRequest.Builder().build();
        String unit_id = new SharedPref(context).getOpenAppUnitId();
        AppOpenAd.load(context, unit_id, request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                super.onAdLoaded(ad);
                AppOpenAd appOpenAd_ = ad;
                FullScreenContentCallback fullScreenContentCallback = new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        if(listener != null) listener.onFinish();
                        //loadOpenAppAd(context, true);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                        if(listener != null) listener.onFinish();
                        //loadOpenAppAd(context, true);
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {

                    }
                };

                appOpenAd_.setFullScreenContentCallback(fullScreenContentCallback);
                if(activityListener != null && ActivityListener.currentActivity != null){
                    appOpenAd_.show(ActivityListener.currentActivity);
                } else {
                    if(listener != null) listener.onFinish();
                }
                Log.d(TAG, "ADMOB Open App loaded");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                if(listener != null) listener.onFinish();
                Log.d(TAG, "ADMOB Open App load failed : " + loadAdError.getMessage());
            }

        });
    }

    public static void loadOpenAppAd(Context context, boolean enable) {
        if (!AdConfig.ad_enable || !enable ) return;
        if (ad_networks == null || !ad_networks.contains(AdNetworkType.ADMOB)) {
            return;
        }
        AdRequest request = new AdRequest.Builder().build();
        appOpenAdLoading = true;
        String unit_id = new SharedPref(context).getOpenAppUnitId();
        AppOpenAd.load(context, unit_id, request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                super.onAdLoaded(ad);
                appOpenAd = ad;
                appOpenAdLoading = false;
                openApploadTime = (new Date()).getTime();
                Log.d(TAG, "ADMOB Open App loaded");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                appOpenAdLoading = false;
                Log.d(TAG, "ADMOB Open App load failed : " + loadAdError.getMessage());
            }

        });
    }

    public static void showOpenAppAd(Context context, boolean enable) {
        showOpenAppAdCore(context, enable, null);
    }

    public static void showOpenAppAd(Context context, boolean enable, AdOpenListener listener) {
        showOpenAppAdCore(context, enable, listener);
    }

    public static void showOpenAppAdCore(Context context, boolean enable, AdOpenListener listener) {
        if (!AdConfig.ad_enable || !enable || appOpenAdLoading) {
            if(listener != null) listener.onFinish();
            return;
        }
        if (ad_networks == null || !ad_networks.contains(AdNetworkType.ADMOB)) {
            if(listener != null) listener.onFinish();
            return;
        }
        if(!wasLoadTimeLessThanNHoursAgo(4)){
            if(listener != null) listener.onFinish();
            loadOpenAppAd(context, true);
            return;
        }
        if(appOpenAd != null && activityListener != null && ActivityListener.currentActivity != null){
            FullScreenContentCallback fullScreenContentCallback = new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    loadOpenAppAd(context, true);
                    if(listener != null) listener.onFinish();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                    loadOpenAppAd(context, true);
                    if(listener != null) listener.onFinish();
                }

                @Override
                public void onAdShowedFullScreenContent() {

                }
            };

            appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
            appOpenAd.show(ActivityListener.currentActivity);
            Log.d(TAG, "ADMOB Open App show");
        }
    }


    private AdSize getAdmobBannerSize() {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);
        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    // check if ad was loaded more than n hours ago.
    private static boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = (new Date()).getTime() - openApploadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    public static int dpToPx(Context c, int dp) {
        Resources r = c.getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

}
