package android.permission;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

@SuppressWarnings({"unused", "WeakerAccess"})
public class PermissionRequest implements Observer {

    protected Context mContext;
    protected List<String> mPermissions;

    protected OnPermissionGrantedListener mOnPermissionGrantedListener;
    protected OnPermissionDeniedListener mOnPermissionDeniedListener;
    protected CharSequence mRequestMessage;
    protected CharSequence mRequestPositiveButtonText;
    protected CharSequence mRequestNegativeButtonText;
    protected CharSequence mDenyMessage;
    protected CharSequence mDenyPositiveButtonText;
    protected CharSequence mDenyNegativeButtonText;

    public interface OnPermissionGrantedListener {
        void onGranted();
    }

    public interface OnPermissionDeniedListener {
        void onDenied(PermissionRequest request, List<String> deniedPermissions);
    }

    public PermissionRequest(Context context, List<String> permissions) {
        mContext = context;
        mPermissions = permissions;
    }

    @Override
    public void update(Observable observer, Object data) {
        @SuppressWarnings("unchecked") final ArrayList<String> deniedPermissions = (ArrayList<String>) data;

        //android.util.Log..("PERMISSION", "승인상태1:" + deniedPermissions);
        final OnPermissionGrantedListener grantedListener = mOnPermissionGrantedListener;
        final boolean granted = (deniedPermissions == null || deniedPermissions.size() <= 0);
        //android.util.Log..("PERMISSION", "승인상태2:" + granted);
        if (grantedListener != null && granted)
            grantedListener.onGranted();

        final OnPermissionDeniedListener deniedListener = mOnPermissionDeniedListener;
        if (deniedListener != null && !granted)
            deniedListener.onDenied(this, deniedPermissions);

        PermissionObserver.getInstance().deleteObserver(this);
    }

    public boolean run() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            update(null, null); //all granted
            return false;
        }

        final List<String> deniedPermissions = getDeniedPermissions(mContext, mPermissions);
        if (deniedPermissions.size() <= 0) {
            update(null, null); //all granted
            return false;
        }

        PermissionObserver.getInstance().addObserver(this);
        Intent intent = new Intent(mContext, PermissionChecker.class);
        intent.putStringArrayListExtra(PermissionChecker.EXTRA.PERMISSIONS, new ArrayList<>(mPermissions));
        intent.putExtra(PermissionChecker.EXTRA.REQUEST_MESSAGE, mRequestMessage);
        intent.putExtra(PermissionChecker.EXTRA.REQUEST_POSITIVE_BUTTON_TEXT, mRequestPositiveButtonText);
        intent.putExtra(PermissionChecker.EXTRA.REQUEST_NEGATIVE_BUTTON_TEXT, mRequestNegativeButtonText);
        intent.putExtra(PermissionChecker.EXTRA.DENY_MESSAGE, mDenyMessage);
        intent.putExtra(PermissionChecker.EXTRA.DENY_POSITIVE_BUTTON_TEXT, mDenyPositiveButtonText);
        intent.putExtra(PermissionChecker.EXTRA.DENY_NEGATIVE_BUTTON_TEXT, mDenyNegativeButtonText);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        return true;
    }

    //--------------------------------------------------------------------------------------
    @NonNull
    static List<String> getDeniedPermissions(@NonNull Context context, List<String> requestedPermissions) {
        final ArrayList<String> deniedPermissions = new ArrayList<>();
        if (requestedPermissions == null)
            return deniedPermissions;

        for (String permission : requestedPermissions) {
            if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(context, permission))
                deniedPermissions.add(permission);
        }
        return deniedPermissions;
    }

    public static boolean isPermissions(@NonNull Context context, @NonNull String... permissions) {
        for (String permission : permissions) {
            if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(context, permission))
                return false;
        }
        return true;
    }

    public static boolean isIn(@NonNull Context context, @NonNull String... permissions) {
        for (String permission : permissions) {
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, permission))
                return true;
        }
        return false;
    }

    //--------------------------------------------------------------------------------------
    public static Builder builder(Context context, String... permissions) {
        return new Builder(context, Arrays.asList(permissions));
    }

    @SuppressWarnings({"UnusedReturnValue", "WeakerAccess", "unused"})
    public static class Builder {
        PermissionRequest p;

        private Builder(Context context, List<String> permissions) {
            p = new PermissionRequest(context, permissions);
        }

        public Builder setOnPermissionGrantedListener(OnPermissionGrantedListener onPermissionGrantedListener) {
            p.mOnPermissionGrantedListener = onPermissionGrantedListener;
            return this;
        }

        public Builder setOnPermissionDeniedListener(OnPermissionDeniedListener onPermissionDeniedListener) {
            p.mOnPermissionDeniedListener = onPermissionDeniedListener;
            return this;
        }

        public Builder setRequestMessage(CharSequence requestMessage) {
            p.mRequestMessage = requestMessage;
            return this;
        }

        public Builder setRequestPositiveButtonText(CharSequence requestPositiveButtonText) {
            p.mRequestPositiveButtonText = requestPositiveButtonText;
            return this;
        }

        public Builder setRequestNegativeButtonText(CharSequence requestNegativeButtonText) {
            p.mRequestNegativeButtonText = requestNegativeButtonText;
            return this;
        }

        public Builder setDenyMessage(CharSequence denyMessage) {
            p.mDenyMessage = denyMessage;
            return this;
        }

        public Builder setDenyPositiveButtonText(CharSequence denyPositiveButtonText) {
            p.mDenyPositiveButtonText = denyPositiveButtonText;
            return this;
        }

        public Builder setDenyNegativeButtonText(CharSequence denyNegativeButtonText) {
            p.mDenyNegativeButtonText = denyNegativeButtonText;
            return this;
        }

        public Builder setRequestMessage(@StringRes int requestMessage) {
            setRequestMessage(p.mContext.getString(requestMessage));
            return this;
        }

        public Builder setRequestPositiveButtonText(@StringRes int requestPositiveButtonText) {
            setRequestPositiveButtonText(p.mContext.getString(requestPositiveButtonText));
            return this;
        }

        public Builder setRequestNegativeButtonText(@StringRes int requestNegativeButtonText) {
            setRequestNegativeButtonText(p.mContext.getString(requestNegativeButtonText));
            return this;
        }

        public Builder setDenyMessage(@StringRes int denyMessage) {
            setDenyMessage(p.mContext.getString(denyMessage));
            return this;
        }

        public Builder setDenyPositiveButtonText(@StringRes int denyPositiveButtonText) {
            setDenyPositiveButtonText(p.mContext.getString(denyPositiveButtonText));
            return this;
        }

        public Builder setDenyNegativeButtonText(@StringRes int denyNegativeButtonText) {
            setDenyNegativeButtonText(p.mContext.getString(denyNegativeButtonText));
            return this;
        }

        public boolean run() {
            return p.run();
        }

    }
}
