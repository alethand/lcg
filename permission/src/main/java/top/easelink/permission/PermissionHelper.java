package top.easelink.permission;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.SparseArray;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {

    public static final int CONTACTS = 1;
    public static final int PHONE = 2;
    public static final int CAMERA = 3;
    public static final int LOCATION = 4;
    public static final int STORAGE = 5;
    public static final int MICROPHONE = 6;
    public static final int SMS = 7;

    public static Application sApplication;


    public static SparseArray<Integer> sPermissionMap = new SparseArray<>();

    static {
        sPermissionMap.put(CAMERA, R.string.REQUEST_PERMISSION_DESCRIPT_CAMERA);
        sPermissionMap.put(STORAGE, R.string.REQUEST_PERMISSION_DESCRIPT_EXTERNAL_STORAGE);
        sPermissionMap.put(MICROPHONE, R.string.REQUEST_PERMISSION_DESCRIPT_RECORD_AUDIO);
    }

    @IntDef({CONTACTS, PHONE, CAMERA, LOCATION, STORAGE, MICROPHONE, SMS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AppPermission {
    }

    public static boolean hasPermissions(@AppPermission int... permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }
        return PermissionManager.getInstance().hasAllPermissions(sApplication, getRawPermission(permissions));
    }

    public static boolean hasPermissions(Context context, @AppPermission int... permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }
        return PermissionManager.getInstance().hasAllPermissions(context, getRawPermission(permissions));
    }

    public static void requestPermissions(Activity act, PermissionsResultAction action, @AppPermission int... permissions) {
        requestPermissions(act, action, null, null, permissions);
    }

    public static void requestPermissions(Activity act, PermissionsResultAction action, boolean showDialog, @AppPermission int... permissions) {
        requestPermissions(act, action, null, null, showDialog, permissions);
    }

    public static void requestPermissions(Activity act, PermissionsResultAction action, DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener cancelListener, @AppPermission int... permissions) {
        requestPermissions(act, action, okListener, cancelListener, true, permissions);
    }

    public static void requestPermissions(Activity act, PermissionsResultAction action, DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener cancelListener, boolean showDialog,  @AppPermission int... permissions) {
        if (permissions == null || permissions.length == 0) {
            if (action != null) {
                action.onGranted();
            }
            return;
        }

        PermissionManager.DialogInfo dialogInfo = null;
        if (showDialog) {
            dialogInfo = getDialogInfo(act, permissions, okListener, cancelListener);
        }

        PermissionManager.getInstance().requestPermissionsIfNecessaryForResult(act, getRawPermission(permissions), action, dialogInfo);
    }

    private static PermissionManager.DialogInfo getDialogInfo(Activity activity, int[] permissions, DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener cancelListener) {
        List<Integer> permissionList = new ArrayList<>();
        List<String> tips = new ArrayList<>();

        for (int permission : permissions) {
            if (!hasPermissions(permission)) {
                permissionList.add(permission);

                if (sPermissionMap.get(permission) != null) {
                    tips.add(activity.getString(sPermissionMap.get(permission)));
                }
            }
        }

        PermissionManager.DialogInfo dialogInfo = null;

        int count = permissionList.size();
        if (count == 0) {
            return null;
        } else if (count == 1) {
            String tip;
            switch (permissionList.get(0)) {
                case STORAGE:
//                    dialogInfo = new PermissionManager.DialogInfo(R.string.storage_permission_missing);
                    break;
                case CAMERA:
                case MICROPHONE:
                    tip = activity.getString(R.string.permission_multi_tip, TextUtils.join(",", tips));
                    dialogInfo = new PermissionManager.DialogInfo(tip);
                    break;
                case CONTACTS :
//                    dialogInfo = new PermissionManager.DialogInfo(R.string.permission_contacts_tip);
                    break;
                case PHONE:
//                    dialogInfo = new PermissionManager.DialogInfo(R.string.permission_device_id_tip);
                    break;
                case LOCATION:
//                     dialogInfo = new PermissionManager.DialogInfo(R.string.permission_location_tip);
                     break;
                case SMS:
//                     dialogInfo = new PermissionManager.DialogInfo(R.string.permission_sms_tip);
                default:
                    break;
            }
        } else {
            if (!tips.isEmpty()) {
                String tip = activity.getString(R.string.permission_multi_tip, TextUtils.join("、", tips));
                dialogInfo = new PermissionManager.DialogInfo(tip);
            } else {
                dialogInfo = new PermissionManager.DialogInfo(R.string.permission_multi_tip);
            }
        }

        if (dialogInfo != null) {
            dialogInfo.setOkListener(okListener);
            dialogInfo.setCancelListener(cancelListener);
        }

        return dialogInfo;
    }

    /**
     * <uses-permission android:name="android.permission.ACCESS_MOCK_LOCATION" />
     * <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS" />
     * <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT" />
     * <p>
     * <uses-permission android:name="android.permission.INTERNET" />
     * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
     * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
     * <uses-permission android:name="com.android.launcher.permission.READ_SETTINGS" />
     * <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT" />
     * <uses-permission android:name="com.android.launcher.permission.UNINSTALL_SHORTCUT" />
     * <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
     * <uses-permission android:name="android.permission.WAKE_LOCK" />
     * <uses-permission android:name="android.permission.CHANGE_CONFIGURATION" />
     * <uses-permission android:name="android.permission.WRITE_SECURE_SETTINGS" />
     * <uses-permission android:name="com.google.android.launcher.permission.READ_SETTINGS" />
     * <!-- 读取桌面是否已经有快捷方式-->
     * <uses-permission android:name="com.android.launcher2.permission.READ_SETTINGS" />
     * <uses-permission android:name="com.android.launcher3.permission.READ_SETTINGS" />
     * <!-- WRITE_SETTINGS is for HTML5VideoView of Coolpad/Lenovo 4.1.2 -->
     * <uses-permission android:name="android.permission.WRITE_SETTINGS" />
     * <uses-permission android:name="android.permission.GET_TASKS" />
     * <!-- 高德定位SDK -->
     * <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
     * <!-- Google cloud messaging-->
     * <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE"/>
     * <permission android:name="com.ss.android.article.pagenewark.permission.C2D_MESSAGE" android:protectionLevel="signature"/>
     * <uses-permission android:name="com.ss.android.article.pagenewark.permission.C2D_MESSAGE"/>
     * <!-- Mixpanel -->
     * <uses-permission android:name="android.permission.BLUETOOTH" />
     * <!-- 添加/移除帐号 -->
     * <uses-permission android:name="android.permission.MANAGE_ACCOUNTS" />
     * <!-- 震动 -->
     * <uses-permission android:name="android.permission.VIBRATE" />
     * <p>
     * <uses-permission android:name="android.permission.GET_PACKAGE_SIZE"/>
     */
    public static String[] getRawPermission(@AppPermission int... permissionArray) {
        List<String> permissionList = new ArrayList<>();
        for (int permission : permissionArray) {
            switch (permission) {
                case CONTACTS:
                    permissionList.add(Manifest.permission.READ_CONTACTS);
                    permissionList.add(Manifest.permission.GET_ACCOUNTS);
                    break;
                case PHONE:
                    permissionList.add(Manifest.permission.READ_PHONE_STATE);
                    break;
                case CAMERA:
                    permissionList.add(Manifest.permission.CAMERA);
                    break;
                case LOCATION:
                    permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                    permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
//                    permissionList.add(Manifest.permission.ACCESS_MOCK_LOCATION);
                    permissionList.add(Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS);
                    break;
                case STORAGE:
                    permissionList.add("android.permission.READ_EXTERNAL_STORAGE");
                    permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    break;
                case MICROPHONE:
                    permissionList.add(Manifest.permission.RECORD_AUDIO);
                    break;
                case SMS:
                    permissionList.add(Manifest.permission.READ_SMS);
                    break;
            }
        }

        return permissionList.toArray(new String[permissionArray.length]);
    }
}
