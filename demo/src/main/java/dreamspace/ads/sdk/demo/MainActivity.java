package dreamspace.ads.sdk.demo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import dreamspace.ads.AdConfig;
import dreamspace.ads.AdNetwork;
import dreamspace.ads.data.AdNetworkType;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AdNetwork adNetwork = new AdNetwork(this);
        AdConfig.ad_inters_interval = 2;
        AdConfig.retry_from_start_max = 2;
        AdConfig.retry_from_start = true;
        AdConfig.ad_networks = new AdNetworkType[]{
                AdNetworkType.FAN,
                AdNetworkType.FAN,
                //AdNetworkType.APPLOVIN,
        };

        adNetwork.init();

//        adNetwork.loadBannerAd(true, findViewById(R.id.banner_admob));
        adNetwork.loadInterstitialAd(true);

//        AdConfig.ad_network = AdNetworkType.FAN;
//        AdNetwork.init(this);
        adNetwork.loadBannerAd(true, findViewById(R.id.banner_fan));
//        adNetwork.loadInterstitialAd(true);
//
//        AdConfig.ad_network = AdNetworkType.UNITY;
//        AdNetwork.init(this);
//        adNetwork.loadBannerAd(true, findViewById(R.id.banner_unity));
//        adNetwork.loadInterstitialAd(true);
//
//        AdConfig.ad_network = AdNetworkType.IRONSOURCE;
//        AdNetwork.init(this);
//        adNetwork.loadBannerAd(true, findViewById(R.id.banner_iron_source));
//        adNetwork.loadInterstitialAd(true);
//
//        AdConfig.ad_network = AdNetworkType.APPLOVIN;
//        AdNetwork.init(this);
//        adNetwork.loadBannerAd(true, findViewById(R.id.banner_applovin));
//        adNetwork.loadInterstitialAd(true);

        ((Button) findViewById(R.id.inters_admob)).setOnClickListener(view -> {
            adNetwork.showInterstitialAd(true);
        });

//        ((Button) findViewById(R.id.inters_fan)).setOnClickListener(view -> {
//            AdConfig.ad_network = AdNetworkType.FAN;
//            adNetwork.showInterstitialAd(true);
//        });
//
//        ((Button) findViewById(R.id.inters_unity)).setOnClickListener(view -> {
//            AdConfig.ad_network = AdNetworkType.UNITY;
//            adNetwork.showInterstitialAd(true);
//        });
//
//        ((Button) findViewById(R.id.inters_ironsource)).setOnClickListener(view -> {
//            AdConfig.ad_network = AdNetworkType.IRONSOURCE;
//            adNetwork.showInterstitialAd(true);
//        });
//
//        ((Button) findViewById(R.id.inters_applovin)).setOnClickListener(view -> {
//            AdConfig.ad_network = AdNetworkType.APPLOVIN;
//            adNetwork.showInterstitialAd(true);
//        });

        ((Button) findViewById(R.id.next_activity)).setOnClickListener(view -> {
            startActivity(new Intent(this, ThirdActivity.class));
        });

    }
}