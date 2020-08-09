package com.te.escucho.helpers

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.sip.*;
import android.util.Log;

import android.net.sip.SipProfile
import android.net.sip.SipAudioCall
import com.te.escucho.MainActivity


class IncomingCallReceiver : BroadcastReceiver() {



    override fun onReceive(context: Context?, intent: Intent?) {
        var incomingCall: SipAudioCall? = null
        try {

            val listener = object : SipAudioCall.Listener() {
                override fun onCallEnded(call: SipAudioCall?) {
                    super.onCallEnded(call)
                }

                override fun onRinging(call: SipAudioCall, caller: SipProfile) {
                    try {
                        call.answerCall(30)
                        call.startAudio();
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            }

            val receiverActivity = context as MainActivity
            val incomingCall = receiverActivity.manager?.takeAudioCall(intent, null);
            incomingCall?.setListener(listener, true);
            //todo set code para recibir la llamada


        } catch (e: Exception) {
            incomingCall?.close()
        }

    }

}