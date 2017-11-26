package android.permission;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;

public class PermissionRequest implements Observer {

    protected Context mContext;
    protected ArrayList<String> mPermissions;
    @Deprecated
    protected OnPermissionRequestListener mOnPermissionRequestListener;
    protected OnPermissionGrantedListener mOnPermissionGrantedListener;
    protected OnPermissionDeniedListener mOnPermissionDeniedListener;
    protected CharSequence mRequestMessage;
    protected CharSequence mDenyMessage;

    @Deprecated
    public interface OnPermissionRequestListener extends OnPermissionDeniedListener, OnPermissionGrantedListener {
    }

    public interface OnPermissionGrantedListener {
        void onGranted();
    }

    public interface OnPermissionDeniedListener {
        void onDenied(ArrayList<String> deniedPermissions);
    }

    public PermissionRequest(Context context, ArrayList<String> permissions) {
        mContext = context;
        mPermissions = permissions;
    }

    /**
     *
     */
    @Deprecated
    public PermissionRequest(Context context, ArrayList<String> permissions
            , OnPermissionRequestListener onPermissionRequestListener
            , String rquestMessage, String denyMessage) {
        mContext = context;
        mPermissions = permissions;
        mOnPermissionRequestListener = onPermissionRequestListener;
        mRequestMessage = rquestMessage;
        mDenyMessage = denyMessage;
    }

    public PermissionRequest(Context context, ArrayList<String> permissions
            , OnPermissionGrantedListener onPermissionGrantedListener
            , OnPermissionDeniedListener onPermissionDeniedListener
            , String rquestMessage, String denyMessage) {
        mContext = context;
        mPermissions = permissions;
        mOnPermissionGrantedListener = onPermissionGrantedListener;
        mOnPermissionDeniedListener = onPermissionDeniedListener;
        mRequestMessage = rquestMessage;
        mDenyMessage = denyMessage;
    }

    @Override
    public void update(Observable observer, Object data) {
        @SuppressWarnings("unchecked")        final ArrayList<String> deniedPermissions = (ArrayList<String>) data;
        final OnPermissionRequestListener listener = mOnPermissionRequestListener;
        if (listener != null) {
            if ((deniedPermissions == null || deniedPermissions.size() <= 0))
                listener.onGranted();
            else
                listener.onDenied(deniedPermissions);
        }

        final OnPermissionGrantedListener grantedListener = mOnPermissionGrantedListener;
        final boolean granted = deniedPermissions == null || deniedPermissions.size() <= 0;
        if (grantedListener != null && granted)
            grantedListener.onGranted();

        final OnPermissionDeniedListener deniedListener = mOnPermissionDeniedListener;
        if (deniedListener != null && !granted)
            grantedListener.onGranted();

        PermissionObserver.getInstance().deleteObserver(this);
    }

    public void run() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            final OnPermissionRequestListener listener = mOnPermissionRequestListener;
            if (listener != null)
                listener.onGranted();

            final OnPermissionGrantedListener grantedListener = mOnPermissionGrantedListener;
            if (grantedListener != null)
                grantedListener.onGranted();

            return;
        }

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

        @Deprecated
        public Builder setOnPermissionRequestListener(OnPermissionRequestListener onPermissionRequestListener) {
            p.mOnPermissionRequestListener = onPermissionRequestListener;
            return this;
        }

        public Builder setOnPermissionRequestListener(OnPermissionGrantedListener onPermissionGrantedListener) {
            p.mOnPermissionGrantedListener = onPermissionGrantedListener;
            return this;
        }
        public Builder setOnPermissionRequestListener(OnPermissionDeniedListener onPermissionDeniedListener) {
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
