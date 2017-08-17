package android.permission;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
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

public class PermissionChecker extends android.support.v7.app.AppCompatActivity {

    private Context mContext;
    private Activity mActivity;

    private static final int REQ_REQUEST = 10;
    private static final int REQ_SETTING = 20;
//	private static final int REQ_SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST = 30;
//	private static final int REQ_SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_SETTING = 31;

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
//	private void requestWindowPermission() {
////		if (permission.equals(Manifest.permission.SYSTEM_ALERT_WINDOW) && !Settings.canDrawOverlays(mContext))
////			needPermissions.add(permission);
//
//		final Context context = mContext;
//		Uri uri = Uri.fromParts("package", context.getPackageName(), null);
//		final Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri);
//		new AlertDialog.Builder(context)//
//				.setMessage(mRequestMessage)//
//				.setCancelable(false)//
//				.setPositiveButton("설정", new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialogInterface, int i) {
//						startActivityForResult(intent, REQ_SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST);
//					}
//				}).show();
//	}

    private boolean step_request;
    private AlertDialog mDlg;

    private void load() {

        if (CHECK_PERMISSIONS(mContext, mPermissions)) {
            fireGranted();
            return;
        }

        if (!step_request) {
            requestPermissions();
            return;
        }

        denyPermissions();
    }

    public static boolean CHECK_PERMISSIONS(Context context, String... permissions) {
        return CHECK_PERMISSIONS(context, new ArrayList<String>(Arrays.asList(permissions)));
    }

    public static boolean CHECK_PERMISSIONS(Context context, ArrayList<String> permissions) {
        if (permissions == null || permissions.size() <= 0)
            return true;

        final ArrayList<String> needPermissions = new ArrayList<String>();
        for (String permission : permissions) {
            int result = ContextCompat.checkSelfPermission(context, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                needPermissions.add(permission);
            }
        }

        permissions.clear();
        permissions.addAll(needPermissions);
        return permissions.size() <= 0;
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
                .setNegativeButton("거부", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        fireDenied();
                    }
                })//
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        fireDenied();
                    }
                })//
                .setPositiveButton("설정", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(mActivity, mPermissions.toArray(new String[mPermissions.size()]), REQ_REQUEST);
                        step_request = true;
                    }
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
                .setPositiveButton("거부", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        fireDenied();
                    }
                })//
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        fireDenied();
                    }
                })//
                .setNegativeButton("설정", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + context.getPackageName()));
                            startActivityForResult(intent, REQ_SETTING);
                        } catch (ActivityNotFoundException e) {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                            startActivityForResult(intent, REQ_SETTING);
                        }
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
