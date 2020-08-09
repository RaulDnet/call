package com.te.escucho.activities

import android.content.Context
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.te.escucho.R

import kotlinx.android.synthetic.main.activity_about_register.*
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.content_about_register.*

class AboutRegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_register)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        close_terms_reg.setOnClickListener {
            closeApp()
        }
        accept_terms_reg.setOnClickListener{
            acceptTerms()
        }

    }
    override fun onBackPressed() {
        closeApp()
    }
    fun closeApp(){
        finishAffinity();
        System.exit(0);
    }
    fun acceptTerms(){
        val editor = getSharedPreferences("CallHelp", Context.MODE_PRIVATE).edit()
        editor.putBoolean("first", true)
        editor.apply()
        finish()
    }
}
