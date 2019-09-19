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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import java.util.*

class PermissionRequest(var context: Context, var permissions: List<String>) : Observer {

    var onGranted: (() -> Unit)? = null
    var onDenied: ((request: PermissionRequest, deniedPermissions: List<String>) -> Unit)? = null
    lateinit var mRequestMessage: CharSequence
    lateinit var mRequestPositiveButtonText: CharSequence
    lateinit var mRequestNegativeButtonText: CharSequence
    lateinit var mDenyMessage: CharSequence
    lateinit var mDenyPositiveButtonText: CharSequence
    lateinit var mDenyNegativeButtonText: CharSequence

    override fun update(o: Observable?, arg: Any?) {
        PermissionObserver.deleteObserver(this)

        @Suppress("UNCHECKED_CAST")
        val deniedPermissions = arg as? List<String>
        if (deniedPermissions.isNullOrEmpty())
            onGranted?.invoke()
        else
            onDenied?.invoke(this, deniedPermissions)
    }

    fun run() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onGranted?.invoke()
            return
        }

        val deniedPermissions = getDeniedPermissions(context, permissions)
        if (deniedPermissions.isEmpty()) {
            onGranted?.invoke()
            return
        }

        PermissionObserver.addObserver(this)
        val intent = Intent(context, PermissionChecker::class.java)
        intent.putStringArrayListExtra(PermissionChecker.EXTRA.PERMISSIONS, ArrayList(permissions))
        intent.putExtra(PermissionChecker.EXTRA.REQUEST_MESSAGE, mRequestMessage)
        intent.putExtra(PermissionChecker.EXTRA.REQUEST_POSITIVE_BUTTON_TEXT, mRequestPositiveButtonText)
        intent.putExtra(PermissionChecker.EXTRA.REQUEST_NEGATIVE_BUTTON_TEXT, mRequestNegativeButtonText)
        intent.putExtra(PermissionChecker.EXTRA.DENY_MESSAGE, mDenyMessage)
        intent.putExtra(PermissionChecker.EXTRA.DENY_POSITIVE_BUTTON_TEXT, mDenyPositiveButtonText)
        intent.putExtra(PermissionChecker.EXTRA.DENY_NEGATIVE_BUTTON_TEXT, mDenyNegativeButtonText)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    class Builder internal constructor(context: Context, permissions: List<String>) {
        private var p: PermissionRequest = PermissionRequest(context, permissions)

        fun setOnPermissionGrantedListener(onGranted: (() -> Unit)?): Builder {
            p.onGranted = onGranted
            return this
        }

        fun setOnPermissionDeniedListener(onDenied: ((request: PermissionRequest, deniedPermissions: List<String>) -> Unit)?): Builder {
            p.onDenied = onDenied
            return this
        }

        fun setRequestMessage(requestMessage: CharSequence): Builder {
            p.mRequestMessage = requestMessage
            return this
        }

        fun setRequestPositiveButtonText(requestPositiveButtonText: CharSequence): Builder {
            p.mRequestPositiveButtonText = requestPositiveButtonText
            return this
        }

        fun setRequestNegativeButtonText(requestNegativeButtonText: CharSequence): Builder {
            p.mRequestNegativeButtonText = requestNegativeButtonText
            return this
        }

        fun setDenyMessage(denyMessage: CharSequence): Builder {
            p.mDenyMessage = denyMessage
            return this
        }

        fun setDenyPositiveButtonText(denyPositiveButtonText: CharSequence): Builder {
            p.mDenyPositiveButtonText = denyPositiveButtonText
            return this
        }

        fun setDenyNegativeButtonText(denyNegativeButtonText: CharSequence): Builder {
            p.mDenyNegativeButtonText = denyNegativeButtonText
            return this
        }

        fun setRequestMessage(@StringRes requestMessage: Int): Builder {
            setRequestMessage(p.context.getString(requestMessage))
            return this
        }

        fun setRequestPositiveButtonText(@StringRes requestPositiveButtonText: Int): Builder {
            setRequestPositiveButtonText(p.context.getString(requestPositiveButtonText))
            return this
        }

        fun setRequestNegativeButtonText(@StringRes requestNegativeButtonText: Int): Builder {
            setRequestNegativeButtonText(p.context.getString(requestNegativeButtonText))
            return this
        }

        fun setDenyMessage(@StringRes denyMessage: Int): Builder {
            setDenyMessage(p.context.getString(denyMessage))
            return this
        }

        fun setDenyPositiveButtonText(@StringRes denyPositiveButtonText: Int): Builder {
            setDenyPositiveButtonText(p.context.getString(denyPositiveButtonText))
            return this
        }

        fun setDenyNegativeButtonText(@StringRes denyNegativeButtonText: Int): Builder {
            setDenyNegativeButtonText(p.context.getString(denyNegativeButtonText))
            return this
        }

        fun run() {
            p.run()
        }
    }

    companion object {
        internal fun getDeniedPermissions(context: Context, requestedPermissions: List<String>): List<String> {
            return requestedPermissions.filter { PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(context, it) }
        }

        fun isPermissions(context: Context, vararg permissions: String): Boolean {
            permissions.forEach {
                if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(context, it))
                    return false
            }
            return true
        }

        fun isIn(context: Context, vararg permissions: String): Boolean {
            permissions.forEach {
                if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, it))
                    return true
            }
            return false
        }

        //--------------------------------------------------------------------------------------
        fun builder(context: Context, vararg permissions: String): Builder {
            return Builder(context, listOf(*permissions))
        }
    }
}
