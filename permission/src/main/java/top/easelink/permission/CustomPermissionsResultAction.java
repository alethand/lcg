package top.easelink.permission;

import android.os.Looper;

import androidx.annotation.NonNull;

public abstract class CustomPermissionsResultAction extends PermissionsResultAction {

    public CustomPermissionsResultAction() {}

    public CustomPermissionsResultAction(@NonNull Looper looper) {
        super(looper);
    }

    /**
     * 自定义action,可用于处理被永久拒绝的permissions
     */
    public abstract void onCustomAction(String[] permissions);
}
