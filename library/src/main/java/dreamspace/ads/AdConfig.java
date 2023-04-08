package dreamspace.ads;

import java.io.Serializable;

import dreamspace.ads.data.AdNetworkType;

public class AdConfig implements Serializable {

    public static boolean ad_enable = true;
    public static boolean debug_mode = true;
    public static boolean enable_gdpr = true;
    public static int retry_ad_networks = 3;
    public static AdNetworkType[] ad_networks = {
            AdNetworkType.ADMOB, AdNetworkType.IRONSOURCE, AdNetworkType.UNITY
    };
    public static int ad_inters_interval = 5;

    public static String ad_admob_publisher_id = "pub-3239677920600357";
    public static String ad_admob_banner_unit_id = "ca-app-pub-3940256099942544/6300978111";
    public static String ad_admob_interstitial_unit_id = "ca-app-pub-3940256099942544/1033173712";

    public static String ad_ironsource_app_key = "170112cfd";
    public static String ad_ironsource_banner_unit_id = "DefaultBanner";
    public static String ad_ironsource_interstitial_unit_id = "DefaultInterstitial";

    public static String ad_unity_game_id = "4297717";
    public static String ad_unity_banner_unit_id = "Banner_Android";
    public static String ad_unity_interstitial_unit_id = "Interstitial_Android";

}
