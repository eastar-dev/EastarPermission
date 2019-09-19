/*
 * Copyright 2016 copyright eastar Jeong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.eastar.permission

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat

class PermissionChecker : androidx.appcompat.app.AppCompatActivity() {
    private var mRequestedPermissions: List<String> = emptyList()//최초 물어본 권한
    private var mRequestMessage: CharSequence? = null
    private var mDenyMessage: CharSequence? = null
    private var mRequestPositiveButtonText: CharSequence? = null
    private var mRequestNegativeButtonText: CharSequence? = null
    private var mDenyPositiveButtonText: CharSequence? = null
    private var mDenyNegativeButtonText: CharSequence? = null

    private var dlg: AlertDialog? = null

    interface EXTRA {
        companion object {
            const val PERMISSIONS = "PERMISSIONS"

            const val REQUEST_MESSAGE = "REQUEST_MESSAGE"
            const val REQUEST_POSITIVE_BUTTON_TEXT = "REQUEST_POSITIVE_BUTTON_TEXT"
            const val REQUEST_NEGATIVE_BUTTON_TEXT = "REQUEST_NEGATIVE_BUTTON_TEXT"

            const val DENY_MESSAGE = "DENY_MESSAGE"
            const val DENY_POSITIVE_BUTTON_TEXT = "DENY_POSITIVE_BUTTON_TEXT"
            const val DENY_NEGATIVE_BUTTON_TEXT = "DENY_NEGATIVE_BUTTON_TEXT"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        parseExtra()
        load()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseExtra()
        load()
    }

    override fun onDestroy() {
        super.onDestroy()
        dlg?.cancel()
        PermissionObserver.deleteObservers()
    }

    private fun parseExtra() {
        mRequestedPermissions = intent.getStringArrayListExtra(EXTRA.PERMISSIONS) ?: emptyList()

        mRequestMessage = intent.getCharSequenceExtra(EXTRA.REQUEST_MESSAGE)
        mRequestPositiveButtonText = intent.getCharSequenceExtra(EXTRA.REQUEST_POSITIVE_BUTTON_TEXT)
        mRequestNegativeButtonText = intent.getCharSequenceExtra(EXTRA.REQUEST_NEGATIVE_BUTTON_TEXT)

        mDenyMessage = intent.getCharSequenceExtra(EXTRA.DENY_MESSAGE)
        mDenyPositiveButtonText = intent.getCharSequenceExtra(EXTRA.DENY_POSITIVE_BUTTON_TEXT)
        mDenyNegativeButtonText = intent.getCharSequenceExtra(EXTRA.DENY_NEGATIVE_BUTTON_TEXT)
    }

    private fun load() {
        val deniedPermissions = PermissionRequest.getDeniedPermissions(this, mRequestedPermissions)
        if (deniedPermissions.isEmpty()) {
            fireGranted()
            return
        }
        requestPermissions(deniedPermissions)
    }

    private fun requestPermissions(deniedPermissions: List<String>) {
        val message = mRequestMessage
        val permissions = deniedPermissions.toTypedArray()

        if (message.isNullOrBlank()) {
            ActivityCompat.requestPermissions(this, permissions, REQ_REQUEST)
            return
        }

        val context = this
        if (mRequestPositiveButtonText.isNullOrBlank())
            mRequestPositiveButtonText = getString(R.string.permission_confirm)
        if (mRequestNegativeButtonText.isNullOrBlank())
            mRequestNegativeButtonText = getString(R.string.permission_close)
        dlg = AlertDialog.Builder(context)
                .setMessage(message)
                .setOnCancelListener { fireDenied() }
                .setPositiveButton(mRequestPositiveButtonText) { _, _ -> ActivityCompat.requestPermissions(this, deniedPermissions.toTypedArray(), REQ_REQUEST) }
                .setNegativeButton(mRequestNegativeButtonText) { _, _ -> fireDenied() }
                .show()
        dlg?.setCanceledOnTouchOutside(false)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQ_REQUEST) {
            val deniedPermissions = PermissionRequest.getDeniedPermissions(this, mRequestedPermissions)
            if (deniedPermissions.isNotEmpty())
                denyPermissions()
            else
                fireGranted()
        }
    }

    private fun denyPermissions() {
        val context = this
        val message = mDenyMessage

        if (message.isNullOrBlank()) {
            fireDenied()
            return
        }

        if (mDenyPositiveButtonText.isNullOrBlank())
            mDenyPositiveButtonText = getString(R.string.permission_close)
        if (mDenyNegativeButtonText.isNullOrBlank())
            mDenyNegativeButtonText = getString(R.string.permission_setting)

        dlg = AlertDialog.Builder(context)
                .setMessage(message)
                .setOnCancelListener { fireDenied() }
                .setPositiveButton(mDenyPositiveButtonText) { _, _ -> fireDenied() }
                .setNegativeButton(mDenyNegativeButtonText) { _, _ ->
                    try {
                        startActivityForResult(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + context.packageName)), REQ_SETTING)
                    } catch (e: ActivityNotFoundException) {
                        startActivityForResult(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS), REQ_SETTING)
                    }
                }
                .show()
        dlg?.setCanceledOnTouchOutside(false)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SETTING) {
            val deniedPermissions = PermissionRequest.getDeniedPermissions(this, mRequestedPermissions)
            if (deniedPermissions.isNotEmpty())
                fireDenied()
            else
                fireGranted()
            return
        }
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun fireGranted() {
        PermissionObserver.notifyObservers()
        finish()
        overridePendingTransition(0, 0)
    }

    private fun fireDenied() {
        val deniedPermissions = PermissionRequest.getDeniedPermissions(this, mRequestedPermissions)
        PermissionObserver.notifyObservers(deniedPermissions)
        finish()
        overridePendingTransition(0, 0)
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    override fun setRequestedOrientation(requestedOrientation: Int) {
        try {
            super.setRequestedOrientation(requestedOrientation)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val REQ_REQUEST = 10
        private const val REQ_SETTING = 20
    }
}
