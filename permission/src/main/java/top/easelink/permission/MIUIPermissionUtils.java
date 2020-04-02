package top.easelink.permission;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import timber.log.Timber;

/**
 * Created by Liu Hanhong on 16/9/26.
 */

public class MIUIPermissionUtils {

    private static final String TAG = MIUIPermissionUtils.class.getSimpleName();

    public static boolean checkPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return checkPermissionKITKAT(context, permission);
        } else {
            return checkPermissionDefault(context, permission);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static boolean checkPermissionKITKAT(Context context, String permission) {
        boolean result = true;
        try {
            if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(permission)) {
                permission = "COARSE_LOCATION";
            } else if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission)) {
                permission = "FINE_LOCATION";
            } else {
                permission = permission.replaceFirst("android.permission.", "");
            }
            AppOpsManager mgr = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            PackageManager pm = context.getPackageManager();
            PackageInfo info;
            info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            Class<?> classType = AppOpsManager.class;
            Field f = classType.getField("OP_" + permission);
            Method m = classType.getDeclaredMethod("checkOp", int.class, int.class, String.class);
            m.setAccessible(true);
            int status = (Integer) m.invoke(mgr, f.getInt(mgr), info.applicationInfo.uid, info.packageName);
            // MIUI8用4来做权限标示
            result = (status != AppOpsManager.MODE_ERRORED && status != AppOpsManager.MODE_IGNORED && status != 4);
        } catch (Exception e) {
            Timber.e("权限检查出错时默认返回有权限，异常代码：" + e);
        }
        return result;
    }

    private static boolean checkPermissionDefault(Context context, String permission) {
        PackageManager pm = context.getPackageManager();
        return (PackageManager.PERMISSION_GRANTED ==
                pm.checkPermission(permission, context.getPackageName()));
    }

    /**
     * 跳转到APP权限设置界面
     */
    public static void startPermissionManager(Activity activity) {
        Intent intent;
        if (DeviceUtils.isMiui()) {
            PackageManager pm = activity.getPackageManager();
            PackageInfo info;
            String packageName = activity.getPackageName();
            try {
                info = pm.getPackageInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                Timber.e(e);
                return;
            }
            intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
            // i.setClassName("com.android.settings", "com.miui.securitycenter.permission.AppPermissionsEditor");
            intent.putExtra("extra_pkgname", packageName);      // for MIUI 6
            intent.putExtra("extra_package_uid", info.applicationInfo.uid);  // for MIUI 5
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
            intent.setData(uri);

        }
        if (IntentUtil.isIntentAvailable(activity, intent)) {
            activity.startActivity(intent);
        }
    }
}
