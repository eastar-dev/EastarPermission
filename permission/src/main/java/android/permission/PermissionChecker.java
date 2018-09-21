package android.permission;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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

    private ArrayList<String> mRequestedPermissions;//최초 물어본 권한
    private ArrayList<String> mDeniedPermissions = new ArrayList<>();//권한이 없는것들

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
        mDenyMessage = intent.getCharSequenceExtra(EXTRA.DENY_MESSAGE);
    }

    private AlertDialog mDlg;

    private void load() {
        final ArrayList<String> deniedPermissions = getDeniedPermissions(mContext, mRequestedPermissions);

        if (deniedPermissions.size() <= 0)
            throw new UnsupportedOperationException("!!deniedPermissions <= 0");
        //android.util.Log..("PERMISSIONS", "묻어봄");
        requestPermissions(deniedPermissions);
    }


    private void requestPermissions(@NonNull final ArrayList<String> deniedPermissions) {
        final CharSequence message = mRequestMessage;
        if (message == null || message.length() <= 0) {
            ActivityCompat.requestPermissions(mActivity, deniedPermissions.toArray(new String[deniedPermissions.size()]), REQ_REQUEST);
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
            final ArrayList<String> deniedPermissions = getDeniedPermissions(mContext, mRequestedPermissions);
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

        final ArrayList<String> deniedPermissions = getDeniedPermissions(mContext, mRequestedPermissions);

        if (mDenyPositiveButtonText == null || mDenyPositiveButtonText.length() <= 0)
            mDenyPositiveButtonText = "설정";
        if (mDenyNegativeButtonText == null || mDenyNegativeButtonText.length() <= 0)
            mDenyNegativeButtonText = "거부";

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
            final ArrayList<String> deniedPermissions = getDeniedPermissions(mContext, mRequestedPermissions);
            if (deniedPermissions.size() > 0)
                denyPermissions();
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
        final ArrayList<String> deniedPermissions = getDeniedPermissions(mContext, mRequestedPermissions);
        PermissionObserver.getInstance().notifyObservers(deniedPermissions);
        finish();
        overridePendingTransition(0, 0);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //권한 있는지
    @NonNull
    private static ArrayList<String> getDeniedPermissions(Context
                                                                  context, List<String> requestedPermissions) {
        final ArrayList<String> deniedPermissions = new ArrayList<>();
        if (requestedPermissions == null)
            return deniedPermissions;

        for (String permission : requestedPermissions) {
            if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(context, permission))
                deniedPermissions.add(permission);
        }
        return deniedPermissions;
    }

    public static boolean hasDeniedPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(context, permission))
                return true;
        }
        return false;
    }

    public static boolean hasDeniedPermissions(Context context, List<String> permissions) {
        for (String permission : permissions) {
            if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(context, permission))
                return true;
        }
        return false;
    }
}
