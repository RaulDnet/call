package com.te.escucho.activities

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.te.escucho.R
import com.te.escucho.services.LinphoneService
import kotlinx.android.synthetic.main.activity_register.*

import kotlinx.android.synthetic.main.activity_register2.*


class Register2Activity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register2)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        val editor = getSharedPreferences("CallHelp", Context.MODE_PRIVATE).edit()
        val reader = getSharedPreferences("CallHelp", Context.MODE_PRIVATE)
        username_text.setText(reader.getString("username",""))
        password_text.setText(reader.getString("password",""))

        button_save.setOnClickListener({
            if (username_text.text.toString().length == 0 || password_text.text.toString().length == 0) {
                Toast.makeText(
                    this@Register2Activity,
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



}
