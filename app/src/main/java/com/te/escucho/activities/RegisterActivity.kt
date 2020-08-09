package com.te.escucho.activities

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.te.escucho.R

import kotlinx.android.synthetic.main.activity_register.*
import android.content.SharedPreferences
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.te.escucho.services.LinphoneService
import android.R.attr.orientation

import android.content.res.Configuration
import android.widget.LinearLayout





class RegisterActivity : AppCompatActivity() {
    val editor: SharedPreferences? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val editor = getSharedPreferences("CallHelp", Context.MODE_PRIVATE).edit()
        val reader = getSharedPreferences("CallHelp", Context.MODE_PRIVATE)
        username_text.setText(reader.getString("username",""))
        password_text.setText(reader.getString("password",""))

        button_save.setOnClickListener({
            if (username_text.text.toString().length == 0 || password_text.text.toString().length == 0) {
                Toast.makeText(
                    this@RegisterActivity,
                    "Inicio de sesión para Escuchador",
                    Toast.LENGTH_LONG
                ).show()

            } else {
                editor.putString("username", username_text.text.toString())
                editor.putString("password", password_text.text.toString())
                editor.putBoolean("conected", true)
                editor.apply()
                finish()
                LinphoneService.setCredentials(
                    username_text.text.toString(),
                    password_text.text.toString()
                )
            }
        })
        button_cancel.setOnClickListener({
            finish()
        })

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.delete_acount, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {

            R.id.delete_account -> {
                LinphoneService.deleteUserData()

            }
            R.id.home->{
                finish()
            }
        }
        return true
    }


}
