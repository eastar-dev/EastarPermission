package android.permission;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;

import java.util.Arrays;
import java.util.List;

public class PermissionChecker extends android.support.v7.app.AppCompatActivity {

    private Context mContext;
    private Activity mActivity;

    private static final int REQ_REQUEST = 10;
    private static final int REQ_SETTING = 20;


    public interface EXTRA {
        String PERMISSIONS = "PERMISSIONS";

        String REQUEST_MESSAGE = "REQUEST_MESSAGE";
        String REQUEST_POSITIVE_BUTTON_TEXT = "REQUEST_POSITIVE_BUTTON_TEXT";
        String REQUEST_NEGATIVE_BUTTON_TEXT = "REQUEST_NEGATIVE_BUTTON_TEXT";

        String DENY_MESSAGE = "DENY_MESSAGE";
        String DENY_POSITIVE_BUTTON_TEXT = "DENY_POSITIVE_BUTTON_TEXT";
        String DENY_NEGATIVE_BUTTON_TEXT = "DENY_NEGATIVE_BUTTON_TEXT";
    }

    private List<String> mRequestedPermissions;//최초 물어본 권한

    private CharSequence mRequestMessage;


    private CharSequence mDenyMessage;

    private CharSequence mRequestPositiveButtonText;
    private CharSequence mRequestNegativeButtonText;
    private CharSequence mDenyPositiveButtonText;
    private CharSequence mDenyNegativeButtonText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mActivity = this;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        parseExtra();
        load();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        parseExtra();
        load();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDlg != null && mDlg.isShowing())
            mDlg.cancel();
    }

    private void parseExtra() {
        final Intent intent = getIntent();
        mRequestedPermissions = intent.getStringArrayListExtra(EXTRA.PERMISSIONS);

        mRequestMessage = intent.getCharSequenceExtra(EXTRA.REQUEST_MESSAGE);
        mRequestPositiveButtonText = intent.getCharSequenceExtra(EXTRA.REQUEST_POSITIVE_BUTTON_TEXT);
        mRequestNegativeButtonText = intent.getCharSequenceExtra(EXTRA.REQUEST_NEGATIVE_BUTTON_TEXT);

        mDenyMessage = intent.getCharSequenceExtra(EXTRA.DENY_MESSAGE);
        mDenyPositiveButtonText = intent.getCharSequenceExtra(EXTRA.DENY_POSITIVE_BUTTON_TEXT);
        mDenyNegativeButtonText = intent.getCharSequenceExtra(EXTRA.DENY_NEGATIVE_BUTTON_TEXT);

    }

    private AlertDialog mDlg;

    private void load() {
        final List<String> deniedPermissions = PermissionRequest.getDeniedPermissions(mContext, mRequestedPermissions);

        if (deniedPermissions.size() <= 0)
            throw new UnsupportedOperationException("!!deniedPermissions <= 0");
        android.util.Log.e("PERMISSIONS", "묻어봄" + deniedPermissions);
        requestPermissions(deniedPermissions);
    }


    private void requestPermissions(@NonNull final List<String> deniedPermissions) {
        final CharSequence message = mRequestMessage;
        final String[] permissions = deniedPermissions.toArray(new String[deniedPermissions.size()]);

        android.util.Log.e("PERMISSIONS", "묻어봄" + Arrays.toString(permissions));

        if (message == null || message.length() <= 0) {
            ActivityCompat.requestPermissions(mActivity, permissions, REQ_REQUEST);
            return;
        }

        if (mRequestPositiveButtonText == null || mRequestPositiveButtonText.length() <= 0)
            mRequestPositiveButtonText = "설정";
        if (mRequestNegativeButtonText == null || mRequestNegativeButtonText.length() <= 0)
            mRequestNegativeButtonText = "거부";
        mDlg = new AlertDialog.Builder(mContext)//
                .setMessage(message)//
                .setOnCancelListener(dialog -> fireDenied())//
                .setPositiveButton(mRequestPositiveButtonText, (dialogInterface, i) -> ActivityCompat.requestPermissions(mActivity, deniedPermissions.toArray(new String[deniedPermissions.size()]), REQ_REQUEST))
                .setNegativeButton(mRequestNegativeButtonText, (dialogInterface, i) -> fireDenied())//
                .show();
        mDlg.setCanceledOnTouchOutside(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_REQUEST) {
            final List<String> deniedPermissions = PermissionRequest.getDeniedPermissions(mContext, mRequestedPermissions);
            if (deniedPermissions.size() > 0)
                denyPermissions();
            else
                fireGranted();
        }
    }

    public void denyPermissions() {
//		Log.l();
        final Context context = mContext;
        final CharSequence message = mDenyMessage;

        if (message == null || message.length() <= 0) {
            fireDenied();
            return;
        }

        if (mDenyPositiveButtonText == null || mDenyPositiveButtonText.length() <= 0)
            mDenyPositiveButtonText = "거부";
        if (mDenyNegativeButtonText == null || mDenyNegativeButtonText.length() <= 0)
            mDenyNegativeButtonText = "설정";

        mDlg = new AlertDialog.Builder(context)//
                .setMessage(message)//
                .setOnCancelListener(dialog -> fireDenied())//
                .setPositiveButton(mDenyPositiveButtonText, (dialogInterface, i) -> fireDenied())//
                .setNegativeButton(mDenyNegativeButtonText, (dialogInterface, i) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + context.getPackageName()));
                        startActivityForResult(intent, REQ_SETTING);
                    } catch (ActivityNotFoundException e) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                        startActivityForResult(intent, REQ_SETTING);
                    }
                })//
                .show();
        mDlg.setCanceledOnTouchOutside(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SETTING) {
            final List<String> deniedPermissions = PermissionRequest.getDeniedPermissions(mContext, mRequestedPermissions);
            if (deniedPermissions.size() > 0)
                fireDenied();
            else
                fireGranted();
            return;
        }
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void fireGranted() {
        PermissionObserver.getInstance().notifyObservers();
        finish();
        overridePendingTransition(0, 0);
    }

    private void fireDenied() {
        final List<String> deniedPermissions = PermissionRequest.getDeniedPermissions(mContext, mRequestedPermissions);
        PermissionObserver.getInstance().notifyObservers(deniedPermissions);
        finish();
        overridePendingTransition(0, 0);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void setRequestedOrientation(int requestedOrientation) {
        try {
            super.setRequestedOrientation(requestedOrientation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
