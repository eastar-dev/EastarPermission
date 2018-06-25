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
        String DENY_MESSAGE = "DENY_MESSAGE";
    }

    private ArrayList<String> mRequestedPermissions;//최초 물어본 권한
    private ArrayList<String> mDeniedPermissions = new ArrayList<>();//권한이 없는것들
    private ArrayList<String> mRequestPermissions = new ArrayList<>();//권한을 물어볼대상

    private CharSequence mRequestMessage;
    private CharSequence mDenyMessage;

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

    private boolean step_request;
    private AlertDialog mDlg;

    private void load() {
        final ArrayList<String> requestedPermissions = mRequestedPermissions;

        final ArrayList<String> deniedPermissions = new ArrayList<>();
        for (String permission : requestedPermissions) {
            if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(mContext, permission))
                deniedPermissions.add(permission);
        }
        if (deniedPermissions.size() <= 0) {
            //android.util.Log..("PERMISSIONS", "이미 다 승인됨");
            fireGranted();
            return;
        }
        mDeniedPermissions = deniedPermissions;
        mRequestPermissions = deniedPermissions;
        //android.util.Log..("PERMISSION", "" + step_request);
        if (!step_request) {
            //android.util.Log..("PERMISSIONS", "묻어봄");
            requestPermissions();
            return;
        }

        //android.util.Log..("PERMISSIONS", "거부됨");
        denyPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_REQUEST) {
            load();
        }
    }

    private void requestPermissions() {
        final CharSequence message = mRequestMessage;
        if (message == null || message.length() <= 0) {
            ActivityCompat.requestPermissions(mActivity, mRequestPermissions.toArray(new String[mRequestPermissions.size()]), REQ_REQUEST);
            //android.util.Log..("PERMISSION", "" + step_request);
            step_request = true;
            return;
        }

        if (NoMore.is(mContext, message)) {
            fireDenied();
            step_request = true;
            return;
        }

        mDlg = new AlertDialog.Builder(mContext)//
                .setMessage(message)//
                .setOnCancelListener(dialog -> fireDenied())//
                .setNegativeButton("거부", (dialogInterface, i) -> fireDenied())//
                .setPositiveButton("설정", (dialogInterface, i) -> {
                    ActivityCompat.requestPermissions(mActivity, mRequestPermissions.toArray(new String[mRequestPermissions.size()]), REQ_REQUEST);
                    step_request = true;
                })//
                .setNeutralButton("다시보지않음", (dialogInterface, i) -> NoMore.set(mContext, message))//
                .show();
        mDlg.setCanceledOnTouchOutside(false);
    }

    public void denyPermissions() {
//		Log.l();
        final Context context = mContext;
        final CharSequence message = mDenyMessage;

        if (message == null || message.length() <= 0) {
            fireDenied();
            return;
        }

        mDlg = new AlertDialog.Builder(context)//
                .setMessage(message)//
                .setOnCancelListener(dialog -> fireDenied())//
                .setPositiveButton("거부", (dialogInterface, i) -> fireDenied())//
                .setNegativeButton("설정", (dialogInterface, i) -> {
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
            load();
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
        PermissionObserver.getInstance().notifyObservers(mDeniedPermissions);
        finish();
        overridePendingTransition(0, 0);
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //권한 있는지
    /** instead IS_PERMISSIONS */
    @Deprecated
    public static boolean CHECK_PERMISSIONS(Context context, String... permissions) {
        return IS_PERMISSIONS(context, permissions);
    }
    /** instead IS_PERMISSIONS */
    @Deprecated
    public static boolean CHECK_PERMISSIONS(Context context, List<String> permissions) {
        return IS_PERMISSIONS(context, permissions);
    }
    /** instead IS_PERMISSIONS */
    @Deprecated
    public static boolean CHECK_PERMISSIONS(Context context, ArrayList<String> permissions) {
        return IS_PERMISSIONS(context, permissions);
    }

    public static boolean IS_PERMISSIONS(Context context, String... permissions) {
        return IS_PERMISSIONS(context, Arrays.asList(permissions));
    }
    public static boolean IS_PERMISSIONS(Context context, List<String> permissions) {
        if (permissions == null || permissions.size() <= 0)
            return true;

        for (String permission : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(context, permission))
                return false;
        }
        return true;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //다시보지않기 저장
    public static class NoMore {
        private static final String NAME = "NoMoreSharedPreferences";
        public static boolean is(Context context, CharSequence key) {
            SharedPreferences prefs = context.getSharedPreferences(NAME, MODE_PRIVATE);
            return prefs.getBoolean(md5(key), false);
        }
        private static void set(Context context, CharSequence key) {
            SharedPreferences prefs = context.getSharedPreferences(NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(md5(key), true);
            editor.apply();
        }
        private static String md5(final CharSequence cs) {
            try {
                MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
                digest.update(cs.toString().getBytes());
                byte messageDigest[] = digest.digest();

                // Create Hex String
                StringBuilder hexString = new StringBuilder();
                for (byte aMessageDigest : messageDigest) {
                    String h = Integer.toHexString(0xFF & aMessageDigest);
                    while (h.length() < 2)
                        h = "0" + h;
                    hexString.append(h);
                }
                return hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return "";
        }
    }
}
