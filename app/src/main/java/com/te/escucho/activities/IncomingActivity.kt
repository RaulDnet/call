package com.te.escucho.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.te.escucho.R
import com.te.escucho.services.LinphoneService

import kotlinx.android.synthetic.main.activity_incoming.*
import org.linphone.core.Call
import org.linphone.core.CallParams
import org.linphone.core.Reason
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.view.Menu
import android.widget.Toast


import android.view.MenuItem
import com.te.escucho.services.LinphoneServiceListener


class IncomingActivity : AppCompatActivity() {

    var call:Call?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
         call = LinphoneService.getCall()

        reject_call.setOnClickListener {
            call?.decline(Reason.Declined)
            finish()
        }
        accept_call.setOnClickListener {
            val intent = Intent(this, CallingActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivityForResult(intent, 0)
            val params: CallParams = LinphoneService.getCore().createCallParams(call);
            params.enableVideo(false);
            var mAudioManager : AudioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager;
            mAudioManager?.setSpeakerphoneOn(false);
            call?.acceptWithParams(params);
        }
        LinphoneService.addListener(object:LinphoneServiceListener(){
            override fun onCallRealesed() {
                finish();
            }
        })

    }

    override fun onBackPressed() {
        call?.decline(Reason.Declined)
        finish()

    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.

        if (loadCredentials()) {
            menuInflater.inflate(R.menu.calling, menu)
        }
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                call?.decline(Reason.Declined)
                finish()
            }
            R.id.close_app_calling->{
                call?.decline(Reason.Declined)
                finishAffinity();
                System.exit(0);
            }
        }
        return true
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK){
            finish();
        }
    }


    fun loadCredentials(): Boolean {
        val prefs: SharedPreferences = getSharedPreferences("CallHelp", MODE_PRIVATE);
        val name: String? = prefs.getString("username", "")
        val pass: String? = prefs.getString("password", "")

        if (name != null && name.length > 0 && pass != null && pass.length > 0) {
            return true;
        }
        return false

    }
}
