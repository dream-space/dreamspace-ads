package dreamspace.ads;

import java.io.Serializable;

import dreamspace.ads.data.AdNetworkType;

public class AdConfig implements Serializable {

    public static boolean ad_enable = true;
    public static boolean ad_enable_open_app = true;
    public static int limit_time_open_app_loading = 10;
    public static boolean debug_mode = true;
    public static boolean enable_gdpr = true;

    public static AdNetworkType ad_network = AdNetworkType.ADMOB;

    public static int retry_every_ad_networks = 2;
    public static int retry_from_start_max = 2;
    public static boolean retry_from_start = false;

    public static AdNetworkType[] ad_networks = {

    };

    public static int ad_inters_interval = 5;

    public static String ad_admob_publisher_id = "pub-3940256099942544";
    public static String ad_admob_banner_unit_id = "ca-app-pub-3940256099942544/6300978111";
    public static String ad_admob_interstitial_unit_id = "ca-app-pub-3940256099942544/1033173712";
    public static String ad_admob_open_app_unit_id = "ca-app-pub-3940256099942544/3419835294";

    public static String ad_fan_banner_unit_id = "IMG_16_9_APP_INSTALL#YOUR_PLACEMENT_ID";
    public static String ad_fan_interstitial_unit_id = "IMG_16_9_APP_INSTALL#YOUR_PLACEMENT_ID";


}
