package com.example.djrain.permissiontester

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.permission.PermissionRequest
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Copyright 2016 Eastar Jeong", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }


        simple.setOnClickListener {
            var request = PermissionRequest.builder(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_CALENDAR)
                    .run()

            Toast.makeText(this, "real requested $request", Toast.LENGTH_SHORT).show()
        }

        request_dlg.setOnClickListener {
            var request = PermissionRequest.builder(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_CALENDAR)
                    .setRequestMessage("for contact photo save image")
                    .setRequestPositiveButtonText("OK")
                    .setRequestNegativeButtonText("cancel")
                    .run()

            Toast.makeText(this, "real requested $request", Toast.LENGTH_SHORT).show()
        }
        result_dlg.setOnClickListener {
            var request = PermissionRequest.builder(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_CALENDAR)
                    .setDenyMessage("if u reject permission, u can't use this service\n\nPlase turn on permissions at[Setting] > [Permission]")
                    .setDenyPositiveButtonText("close")
                    .setDenyNegativeButtonText("setting")
                    .run()

            Toast.makeText(this, "real requested $request", Toast.LENGTH_SHORT).show()
        }
        all.setOnClickListener {
            var request = PermissionRequest.builder(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_CALENDAR)
                    .setRequestMessage("for contact photo save image")
                    .setRequestPositiveButtonText("OK")
                    .setRequestNegativeButtonText("cancel")
                    .setDenyMessage("if u reject permission, u can't use this service\n\nPlase turn on permissions at[Setting] > [Permission]")
                    .setDenyPositiveButtonText("close")
                    .setDenyNegativeButtonText("setting")
                    .run()

            Toast.makeText(this, "real requested $request", Toast.LENGTH_SHORT).show()
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
