package dev.eastar.permission.demo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import dev.eastar.permission.PermissionRequest
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Copyright 2016 eastar Jeong", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        simple.setOnClickListener {
            PermissionRequest.builder(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_CALENDAR)
                    .setOnPermissionGrantedListener { toast("Requested Granted") }
                    .setOnPermissionDeniedListener { _, deniedPermissions -> toast("Requested Denied $deniedPermissions") }
                    .run()
        }

        request_dlg.setOnClickListener {
            PermissionRequest.builder(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_CALENDAR)
                    .setOnPermissionGrantedListener { toast("Requested Granted") }
                    .setOnPermissionDeniedListener { _, deniedPermissions -> toast("Requested Denied $deniedPermissions") }
                    .setRequestMessage("for contact photo save image")
                    .setRequestPositiveButtonText("OK")
                    .setRequestNegativeButtonText("cancel")
                    .run()
        }

        result_dlg.setOnClickListener {
            PermissionRequest.builder(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_CALENDAR)
                    .setOnPermissionGrantedListener { toast("Requested Granted") }
                    .setOnPermissionDeniedListener { _, deniedPermissions -> toast("Requested Denied $deniedPermissions") }
                    .setDenyMessage("if u reject permission, u can't use this service\n\nPlase turn on permissions at[Setting] > [Permission]")
                    .setDenyPositiveButtonText("close")
                    .setDenyNegativeButtonText("setting")
                    .run()
        }

        all.setOnClickListener {
            PermissionRequest.builder(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_CALENDAR)
                    .setOnPermissionGrantedListener { toast("Requested Granted") }
                    .setOnPermissionDeniedListener { _, deniedPermissions -> toast("Requested Denied $deniedPermissions") }
                    .setRequestMessage("for contact photo save image")
                    .setRequestPositiveButtonText("OK")
                    .setRequestNegativeButtonText("cancel")
                    .setDenyMessage("if u reject permission, u can't use this service\n\nPlase turn on permissions at[Setting] > [Permission]")
                    .setDenyPositiveButtonText("close")
                    .setDenyNegativeButtonText("setting")
                    .run()

            setting.setOnClickListener { setting() }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

fun Context.toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

fun Context.setting() = try {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
} catch (e: Exception) {
    e.printStackTrace()
}