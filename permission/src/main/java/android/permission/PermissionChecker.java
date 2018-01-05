package android.permission;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PermissionChecker extends android.support.v7.app.AppCompatActivity {

    private Context mContext;
    private Activity mActivity;

    private static final int REQ_REQUEST = 10;
    private static final int REQ_SETTING = 20;

    public static interface EXTRA {
        String PERMISSIONS = "PERMISSIONS";
        String REQUEST_MESSAGE = "REQUEST_MESSAGE";
        String DENY_MESSAGE = "DENY_MESSAGE";
    }

    private ArrayList<String> mPermissions;
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
    protected void onDestroy() {
        super.onDestroy();
        if (mDlg != null && mDlg.isShowing())
            mDlg.cancel();
    }

    private void parseExtra() {
        final Intent intent = getIntent();
        mPermissions = intent.getStringArrayListExtra(EXTRA.PERMISSIONS);
        mRequestMessage = intent.getCharSequenceExtra(EXTRA.REQUEST_MESSAGE);
        mDenyMessage = intent.getCharSequenceExtra(EXTRA.DENY_MESSAGE);
    }

    private boolean step_request;
    private AlertDialog mDlg;

    private void load() {
        if (check_permissions()) {
            fireGranted();
            return;
        }
        if (!step_request) {
            requestPermissions();
            return;
        }
        denyPermissions();
    }

    public boolean check_permissions() {
        final ArrayList<String> requestPermissions = new ArrayList<>();
        for (String permission : mPermissions) {
            if (needRequestPermissions(mActivity, permission))
                requestPermissions.add(permission);
        }
        mPermissions.clear();
        mPermissions.addAll(requestPermissions);
        return mPermissions.size() <= 0;
    }
    public static boolean CHECK_PERMISSIONS(Activity activity, String... permissions) {
        return CHECK_PERMISSIONS(activity, Arrays.asList(permissions));
    }
    public static boolean CHECK_PERMISSIONS(Activity activity, List<String> permissions) {
        if (permissions == null || permissions.size() <= 0)
            return true;

        final ArrayList<String> requestPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (needRequestPermissions(activity, permission))
                requestPermissions.add(permission);
        }
        return requestPermissions.size() <= 0;
    }
    private static boolean needRequestPermissions(Activity activity, String permission) {
        //이미승인됨
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(activity, permission))
            return false;
        //영구거부됨
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission))
            return false;
        return true;
    }

    private void requestPermissions() {
        final CharSequence message = mRequestMessage;
        if (message == null || message.length() <= 0) {
            ActivityCompat.requestPermissions(mActivity, mPermissions.toArray(new String[mPermissions.size()]), REQ_REQUEST);
            step_request = true;
            return;
        }

        mDlg = new AlertDialog.Builder(mContext)//
                .setMessage(message)//
                .setOnCancelListener(dialog -> fireDenied())//
                .setNegativeButton("거부", (dialogInterface, i) -> fireDenied())//
                .setPositiveButton("설정", (dialogInterface, i) -> {
                    ActivityCompat.requestPermissions(mActivity, mPermissions.toArray(new String[mPermissions.size()]), REQ_REQUEST);
                    step_request = true;
                })//
                .show();
        mDlg.setCanceledOnTouchOutside(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == REQ_REQUEST) {
            final ArrayList<String> deniedPermissions = new ArrayList<String>();
            final int N = permissions.length;
            for (int i = 0; i < N; i++) {
                final String permission = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    deniedPermissions.add(permission);
                }
            }
            mPermissions.clear();
            mPermissions.addAll(deniedPermissions);
            load();
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
        PermissionObserver.getInstance().notifyObservers(mPermissions);
        finish();
        overridePendingTransition(0, 0);
    }

}
