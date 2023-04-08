package dreamspace.ads.demo;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import dreamspace.ads.AdConfig;
import dreamspace.ads.AdNetwork;

public class MainActivity extends AppCompatActivity {

    private AdNetwork adNetwork;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AdNetwork.init(this);
        adNetwork = new AdNetwork(this);
        AdConfig.ad_inters_interval = 0;

        AdNetwork.init(this);
        adNetwork.loadBannerAd(true, findViewById(R.id.banner_admob));
        adNetwork.loadInterstitialAd(true);


        ((Button) findViewById(R.id.inters_admob)).setOnClickListener(view -> {
            adNetwork.showInterstitialAd(true);
        });


    }
}