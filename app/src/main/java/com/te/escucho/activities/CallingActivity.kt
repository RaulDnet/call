package com.te.escucho.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.CalendarContract
import android.view.View
import androidx.core.content.ContextCompat
import com.te.escucho.R
import kotlinx.android.synthetic.main.activity_calling.*
import android.net.sip.SipAudioCall
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.net.sip.SipProfile
import android.net.sip.SipManager
import android.net.sip.SipException
import android.net.sip.SipRegistrationListener
import android.content.Intent
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.preference.PreferenceManager
import android.content.SharedPreferences
import android.content.res.Resources
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.te.escucho.services.LinphoneService
import com.te.escucho.services.LinphoneServiceListener
import org.linphone.core.Call
import org.linphone.core.Reason
import java.lang.Exception
import java.text.ParseException
import java.util.*
import kotlin.concurrent.schedule

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class CallingActivity : AppCompatActivity() {

    var manager: SipManager? = null
    var me: SipProfile? = null
    var call: SipAudioCall? = null
    var timer: Int = 0;
    var mAudioManager: AudioManager? = null
    var speakerMode: Boolean = false;
    var muteMode: Boolean = false;
    var wakeLock: PowerManager.WakeLock? = null;
    override fun onBackPressed() {
        LinphoneService.getCall()?.terminate();

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
                LinphoneService.getCall()?.terminate();
                finishCalling()

            }
            R.id.close_app_calling->{
                LinphoneService.getCall()?.terminate();
                finishAffinity();
                System.exit(0);
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_calling)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        speaker.setOnClickListener { toggleSpeaker() }
        mute.setOnClickListener { toggleMute() }
        cancel_call.setOnClickListener {
            LinphoneService.getCall()?.terminate();
            finishCalling()
        }
        mAudioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager;
        LinphoneService.addListener(object : LinphoneServiceListener() {

            override fun onCallConneted() {
                this@CallingActivity.onCallEstablishedEvent()
            }

            override fun onCallEnd() {
                onCallEndEvent()
            }

            override fun onCallRealesed() {
                onCallEndEvent()
            }
        })
        if (LinphoneService.getCall()?.state == Call.State.StreamsRunning) {
            this.onCallEstablishedEvent();
        }
        try {
            val manager: PowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager;
            val field =
                PowerManager::class.java.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
            wakeLock = manager.newWakeLock(field, getLocalClassName());
            wakeLock?.acquire();
        } catch (ignored: Throwable) {
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)


    }

    public override fun onStart() {
        super.onStart()


    }

    public override fun onResume() {
        super.onResume()


    }

    public override fun onDestroy() {
        super.onDestroy()


    }


    fun toggleSpeaker() {

        speakerMode = !speakerMode
        mAudioManager?.setSpeakerphoneOn(speakerMode);
        if (speakerMode) {
            speaker.setBackgroundTintList(
                ContextCompat.getColorStateList(
                    getApplicationContext(),
                    R.color.colorPrimary
                )
            );
        } else {
            speaker.setBackgroundTintList(
                ContextCompat.getColorStateList(
                    getApplicationContext(),
                    R.color.transparent
                )
            );
        }
    }

    fun speakerOff() {
        mAudioManager?.setSpeakerphoneOn(false);
    }

    fun toggleMute() {
        LinphoneService.getCore().enableMic(muteMode)
        muteMode = !muteMode
        if (muteMode) {
            mute.setBackgroundTintList(
                ContextCompat.getColorStateList(
                    getApplicationContext(),
                    R.color.colorPrimary
                )
            );
        } else {
            mute.setBackgroundTintList(
                ContextCompat.getColorStateList(
                    getApplicationContext(),
                    R.color.transparent
                )
            );
        }
    }


    fun onCallEstablishedEvent() {
        state_calling_text.text = """Llamada en progreso..."""
        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                updateTimer()
                mainHandler.postDelayed(this, 1000)
            }
        })
    }


    fun updateTimer() {
        timer++;
        if (timer == 1620){
            try {
                val notification = RingtoneManager . getDefaultUri (RingtoneManager.TYPE_NOTIFICATION);
                val r = RingtoneManager.getRingtone (getApplicationContext(), notification);
                r.play();
            } catch ( e:Exception) {
                e.printStackTrace();
            }
        }
        var mins: String = (timer / 60).toString()
        var seconds: String = (timer % 60).toString()
        mins = if (mins.length == 1) "0$mins" else mins
        seconds = if (seconds.length == 1) "0$seconds" else seconds
        val timeString: String = mins + " : " + seconds
        timer_call.text = timeString;
    }

    fun onCallEndEvent() {
        state_calling_text.text = """Llamada terminada"""
        finishCalling()
    }

    private fun finishCalling() {
        if (!loadCredentials()) {
            LinphoneService.unregister()
            val builder: AlertDialog.Builder? = AlertDialog.Builder(this)
            builder!!.setMessage("Desea llenar una encuesta sobre el servicio?")
                .setTitle("Encuesta")

            builder.apply {
                setPositiveButton("Si") { dialog, id ->
                    val intent2 = Intent();
                    setResult(RESULT_OK, intent2);
                    try {
                        wakeLock?.release();
                    } catch (e: Exception) {

                    }

                    finish()
                    val intent = Intent();
                    intent.action = Intent.ACTION_VIEW;
                    intent.data = Uri.parse("https://docs.google.com/forms/d/1TYoudOdds776BFxLXgt3szexYYTGY7-G_ZtIWhd3nIg/edit?usp=sharing_eip&ts=5e913818")
                    startActivity(intent);
                }
                setNegativeButton("No") { dialog, id ->

                    val intent = Intent();
                    setResult(RESULT_OK, intent);
                    try {
                        wakeLock?.release();
                    } catch (e: Exception) {

                    }

                    finish()
                }
            }
            val dialog: AlertDialog? = builder.create()

            dialog?.show()
        }else{
            val intent = Intent();
            setResult(RESULT_OK, intent);
            try {
                wakeLock?.release();
            } catch (e: Exception) {

            }

            finish()
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




