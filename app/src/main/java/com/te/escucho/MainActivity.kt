package com.te.escucho

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.net.sip.*

import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.net.sip.SipProfile
import android.content.SharedPreferences
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.media.RingtoneManager
import androidx.core.content.ContextCompat

import android.net.sip.SipAudioCall
import android.os.Build
import android.os.Process
import android.text.Layout
import android.widget.LinearLayout
import android.widget.Toast
import com.te.escucho.activities.AboutAnonimousActivity
import com.te.escucho.activities.AboutRegisterActivity
import com.te.escucho.activities.Register2Activity
import com.te.escucho.activities.RegisterActivity
import com.te.escucho.helpers.IncomingCallReceiver
import com.te.escucho.helpers.User
import com.te.escucho.services.LinphoneService
import com.te.escucho.services.LinphoneServiceListener
import kotlinx.android.synthetic.main.activity_register.*


class MainActivity : AppCompatActivity() {

    private var fromcalling: Boolean = false
    private var conected: Boolean = false;
    private var REQUEST_SIP = 1
    var me: SipProfile? = null
    var manager: SipManager? = null
    var call: SipAudioCall? = null
    var listener: LinphoneServiceListener? = null
    val centerNumber: String = "900"
    var username: String? = null
    var password: String? = null

    var conectMenuItem: MenuItem? = null
    var disconectMenuItem: MenuItem? = null
    var closeItem: MenuItem? = null
    var credentialsItem: MenuItem? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val reader = getSharedPreferences("CallHelp", Context.MODE_PRIVATE)
        if (!reader.getBoolean("first", false)) {
            var intent = Intent(this, AboutAnonimousActivity::class.java)
            if (loadCredentials()) {
                intent = Intent(this, AboutRegisterActivity::class.java)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return
        }

        val callReceiver: IncomingCallReceiver;
        initiate_call.setOnClickListener({
            if (!LinphoneService.isReady()) {

                startService(Intent().setClass(this, LinphoneService::class.java));
                fromcalling = true;
            }
        })

        listener = object : LinphoneServiceListener() {
            override fun onCreated() {


                if (loadCredentials()) {
                    appbar_status.visibility = LinearLayout.VISIBLE
                    if (isConected()) {
                        LinphoneService.setCredentials(username, password);
                    }
                    updateMenuItem()
                }
                this@MainActivity.initiate_call.setOnClickListener {
                    if (LinphoneService.registerStatus == 2)
                        LinphoneService.getInstance().CallTo(centerNumber)
                    else {
                        LinphoneService.setCredentialsAnonimous(getUserNameForCall())
                    }
                }
                if (fromcalling) {
                    fromcalling = false;
                    LinphoneService.setCredentialsAnonimous(getUserNameForCall())
                }
            }

            override fun onRegisterOK() {
                if (!LinphoneService.hasUserData()) {
                    LinphoneService.getInstance().CallTo(centerNumber)
                } else {
                    setConected(true);
                    uiConectado()
                    updateMenuItem()
                }
            }

            override fun onRegisterFailed() {
                if (LinphoneService.hasUserData()) {
                    uiDesconectado()
                    setConected(false)
                    updateMenuItem()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Lo Sentimos, no se pudo comunicar",
                        Toast.LENGTH_LONG
                    ).show()
                }

            }

            override fun onRegisterProgress() {
                if (LinphoneService.hasUserData()) {
                    uiConectando()

                    hideMenuItems()
                }
            }

            override fun onCredentialsSaved(u: String?, p: String?) {
                if (!LinphoneService.isReady() && loadCredentials()) {
                    initializeManager()
                } else {
                    this@MainActivity.username = u
                    this@MainActivity.password = p
                    appbar_status.visibility = LinearLayout.VISIBLE
                }
                closeItem?.setVisible(true);
                credentialsItem?.setVisible(false);
            }

            override fun onUserDataDeleted() {
                setConected(false);
                removeCredentials()
                appbar_status.visibility = LinearLayout.GONE
            }
        }

        LinphoneService.addListener(listener);

    }

    override fun onStart() {
        super.onStart()
        permisions_check();
    }

    private fun hideMenuItems() {
        conectMenuItem?.setVisible(false)
        disconectMenuItem?.setVisible(false)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        conectMenuItem = menu.findItem(R.id.action_conect);
        disconectMenuItem = menu.findItem(R.id.action_disconect);
        credentialsItem = menu.findItem(R.id.action_settings);
        closeItem = menu.findItem(R.id.close_app_main);
        if (loadCredentials()) {
            updateMenuItem()
            credentialsItem?.setVisible(false);
            closeItem?.setVisible(true);
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, Register2Activity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            R.id.action_about -> {
                var intent = Intent(this, AboutAnonimousActivity::class.java)
                if (loadCredentials()) {
                    intent = Intent(this, AboutRegisterActivity::class.java)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            R.id.action_conect -> {
                if (loadCredentials()) {
                    LinphoneService.setCredentials(username, password);
                }
            }
            R.id.action_disconect -> {
                LinphoneService.unregister()

            }
            R.id.close_app_main -> {
                //Process.killProcess(Process.myPid())
                finishAffinity()
                System.exit(1);
            }
//            R.id.test -> {
//                dtry {
//                    val notification =
//                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//                    val r = RingtoneManager.getRingtone(getApplicationContext(), notification);
//                    r.play();
//                } catch (e: Exception) {
//                    e.printStackTrace();
//                }
//            }
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }


    fun permisions_check() {
        val permisions = arrayOf(
            Manifest.permission.USE_SIP,
            Manifest.permission.INTERNET,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.VIBRATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE

        )
        var no_allowed = arrayListOf<String>()
        for (item in permisions) {
            val permissionCheck =
                ContextCompat.checkSelfPermission(this, item)

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                no_allowed.add(item);
            }
        }
        if (no_allowed.size > 0) {
            ActivityCompat.requestPermissions(
                this,
                no_allowed.toTypedArray(),
                REQUEST_SIP
            )
        } else {
            initializeManager()
        }


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_SIP -> if (!(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                finishAffinity()
            }

        }
        initializeManager()
    }

    fun initializeManager() {

        LinphoneService.addListener(listener);
        if (loadCredentials()) {
            hideBtnCall()
            ContextCompat.startForegroundService(
                this,
                Intent().setClass(this, LinphoneService::class.java)
            );
        }


        return

    }

    fun loadCredentials(): Boolean {
        val prefs: SharedPreferences = getSharedPreferences("CallHelp", MODE_PRIVATE);
        val name: String? = prefs.getString("username", "")
        val pass: String? = prefs.getString("password", "")

        if (name != null && name.length > 0 && pass != null && pass.length > 0) {
            username = name;
            password = pass;
            return true;
        }
        return false

    }

    fun uiDesconectado() {
        appbar_status_text.text = """Desconectado""";
        appbar_status_image.setImageDrawable(resources.getDrawable(R.drawable.dot_red))
    }

    fun uiConectando() {
        appbar_status_text.text = """Conectando""";
        appbar_status_image.setImageDrawable(resources.getDrawable(R.drawable.dot_yellow))
    }

    fun uiConectado() {
        appbar_status_text.text = """Conectado""";
        appbar_status_image.setImageDrawable(resources.getDrawable(R.drawable.dot_green))
    }

    fun isConected(): Boolean {
        val prefs: SharedPreferences = getSharedPreferences("CallHelp", MODE_PRIVATE);
        conected = prefs.getBoolean("conected", false)
        return conected
    }

    fun setConected(v: Boolean) {
        val editor = getSharedPreferences("CallHelp", MODE_PRIVATE).edit()
        editor.putBoolean("conected", v)
        editor.apply()
        conected = v

    }

    fun removeCredentials() {
        val editor = getSharedPreferences("CallHelp", MODE_PRIVATE).edit()
        editor.putString("username", null)
        editor.putString("password", null)
        editor.apply()

    }

    fun updateMenuItem() {
        conectMenuItem?.setVisible(!conected);
        disconectMenuItem?.setVisible(conected);
    }

    fun getUserNameForCall(): User {
        if (loadCredentials()) {
            return User(username, password)
        } else {
            val u: Int = (16..51).random()
            val p = "123456"

            return User(u.toString().padStart(3, '0'), p)
        }
    }

    fun hideBtnCall() {
        initite_call_container.visibility = LinearLayout.GONE;
        warning_anonimous.visibility = LinearLayout.GONE
        warning_register.visibility = LinearLayout.VISIBLE
    }

}
