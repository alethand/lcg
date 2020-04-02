package top.easelink.permission;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;

public class ActivityCompatApi23 {
    public interface RequestPermissionsRequestCodeValidator {
        public void validateRequestPermissionsRequestCode(int requestCode);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static void requestPermissions(Activity activity, String[] permissions,
                                          int requestCode) {
        if (activity instanceof RequestPermissionsRequestCodeValidator) {
            ((RequestPermissionsRequestCodeValidator) activity)
                    .validateRequestPermissionsRequestCode(requestCode);
        }
        activity.requestPermissions(permissions, requestCode);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean shouldShowRequestPermissionRationale(Activity activity,
                                                               String permission) {
        return activity.shouldShowRequestPermissionRationale(permission);
    }
}