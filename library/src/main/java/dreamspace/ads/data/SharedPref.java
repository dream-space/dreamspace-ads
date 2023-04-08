package dreamspace.ads.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPref {

    private Context ctx;
    private SharedPreferences custom_prefence;

    public SharedPref(Context context) {
        this.ctx = context;
        custom_prefence = context.getSharedPreferences("android-ads", Context.MODE_PRIVATE);
    }

    // Preference for first launch
    public void setIntersCounter(int counter) {
        custom_prefence.edit().putInt("INTERS_COUNT", counter).apply();
    }

    public int getIntersCounter() {
        return custom_prefence.getInt("INTERS_COUNT", 0);
    }

    public void clearIntersCounter() {
        custom_prefence.edit().putInt("INTERS_COUNT", 0).apply();
    }


}
