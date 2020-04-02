package top.easelink.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class to help you manage your permissions simply.
 */
public class PermissionManager {

    private static final String TAG = PermissionManager.class.getSimpleName();

    public interface IAlertBuilder {
         AlertDialog.Builder getAlertDialogBuilder(Context context);
    }

    private static IAlertBuilder sAlertBuilder;
    public static void setAlertBuilder(IAlertBuilder builder) {
        sAlertBuilder = builder;
    }

//    private final Set<String> mPendingRequests = new HashSet<>(1);
    private final Set<String> mPermissions = new HashSet<>(1);
    private final List<PermissionsResultAction> mPendingActions = new ArrayList<>(1);
    private final List<PermissionsResultAction> mPendingActionsForGc = new ArrayList<>(1);
    private DialogInfo mPendingDialogInfo;

    private static PermissionManager mInstance = null;

    private static Map<String, Integer> sDescriptMap = new HashMap<>();

    static {
        sDescriptMap.put(Manifest.permission.ACCESS_COARSE_LOCATION, R.string.REQUEST_PERMISSION_DESCRIPT_LOCATION);
        sDescriptMap.put(Manifest.permission.ACCESS_FINE_LOCATION, R.string.REQUEST_PERMISSION_DESCRIPT_LOCATION);
        sDescriptMap.put(Manifest.permission.READ_SMS, R.string.REQUEST_PERMISSION_DESCRIPT_SMS);
        sDescriptMap.put(Manifest.permission.READ_CONTACTS, R.string.REQUEST_PERMISSION_DESCRIPT_CONTACT);
        sDescriptMap.put(Manifest.permission.CAMERA, R.string.REQUEST_PERMISSION_DESCRIPT_CAMERA);
        sDescriptMap.put(Manifest.permission.RECORD_AUDIO, R.string.REQUEST_PERMISSION_DESCRIPT_RECORD_AUDIO);
        sDescriptMap.put(Manifest.permission.READ_PHONE_STATE, R.string.REQUEST_PERMISSION_DESCRIPT_READ_PHONE_STATE);
        sDescriptMap.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.REQUEST_PERMISSION_DESCRIPT_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            sDescriptMap.put(Manifest.permission.READ_EXTERNAL_STORAGE, R.string.REQUEST_PERMISSION_DESCRIPT_EXTERNAL_STORAGE);
        }
    }

    public static PermissionManager getInstance() {
        if (mInstance == null) {
            mInstance = new PermissionManager();
        }
        return mInstance;
    }

    private PermissionManager() {
        initializePermissionsMap();
    }

    /**
     * This method uses reflection to read all the permissions in the Manifest class.
     * This is necessary because some permissions do not exist on older versions of Android,
     * since they do not exist, they will be denied when you check whether you have permission
     * which is problematic since a new permission is often added where there was no previous
     * permission required. We initialize a Set of available permissions and check the set
     * when checking if we have permission since we want to know when we are denied a permission
     * because it doesn't exist yet.
     */
    private synchronized void initializePermissionsMap() {
        Field[] fields = Manifest.permission.class.getFields();
        for (Field field : fields) {
            String name = null;
            try {
                name = (String) field.get("");
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Could not access field", e);
            }
            mPermissions.add(name);
        }
    }

    /**
     * This method retrieves all the permissions declared in the application's manifest.
     * It returns a non null array of permisions that can be declared.
     *
     * @param activity the Activity necessary to check what permissions we have.
     * @return a non null array of permissions that are declared in the application manifest.
     */
    @NonNull
    private synchronized String[] getManifestPermissions(@NonNull final Activity activity) {
        PackageInfo packageInfo = null;
        List<String> list = new ArrayList<>(1);
        try {
            Log.d(TAG, activity.getPackageName());
            packageInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "A problem occurred when retrieving permissions", e);
        }
        if (packageInfo != null) {
            String[] permissions = packageInfo.requestedPermissions;
            if (permissions != null) {
                for (String perm : permissions) {
                    Log.d(TAG, "Manifest contained permission: " + perm);
                    list.add(perm);
                }
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * This method adds the {@link PermissionsResultAction} to the current list
     * of pending actions that will be completed when the permissions are
     * received. The list of permissions passed to this method are registered
     * in the PermissionsResultAction object so that it will be notified of changes
     * made to these permissions.
     *
     * @param permissions the required permissions for the action to be executed.
     * @param action      the action to add to the current list of pending actions.
     */
    private synchronized void addPendingAction(@NonNull String[] permissions,
                                               @Nullable PermissionsResultAction action) {
        if (action == null) {
            return;
        }
        action.registerPermissions(permissions);
        mPendingActionsForGc.add(action);
        mPendingActions.add(action);
    }

    /**
     * This method removes a pending action from the list of pending actions.
     * It is used for cases where the permission has already been granted, so
     * you immediately wish to remove the pending action from the queue and
     * execute the action.
     *
     * @param action the action to remove
     */
    private synchronized void removePendingAction(@Nullable PermissionsResultAction action) {
        for (Iterator<PermissionsResultAction> iterator = mPendingActions.iterator();
             iterator.hasNext(); ) {
            PermissionsResultAction resultAction = iterator.next();
            if (resultAction == action) {
                iterator.remove();
            }
        }
        for (Iterator<PermissionsResultAction> iterator = mPendingActionsForGc.iterator();
             iterator.hasNext(); ) {
            PermissionsResultAction act = iterator.next();
            if (act == action) {
                iterator.remove();
            }
        }
    }

    /**
     * This static method can be used to check whether or not you have a specific permission.
     * It is basically a less verbose method of using {@link PermissionActivityCompat#checkSelfPermission(Context, String)}
     * and will simply return a boolean whether or not you have the permission. If you pass
     * in a null Context object, it will return false as otherwise it cannot check the permission.
     * However, the Activity parameter is nullable so that you can pass in a reference that you
     * are not always sure will be valid or not (e.g. getActivity() from Fragment).
     *
     * @param context    the Context necessary to check the permission
     * @param permission the permission to check
     * @return true if you have been granted the permission, false otherwise
     */
    @SuppressWarnings("unused")
    public synchronized boolean hasPermission(@Nullable Context context, @NonNull String permission) {
        if (context == null) {
            return false;
        }

        if (DeviceUtils.isMiui()) {
            return MIUIPermissionUtils.checkPermission(context, permission) && (PermissionActivityCompat.checkSelfPermission(context, permission)
                    == PackageManager.PERMISSION_GRANTED || !mPermissions.contains(permission));
        } else {
            return (PermissionActivityCompat.checkSelfPermission(context, permission)
                    == PackageManager.PERMISSION_GRANTED || !mPermissions.contains(permission));
        }
    }

    /**
     * This static method can be used to check whether or not you have several specific permissions.
     * It is simpler than checking using {@link PermissionActivityCompat#checkSelfPermission(Context, String)}
     * for each permission and will simply return a boolean whether or not you have all the permissions.
     * If you pass in a null Context object, it will return false as otherwise it cannot check the
     * permission. However, the Activity parameter is nullable so that you can pass in a reference
     * that you are not always sure will be valid or not (e.g. getActivity() from Fragment).
     *
     * @param context     the Context necessary to check the permission
     * @param permissions the permissions to check
     * @return true if you have been granted all the permissions, false otherwise
     */
    @SuppressWarnings("unused")
    public synchronized boolean hasAllPermissions(@Nullable Context context, @NonNull String[] permissions) {
        if (context == null) {
            return false;
        }
        boolean hasAllPermissions = true;
        for (String perm : permissions) {
            hasAllPermissions &= hasPermission(context, perm);
        }
        return hasAllPermissions;
    }

    /**
     * This method will request all the permissions declared in your application manifest
     * for the specified {@link PermissionsResultAction}. The purpose of this method is to enable
     * all permissions to be requested at one shot. The PermissionsResultAction is used to notify
     * you of the user allowing or denying each permission. The Activity and PermissionsResultAction
     * parameters are both annotated Nullable, but this method will not work if the Activity
     * is null. It is only annotated Nullable as a courtesy to prevent crashes in the case
     * that you call this from a Fragment where {@link Fragment#getActivity()} could yield
     * null. Additionally, you will not receive any notification of permissions being granted
     * if you provide a null PermissionsResultAction.
     *
     * @param activity the Activity necessary to request and check permissions.
     * @param action   the PermissionsResultAction used to notify you of permissions being accepted.
     * @param dialogInfo  the information of the dialog which shows when user had checked "never show again"
     */
    @SuppressWarnings("unused")
    public synchronized void requestAllManifestPermissionsIfNecessary(final @Nullable Activity activity,
                                                                      final @Nullable PermissionsResultAction action,
                                                                      @Nullable DialogInfo dialogInfo) {
        if (activity == null) {
            return;
        }
        String[] perms = getManifestPermissions(activity);
        requestPermissionsIfNecessaryForResult(activity, perms, action, dialogInfo);
    }

    /**
     * This method should be used to execute a {@link PermissionsResultAction} for the array
     * of permissions passed to this method. This method will request the permissions if
     * they need to be requested (i.e. we don't have permission yet) and will add the
     * PermissionsResultAction to the queue to be notified of permissions being granted or
     * denied. In the case of pre-Android Marshmallow, permissions will be granted immediately.
     * The Activity variable is nullable, but if it is null, the method will fail to execute.
     * This is only nullable as a courtesy for Fragments where getActivity() may yeild null
     * if the Fragment is not currently added to its parent Activity.
     *
     * @param activity    the activity necessary to request the permissions.
     * @param permissions the list of permissions to request for the {@link PermissionsResultAction}.
     * @param action      the PermissionsResultAction to notify when the permissions are granted or denied.
     * @param dialogInfo  the information of the dialog which shows when user had checked "never show again"
     */
    @SuppressWarnings("unused")
    public synchronized void requestPermissionsIfNecessaryForResult(@Nullable final Activity activity,
                                                                    @NonNull final String[] permissions,
                                                                    @Nullable final PermissionsResultAction action,
                                                                    @Nullable DialogInfo dialogInfo) {
        if (activity == null) {
            return;
        }
        try {
            addPendingAction(permissions, action);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                doPermissionWorkBeforeAndroidM(activity, permissions, action);
            } else {
                List<String> permList = getPermissionsListToRequest(activity, permissions, action);
                if (permList.isEmpty()) {
                    //if there is no permission to request, there is no reason to keep the action int the list
                    if (action != null) {
                        action.onResult(Arrays.asList(permissions), PackageManager.PERMISSION_GRANTED);
                    }
                    removePendingAction(action);
                } else {
                    String[] permsToRequest = permList.toArray(new String[permList.size()]);
//                    mPendingRequests.addAll(permList);
                    mPendingDialogInfo = dialogInfo;
                    PermissionActivityCompat.requestPermissions(activity, permsToRequest, 1);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * This method should be used to execute a {@link PermissionsResultAction} for the array
     * of permissions passed to this method. This method will request the permissions if
     * they need to be requested (i.e. we don't have permission yet) and will add the
     * PermissionsResultAction to the queue to be notified of permissions being granted or
     * denied. In the case of pre-Android Marshmallow, permissions will be granted immediately.
     * The Fragment variable is used, but if {@link Fragment#getActivity()} returns null, this method
     * will fail to work as the activity reference is necessary to check for permissions.
     *
     * @param fragment    the fragment necessary to request the permissions.
     * @param permissions the list of permissions to request for the {@link PermissionsResultAction}.
     * @param action      the PermissionsResultAction to notify when the permissions are granted or denied.
     * @param dialogInfo  the information of the dialog which shows when user had checked "never show again"
     */
    @SuppressWarnings("unused")
    public synchronized void requestPermissionsIfNecessaryForResult(@NonNull Fragment fragment,
                                                                    @NonNull String[] permissions,
                                                                    @Nullable PermissionsResultAction action,
                                                                    @Nullable DialogInfo dialogInfo) {
        Activity activity = fragment.getActivity();
        if (activity == null) {
            return;
        }
        try {
            addPendingAction(permissions, action);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                doPermissionWorkBeforeAndroidM(activity, permissions, action);
            } else {
                List<String> permList = getPermissionsListToRequest(activity, permissions, action);
                if (permList.isEmpty()) {
                    //if there is no permission to request, there is no reason to keep the action int the list
                    if (action != null) {
                        action.onResult(Arrays.asList(permissions), PackageManager.PERMISSION_GRANTED);
                    }
                    removePendingAction(action);
                } else {
                    String[] permsToRequest = permList.toArray(new String[permList.size()]);
//                    mPendingRequests.addAll(permList);
                    mPendingDialogInfo = dialogInfo;
                    fragment.requestPermissions(permsToRequest, 1);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * 重载这个方法的目的是因为这个方法原来会在透明的界面里面调用，
     * 这个透明的界面需要在权限拒绝之后，finish自己，其他的场景不需要finish
     * 所以默认我们传递false
     */
    public synchronized void notifyPermissionsChange(@NonNull Activity activity,
                                                     @NonNull String[] permissions,
                                                     @NonNull int[] results) {
        notifyPermissionsChange(activity, permissions, results, false);
    }
    /**
     * This method notifies the PermissionsManager that the permissions have change. If you are making
     * the permissions requests using an Activity, then this method should be called from the
     * Activity callback onRequestPermissionsResult() with the variables passed to that method. If
     * you are passing a Fragment to make the permissions request, then you should call this in
     * the onRequestPermissionsResult method.
     * It will notify all the pending PermissionsResultAction objects currently
     * in the queue, and will remove the permissions request from the list of pending requests.
     *
     * @param permissions the permissions that have changed.
     * @param results     the values for each permission.
     */
    @SuppressWarnings("unused")
    public synchronized void notifyPermissionsChange(@NonNull Activity activity,
                                                     @NonNull String[] permissions,
                                                     @NonNull int[] results,
                                                        boolean isFinishActivity) {
        try {
            // for each permission check if the user grantet/denied them
            // you may want to group the rationale in a single dialog,
            List<String> neverAskPermissions = new ArrayList<>(3);
            for (int i = 0, len = permissions.length; i < len; i++) {
                String permission = permissions[i];
                if (results[i] == PackageManager.PERMISSION_DENIED || (DeviceUtils.isMiui() && !MIUIPermissionUtils.checkPermission(activity, permission))) {//MIUI针对部分权限会直接返回GRANTED, 需再次判断
                    if (results[i] != PackageManager.PERMISSION_DENIED) {
                        results[i] = PackageManager.PERMISSION_DENIED;//MIUI授权状态纠正
                    }
                    boolean showRationale = PermissionActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
                    if (!showRationale && sDescriptMap.containsKey(permission)) {
                        neverAskPermissions.add(permission);
                    }
                }
            }

            if (!neverAskPermissions.isEmpty()) {
                if (mPendingDialogInfo != null) {
                    handleNeverShowPermissionDialog(activity, permissions, neverAskPermissions.toArray(new String[neverAskPermissions.size()]), results, mPendingDialogInfo, isFinishActivity);
                    return;
                }
            }
            invokeAndClearPermissionResult(permissions, results, null);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void invokeAndClearPermissionResult(@NonNull String[] permissions, @NonNull int[] results, String[] customActionPermissions) {
        try {
            int size = permissions.length;
            if (results.length < size) {
                size = results.length;
            }

            List<String> deniedPermissionList = new ArrayList<>();
            for (int n = 0; n < size; n++) {
                String permission = permissions[n];
                if (results[n] == PackageManager.PERMISSION_DENIED) {
                    deniedPermissionList.add(permission);
                }
            }

            Iterator<PermissionsResultAction> iterator = mPendingActions.iterator();
            while (iterator.hasNext()) {
                PermissionsResultAction action = iterator.next();
                if (customActionPermissions != null && customActionPermissions.length > 0 && action instanceof CustomPermissionsResultAction) {
                    ((CustomPermissionsResultAction) action).onCustomAction(customActionPermissions);
                    continue;
                }

                if (deniedPermissionList.isEmpty()) {
                    action.onResult(Arrays.asList(permissions), PackageManager.PERMISSION_GRANTED);
                    removePendingAction(action);
                } else {
                    action.onResult(deniedPermissionList, PackageManager.PERMISSION_DENIED);
                    removePendingAction(action);
                }

            }
            Iterator<PermissionsResultAction> iteratorForGc = mPendingActionsForGc.iterator();
            while (iteratorForGc.hasNext()) {
                iteratorForGc.next();
                iteratorForGc.remove();
            }

//            for (int n = 0; n < size; n++) {
//                mPendingRequests.remove(permissions[n]);
//            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void handleNeverShowPermissionDialog(final Activity activity,
                                                 final String[] permissions,
                                                 final String[] neverAskPermissions,
                                                 @NonNull final int[] results,
                                                 @Nullable final DialogInfo dialogInfo,
                                                 final boolean isFinishActivity) {
        if (activity == null || permissions == null || permissions.length <= 0) {
            return;
        }
        try {
            showSimpleDialog(activity, dialogInfo,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (dialogInfo != null && dialogInfo.getOkListener() != null) {
                                dialogInfo.getOkListener().onClick(dialog, which);
                            }

                            if (DeviceUtils.isMiui()) {
                                MIUIPermissionUtils.startPermissionManager(activity);
                            } else {
                                try {
                                    Uri packageURI = Uri.parse("package:" + activity.getPackageName());
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                                    activity.startActivity(intent);
                                } catch (Throwable tr) {
                                    // ignore
                                    tr.printStackTrace();
                                }
                            }
                            if (activity != null && isFinishActivity) {
                                activity.finish();
                            }
                            invokeAndClearPermissionResult(permissions, results, neverAskPermissions);
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (dialogInfo != null && dialogInfo.getCancelListener() != null) {
                                dialogInfo.getCancelListener().onClick(dialog, which);
                            }
                            invokeAndClearPermissionResult(permissions, results, null);
                            if (activity != null && isFinishActivity) {
                                activity.finish();
                            }
                        }
                    });
        } catch (Throwable t) {
            // ignore
            t.printStackTrace();
        }
    }

    /**
     * When request permissions on devices before Android M (Android 6.0, API Level 23)
     * Do the granted or denied work directly according to the permission status
     *
     * @param activity    the activity to check permissions
     * @param permissions the permissions names
     * @param action      the callback work object, containing what we what to do after
     *                    permission check
     */
    private void doPermissionWorkBeforeAndroidM(@NonNull Activity activity,
                                                @NonNull String[] permissions,
                                                @Nullable PermissionsResultAction action) {
//        for (String perm : permissions) {
//            try {
//                if (action != null) {
//                    boolean result = false;
//                    if (!mPermissions.contains(perm)) {
//                        result = action.onResult(perm, Permissions.NOT_FOUND);
//                    } else if (PermissionActivityCompat.checkSelfPermission(activity, perm)
//                            != PackageManager.PERMISSION_GRANTED) {
//                        result = action.onResult(perm, Permissions.DENIED);
//                    } else {
//                        result = action.onResult(perm, Permissions.GRANTED);
//                    }
//                    if (result) {
//                        break;
//                    }
//                }
//            } catch (Throwable t) {
//                t.printStackTrace();
//            }
//        }

        if (action == null) {
            return;
        }

        List<String> deniedPermissionList = new ArrayList<>();
        for (String perm : permissions) {
            if (!mPermissions.contains(perm)) {
                continue;
            }

            if (PermissionActivityCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissionList.add(perm);
            }
        }

        if (deniedPermissionList.isEmpty()) {
            action.onResult(Arrays.asList(permissions), PackageManager.PERMISSION_GRANTED);
        } else {
            action.onResult(deniedPermissionList, PackageManager.PERMISSION_DENIED);
        }

        removePendingAction(action);
    }

    /**
     * Filter the permissions list:
     * If a permission is not granted, add it to the result list
     * if a permission is granted, do the granted work, do not add it to the result list
     *
     * @param activity    the activity to check permissions
     * @param permissions all the permissions names
     * @param action      the callback work object, containing what we what to do after
     *                    permission check
     * @return a list of permissions names that are not granted yet
     */
    @NonNull
    private List<String> getPermissionsListToRequest(@NonNull Activity activity,
                                                     @NonNull String[] permissions,
                                                     @Nullable PermissionsResultAction action) {
        List<String> permList = new ArrayList<>(permissions.length);
        for (String perm : permissions) {
            if (!mPermissions.contains(perm)) {
//                if (action != null) {
//                    action.onResult(perm, Permissions.NOT_FOUND);
//                }
            } else if (!hasPermission(activity, perm)) {
//                if (!mPendingRequests.contains(perm)) {
                    permList.add(perm);
//                }
            } else {
//                if (action != null) {
//                    action.onResult(perm, Permissions.GRANTED);
//                }
            }
        }
        return permList;
    }

    private void showSimpleDialog(Context context, DialogInfo info, DialogInterface.OnClickListener okListener,
                                  DialogInterface.OnClickListener cancelListener) {
        if (info == null) {
            return;
        }

        AlertDialog.Builder builder;
        if (sAlertBuilder != null) {
            builder = sAlertBuilder.getAlertDialogBuilder(context);
        } else {
            builder = new AlertDialog.Builder(context);
        }
        boolean needShow = false;
        if (!TextUtils.isEmpty(info.getTitleTxt(context))) {
            builder.setTitle(info.getTitleTxt(context));
            needShow = true;
        }
        if (!TextUtils.isEmpty(info.getMessage(context))) {
            builder.setMessage(info.getMessage(context));
            needShow = true;
        }
        if (!TextUtils.isEmpty(info.getCancelTxt(context)) || !TextUtils.isEmpty(info.getOkTxt(context))) {
            if (!TextUtils.isEmpty(info.getCancelTxt(context))) {
                builder.setNegativeButton(info.getCancelTxt(context), cancelListener);
            }
            if (!TextUtils.isEmpty(info.getOkTxt(context))) {
                builder.setPositiveButton(info.getOkTxt(context), okListener);
            }

            builder.setCancelable(false);
            needShow = true;
        }

        if (needShow) {
            builder.show();
        }
    }

    public static class DialogInfo {
        private final int msgTxtId;
        private final int titleTxtId;
        private final int okTxtId;
        private final int cancelTxtId;

        private final String msgTxt;
        private final String titleTxt;
        private final String okTxt;
        private final String cancelTxt;

        private DialogInterface.OnClickListener mOkListener;

        private DialogInterface.OnClickListener mCancelListener;

        public DialogInfo(int msgTxtId) {
            this.msgTxtId = msgTxtId;
            this.titleTxtId = R.string.permission_request;
            this.okTxtId = R.string.permission_go_to_settings;
            this.cancelTxtId = R.string.permission_cancel;

            this.msgTxt = null;
            this.titleTxt = null;
            this.okTxt = null;
            this.cancelTxt = null;
        }

        public DialogInfo(String msgTxt) {
            this.msgTxtId = 0;
            this.titleTxtId = R.string.permission_request;
            this.okTxtId = R.string.permission_go_to_settings;
            this.cancelTxtId = R.string.permission_cancel;

            this.msgTxt = msgTxt;
            this.titleTxt = null;
            this.okTxt = null;
            this.cancelTxt = null;
        }

        String getMessage(Context context) {
            return getTxt(context, msgTxtId, msgTxt);
        }

        String getTitleTxt(Context context) {
            return getTxt(context, titleTxtId, titleTxt);
        }

        String getOkTxt(Context context) {
            return getTxt(context, okTxtId, okTxt);
        }

        String getCancelTxt(Context context) {
            return getTxt(context, cancelTxtId, cancelTxt);
        }

        public void setOkListener(DialogInterface.OnClickListener okListener) {
            mOkListener = okListener;
        }

        public DialogInterface.OnClickListener getOkListener() {
            return mOkListener;
        }

        public void setCancelListener(DialogInterface.OnClickListener cancelListener) {
            mCancelListener = cancelListener;
        }

        public DialogInterface.OnClickListener getCancelListener() {
            return mCancelListener;
        }

        private String getTxt(Context context, int id, String txt) {
            if (txt != null) {
                return txt;
            }
            if (id > 0) {
                String appLabel = context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
                return context.getString(id, appLabel);
            }

            return null;
        }
    }
}
