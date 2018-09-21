package android.permission;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;

public class PermissionRequest implements Observer {

    protected Context mContext;
    protected ArrayList<String> mPermissions;

    protected OnPermissionGrantedListener mOnPermissionGrantedListener;
    protected OnPermissionDeniedListener mOnPermissionDeniedListener;
    protected CharSequence mRequestMessage;
    protected CharSequence mDenyMessage;

    public interface OnPermissionGrantedListener {
        void onGranted();
    }

    public interface OnPermissionDeniedListener {
        void onDenied(PermissionRequest request, ArrayList<String> deniedPermissions);
    }

    public PermissionRequest(Context context, ArrayList<String> permissions) {
        mContext = context;
        mPermissions = permissions;
    }

    public ArrayList<String> getRequestPermissions() {
        return mPermissions;
    }

    public PermissionRequest(Context context, ArrayList<String> permissions, OnPermissionGrantedListener onPermissionGrantedListener, OnPermissionDeniedListener onPermissionDeniedListener, String rquestMessage, String denyMessage) {
        mContext = context;
        mPermissions = permissions;
        mOnPermissionGrantedListener = onPermissionGrantedListener;
        mOnPermissionDeniedListener = onPermissionDeniedListener;
        mRequestMessage = rquestMessage;
        mDenyMessage = denyMessage;
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

    public void run() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && mOnPermissionGrantedListener != null)
            mOnPermissionGrantedListener.onGranted();


        final ArrayList<String> deniedPermissions = new ArrayList<>();
        for (String permission : mPermissions) {
            if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(mContext, permission))
                deniedPermissions.add(permission);
        }

        if (deniedPermissions.size() <= 0 && mOnPermissionGrantedListener != null)
            mOnPermissionGrantedListener.onGranted();

        PermissionObserver.getInstance().addObserver(this);
        Intent intent = new Intent(mContext, PermissionChecker.class);
        intent.putExtra(PermissionChecker.EXTRA.PERMISSIONS, mPermissions);
        intent.putExtra(PermissionChecker.EXTRA.REQUEST_MESSAGE, mRequestMessage);
        intent.putExtra(PermissionChecker.EXTRA.DENY_MESSAGE, mDenyMessage);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    public static class Builder {
        PermissionRequest p;

        public Builder(Context context, ArrayList<String> permissions) {
            p = new PermissionRequest(context, permissions);
        }

        public Builder(Context context, String... permissions) {
            this(context, new ArrayList<String>(Arrays.asList(permissions)));
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

        public Builder setDenyMessage(CharSequence denyMessage) {
            p.mDenyMessage = denyMessage;
            return this;
        }

        public void run() {
            p.run();
        }
    }
}
