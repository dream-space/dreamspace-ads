package dreamspace.ads;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerListener;
import com.ironsource.mediationsdk.sdk.InterstitialListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

import dreamspace.ads.data.AdNetworkType;
import dreamspace.ads.data.SharedPref;
import dreamspace.ads.gdpr.LegacyGDPR;

public class AdNetwork {

    private static final String TAG = AdNetwork.class.getSimpleName();

    private final Activity activity;
    private final SharedPref sharedPref;
    private static int last_interstitial_index = 0;

    //Interstitial
    private InterstitialAd adMobInterstitialAd;

    public AdNetwork(Activity activity) {
        this.activity = activity;
        sharedPref = new SharedPref(activity);
    }

    public static void init(Activity context) {
        if (!AdConfig.ad_enable) return;
        MobileAds.initialize(context);
        UnityAds.initialize(context, AdConfig.ad_unity_game_id, AdConfig.debug_mode);
        IronSource.init(context, AdConfig.ad_ironsource_app_key, IronSource.AD_UNIT.BANNER, IronSource.AD_UNIT.INTERSTITIAL);
    }

    public void loadBannerAd(boolean enable, LinearLayout ad_container) {
        loadBannerAdMain(enable, 0, 0, ad_container);
    }


    private void loadBannerAdMain(boolean enable, int ad_index, int retry_count, LinearLayout ad_container) {
        if (!AdConfig.ad_enable || !enable) return;
        if (ad_index >= AdConfig.ad_networks.length) return;
        retry_count = retry_count + 1;
        if (retry_count >= AdConfig.retry_ad_networks) {
            retry_count = 0;
            ad_index = ad_index + 1;
        }
        int finalRetry = retry_count;
        if(ad_index >= AdConfig.ad_networks.length) ad_index = 0;
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
                        Log.d(TAG, "ADMOB banner onAdFailedToLoad");
                        delayAndLoadBanner(true, finalIndex, finalRetry, ad_container);
                    }
                });
            } else if (AdConfig.ad_networks[finalIndex] == AdNetworkType.UNITY) {
                BannerView bottomBanner = new BannerView(activity, AdConfig.ad_unity_banner_unit_id, getUnityBannerSize());
                bottomBanner.setListener(new BannerView.IListener() {
                    @Override
                    public void onBannerLoaded(BannerView bannerView) {
                        ad_container.setVisibility(View.VISIBLE);
                        Log.d(TAG, "UNITY banner onBannerLoaded");
                    }

                    @Override
                    public void onBannerClick(BannerView bannerView) {

                    }

                    @Override
                    public void onBannerFailedToLoad(BannerView bannerView, BannerErrorInfo bannerErrorInfo) {
                        ad_container.setVisibility(View.GONE);
                        Log.d(TAG, "UNITY banner onBannerFailedToLoad");
                        delayAndLoadBanner(true, finalIndex, finalRetry, ad_container);
                    }

                    @Override
                    public void onBannerLeftApplication(BannerView bannerView) {

                    }
                });
                ad_container.addView(bottomBanner);
                bottomBanner.load();

            } else if (AdConfig.ad_networks[finalIndex] == AdNetworkType.IRONSOURCE) {
                IronSource.init(activity, AdConfig.ad_ironsource_app_key, IronSource.AD_UNIT.BANNER, IronSource.AD_UNIT.INTERSTITIAL);

                IronSourceBannerLayout banner = IronSource.createBanner(activity, ISBannerSize.BANNER);
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                ad_container.addView(banner, 0, layoutParams);
                banner.setBannerListener(new BannerListener() {
                    @Override
                    public void onBannerAdLoaded() {
                        ad_container.setVisibility(View.VISIBLE);
                        Log.d(TAG, "IRONSOURCE banner onBannerAdLoaded");
                    }

                    @Override
                    public void onBannerAdLoadFailed(IronSourceError ironSourceError) {
                        ad_container.setVisibility(View.GONE);
                        Log.d(TAG, "IRONSOURCE banner onBannerAdLoadFailed");
                        delayAndLoadBanner(true, finalIndex, finalRetry, ad_container);
                    }

                    @Override
                    public void onBannerAdClicked() {

                    }

                    @Override
                    public void onBannerAdScreenPresented() {

                    }

                    @Override
                    public void onBannerAdScreenDismissed() {

                    }

                    @Override
                    public void onBannerAdLeftApplication() {

                    }
                });
                IronSource.loadBanner(banner, AdConfig.ad_ironsource_banner_unit_id);

            }
        });
    }

    private void delayAndLoadBanner(boolean enable, int ad_index, int retry_count, LinearLayout ad_container){
        new Handler(activity.getMainLooper()).postDelayed(() -> {
            loadBannerAdMain(enable, ad_index, retry_count, ad_container);
        }, 2000);
    }

    public void loadInterstitialAd(boolean enable) {
        loadInterstitialAd(enable, 0, 0);
    }

    private void loadInterstitialAd(boolean enable, int ad_index, int retry_count) {
        if (!AdConfig.ad_enable || !enable) return;
        last_interstitial_index = ad_index;
        retry_count = retry_count + 1;
        if (retry_count >= AdConfig.retry_ad_networks) {
            retry_count = 0;
            ad_index = ad_index + 1;
        }
        int finalRetry = retry_count;
        if(ad_index >= AdConfig.ad_networks.length) ad_index = 0;
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
                    Log.i(TAG, loadAdError.getMessage());
                    adMobInterstitialAd = null;
                    Log.i(TAG, "ADMOB interstitial onAdFailedToLoad");
                    delayAndInterstitial(true, finalIndex, finalRetry);
                }
            });
        } else if (AdConfig.ad_networks[finalIndex] == AdNetworkType.UNITY) {
            UnityAds.load(AdConfig.ad_unity_interstitial_unit_id, new IUnityAdsLoadListener() {
                @Override
                public void onUnityAdsAdLoaded(String placementId) {
                    Log.i(TAG, "UNITY interstitial onUnityAdsAdLoaded");
                }

                @Override
                public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                    Log.i(TAG, "UNITY interstitial onUnityAdsFailedToLoad");
                    delayAndInterstitial(true, finalIndex, finalRetry);
                }
            });
        } else if (AdConfig.ad_networks[finalIndex] == AdNetworkType.IRONSOURCE) {
            IronSource.loadInterstitial();
            IronSource.setInterstitialListener(new InterstitialListener() {
                @Override
                public void onInterstitialAdReady() {
                    Log.i(TAG, "IRONSOURCE interstitial onInterstitialAdReady");
                }

                @Override
                public void onInterstitialAdLoadFailed(IronSourceError ironSourceError) {
                    Log.i(TAG, "IRONSOURCE interstitial onInterstitialAdLoadFailed");
                    delayAndInterstitial(true, finalIndex, finalRetry);
                }

                @Override
                public void onInterstitialAdOpened() {

                }

                @Override
                public void onInterstitialAdClosed() {

                }

                @Override
                public void onInterstitialAdShowSucceeded() {

                }

                @Override
                public void onInterstitialAdShowFailed(IronSourceError ironSourceError) {

                }

                @Override
                public void onInterstitialAdClicked() {

                }
            });
        }
    }

    private void delayAndInterstitial(boolean enable, int ad_index, int retry_count){
        new Handler(activity.getMainLooper()).postDelayed(() -> {
            loadInterstitialAd(enable, ad_index, retry_count);
        }, 2000);
    }

    public boolean showInterstitialAd(boolean enable) {
        if (!AdConfig.ad_enable || !enable) return false;
        int counter = sharedPref.getIntersCounter();
        if (counter > AdConfig.ad_inters_interval) {
            if (AdConfig.ad_networks[last_interstitial_index] == AdNetworkType.ADMOB) {
                if (adMobInterstitialAd == null) {
                    loadInterstitialAd(enable);
                    return false;
                }
                adMobInterstitialAd.show(activity);
                adMobInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent();
                        sharedPref.setIntersCounter(0);
                        loadInterstitialAd(enable);
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent();
                        adMobInterstitialAd = null;
                    }
                });
            } else if (AdConfig.ad_networks[last_interstitial_index] == AdNetworkType.UNITY) {
                UnityAds.show(activity, AdConfig.ad_unity_interstitial_unit_id, new IUnityAdsShowListener() {
                    @Override
                    public void onUnityAdsShowFailure(String s, UnityAds.UnityAdsShowError unityAdsShowError, String s1) {

                    }

                    @Override
                    public void onUnityAdsShowStart(String s) {
                        sharedPref.setIntersCounter(0);
                        loadInterstitialAd(enable);
                    }

                    @Override
                    public void onUnityAdsShowClick(String s) {

                    }

                    @Override
                    public void onUnityAdsShowComplete(String s, UnityAds.UnityAdsShowCompletionState unityAdsShowCompletionState) {

                    }
                });

            } else if (AdConfig.ad_networks[last_interstitial_index] == AdNetworkType.IRONSOURCE) {
                if (IronSource.isInterstitialReady()) {
                    IronSource.showInterstitial(AdConfig.ad_ironsource_interstitial_unit_id);
                }
                IronSource.setInterstitialListener(new InterstitialListener() {
                    @Override
                    public void onInterstitialAdReady() {

                    }

                    @Override
                    public void onInterstitialAdLoadFailed(IronSourceError ironSourceError) {

                    }

                    @Override
                    public void onInterstitialAdOpened() {

                    }

                    @Override
                    public void onInterstitialAdClosed() {
                        sharedPref.setIntersCounter(0);
                        loadInterstitialAd(enable);
                    }

                    @Override
                    public void onInterstitialAdShowSucceeded() {

                    }

                    @Override
                    public void onInterstitialAdShowFailed(IronSourceError ironSourceError) {

                    }

                    @Override
                    public void onInterstitialAdClicked() {

                    }
                });

            }
            return true;
        } else {
            sharedPref.setIntersCounter(sharedPref.getIntersCounter() + 1);
        }
        return false;
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

    private UnityBannerSize getUnityBannerSize() {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);
        return new UnityBannerSize(adWidth, 50);
    }

    public static int dpToPx(Context c, int dp) {
        Resources r = c.getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

}
