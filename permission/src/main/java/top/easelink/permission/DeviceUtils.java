package top.easelink.permission;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.view.ViewConfiguration;
import android.view.Window;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Properties;

public class DeviceUtils {
    private static int sStatusBarHeight = 0;

    /**
     * check current ROM is MIUI or not
     */
    private static boolean sIsMiui = false;
    private static boolean sIsMiuiInited = false;

    public static boolean isMiui() {
        if (!sIsMiuiInited) {
            try {
                Class<?> clz = Class.forName("miui.os.Build");
                if (clz != null) {
                    sIsMiui = true;
                }
            } catch (Exception e) {
                // ignore
            }
            sIsMiuiInited = true;
        }
        return sIsMiui;
    }

    /**
     * Miui自有Api
     */
    public static void setMiuiStatusBarDarkMode(boolean darkmode, Window window) {
        try {
            Class<? extends Window> clazz = window.getClass();
            Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
            Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
            int darkModeFlag = field.getInt(layoutParams);
            Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
            extraFlagField.invoke(window, darkmode ? darkModeFlag : 0, darkModeFlag);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static String sMiuiVersion;

    public static boolean isMiuiV7() {
        initMiuiVersion();
        return "V7".equals(sMiuiVersion);
    }

    public static boolean isMiuiV8() {
        initMiuiVersion();
        return "V8".equals(sMiuiVersion);
    }

    private static void initMiuiVersion() {
        if (sMiuiVersion == null) {
            sMiuiVersion = getSystemProperty("ro.miui.ui.version.name");
            sMiuiVersion = sMiuiVersion == null ? "" : sMiuiVersion;
        }
    }

    private static String getSystemProperty(String propName) {
        String line = "";
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return line;
    }

    private static int sEmuiLevel = -1;

    public static int getEmuiLevel() {
        if (sEmuiLevel > -1) {
            return sEmuiLevel;
        }
        sEmuiLevel = 0;
        Properties properties = new Properties();
        File propFile = new File(Environment.getRootDirectory(), "build.prop");
        FileInputStream fis = null;
        if (propFile.exists()) {
            try {
                fis = new FileInputStream(propFile);
                properties.load(fis);
                fis.close();
                fis = null;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
            }
        }
        if (properties.containsKey("ro.build.hw_emui_api_level")) {
            String valueString = properties.getProperty("ro.build.hw_emui_api_level");
            try {
                sEmuiLevel = Integer.parseInt(valueString);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sEmuiLevel;
    }

    public static boolean isHuawei() {
        return Build.MANUFACTURER != null && Build.MANUFACTURER.contains("HUAWEI");
    }

    public static boolean isSumsungV4_4_4() {
        if (isSamsung()) {
            if (Build.VERSION.RELEASE.startsWith("4.4.4")) {
                return true;
            } else if (Build.VERSION.RELEASE.startsWith("4.4.2") && Build.DEVICE.startsWith("klte")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSumsungV5() {
        if (isSamsung()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSumsungCorePrime() {
        if (isSamsung()) {
            if (Build.DISPLAY.contains("G3608ZMU1AOA4")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFlyme4() {
        return Build.DISPLAY.startsWith("Flyme OS 4");
    }

    public static boolean isFlyme2() {
        return Build.DISPLAY.startsWith("Flyme 2");
    }

    public static boolean isOnePlusLOLLIPOP() {
        return Build.BRAND.equals("ONEPLUS") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean isSamsung() {
        if ("samsung".equalsIgnoreCase(Build.BRAND) || "samsung".equalsIgnoreCase(Build.MANUFACTURER)) {
            return true;
        }

        return false;
    }

    public static boolean isLG() {
        if ("lge".equalsIgnoreCase(Build.BRAND) || "lge".equalsIgnoreCase(Build.MANUFACTURER)) {
            if (Build.MODEL != null) {
                String str = Build.MODEL.toLowerCase();
                if (str.contains("lg")) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isMeizuMx3() {
        if (isMeizu()) {
            return "mx3".equalsIgnoreCase(Build.DEVICE);
        }

        return false;
    }

    public static boolean isHtcOs() {
        if (Build.BRAND != null && Build.BRAND.toLowerCase().contains("htc")
                && Build.MANUFACTURER != null && Build.MANUFACTURER.toLowerCase().contains("htc")
                && Build.MODEL != null && Build.MODEL.toLowerCase().contains("htc")) {
            return true;
        }
        return false;
    }

    public static boolean isMeizu() {
        String brand = Build.BRAND;
        if (brand == null) {
            return false;
        }

        return brand.toLowerCase(Locale.ENGLISH).indexOf("meizu") > -1;
    }

    public static boolean hasSmartBar() {
        if (!isMeizu()) {
            return false;
        }

        try {
            Method method = Class.forName("android.os.Build").getMethod("hasSmartBar");
            return ((Boolean) method.invoke(null)).booleanValue();
        } catch (Exception e) {
        }

        if (Build.DEVICE.equals("mx") || Build.DEVICE.equals("m9")) {
            return false;
        }
        return true;
    }

    @SuppressLint("NewApi")
    public static boolean hasVirtualButtons(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            boolean hasPermanentMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey();
            boolean hasVirtualButtons = !hasPermanentMenuKey;
            return hasVirtualButtons;
        } else {
            return false;
        }
    }

    public static boolean isLenovo() {
        if ("lenovo".equalsIgnoreCase(Build.BRAND) || "lenovo".equalsIgnoreCase(Build.MANUFACTURER)) {
            return true;
        }
        return false;
    }

    public static int getStatusBarHeight(Context context) {
        if (sStatusBarHeight > 0) {
            return sStatusBarHeight;
        }
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelOffset(resourceId);
        }

        if (result == 0) {
            result = (int) dip2Px(context, 25);
        }
        sStatusBarHeight = result;
        return result;
    }

    public static boolean isOppo() {
        return "oppo".equalsIgnoreCase(Build.MANUFACTURER) || "oppo".equalsIgnoreCase(Build.BRAND);
    }

    public static boolean isXiaomi() {
        return "xiaomi".equalsIgnoreCase(Build.MANUFACTURER) || "xiaomi".equalsIgnoreCase(Build.BRAND);
    }

    public static Intent getSafeCenterIntent() {
        Intent intent = new Intent();
        if (isOppo()) {
            intent.setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity");
        } else if (isXiaomi()) {
            intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
        } else {
            intent = null;
        }
        return intent;
    }

    public static String getSafeCenterPackageName() {
        if (isXiaomi()) {
            return "com.miui.securitycenter";
        }
        if (isOppo()) {
            return "com.coloros.safecenter";
        }
        return "";
    }
    private static float dip2Px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return dipValue * scale + 0.5f;
    }


}
