package com.te.escucho.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.te.escucho.helpers.User;
import com.te.escucho.MainActivity;
import com.te.escucho.R;
import com.te.escucho.activities.CallingActivity;
import com.te.escucho.activities.IncomingActivity;

import org.linphone.core.AccountCreator;
import org.linphone.core.Address;
import org.linphone.core.Call;
import org.linphone.core.CallParams;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.Factory;
import org.linphone.core.LogCollectionState;

import org.linphone.core.ProxyConfig;
import org.linphone.core.RegistrationState;
import org.linphone.core.TransportType;
import org.linphone.core.tools.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

public class LinphoneService extends Service {

    private static LinphoneService sInstance;

    public static final int NOTIF_ID = 1;
    public static final String NOTIF_CHANNEL_ID = "DNETCALL_Id";

    private Handler mHandler;
    private Timer mTimer;

    private Core mCore;
    private CoreListenerStub mCoreListener;

    private static String username;
    private static String password;

    static private LinphoneServiceListener listener;
    private static boolean anonimous;

    public static boolean isReady() {
        return sInstance != null;
    }

    public static LinphoneService getInstance() {
        return sInstance;
    }

    public static Core getCore() {
        return sInstance.mCore;
    }

    static private String host = "185.176.42.76";
    public Call call;

    static public int registerStatus = 0;
    static public int callStatus = 0;

    @Nullable
    @Override

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // The first call to liblinphone SDK MUST BE to a Factory method
        // So let's enable the library debug logs & log collection
        String basePath = getFilesDir().getAbsolutePath();
        Factory.instance().setLogCollectionPath(basePath);
        Factory.instance().enableLogCollection(LogCollectionState.Enabled);
        Factory.instance().setDebugMode(true, "CALL");


        mHandler = new Handler();
        // This will be our main Core listener, it will change activities depending on events
        mCoreListener = new CoreListenerStub() {
            
            @Override
            public void onCallStateChanged(Core core, Call currentCall, Call.State state, String message) {
                // Toast.makeText(LinphoneService.this, message, Toast.LENGTH_SHORT).show();
                switch (state) {
                    case OutgoingInit:
                    case OutgoingProgress:
                    case OutgoingRinging:
                        call = currentCall;
                        if (callStatus == 0) {
                            callStatus = 1;
                            navigateCallActivity();
                            listener.onCallConneting();
                        }
                        break;
                    case Connected:
                        call = currentCall;
                        callStatus = 2;
                        listener.onCallConneted();
                        break;
                    case IncomingReceived:
                        call = currentCall;
                        navigateIncomingCallActivity();
                        listener.onCallReceiving();

                        break;
                    case End:
                    case Error:
                        call = null;
                        callStatus = 0;
                        if (anonimous) {
                            username = null;
                        }
                        listener.onCallEnd();
                    case Released:
                        listener.onCallRealesed();
                    default:
                        break;
                }

            }

            @Override
            public void onCallCreated(Core lc, Call call) {
                Log.e("CAll", "Created");
            }

            @Override
            public void onRegistrationStateChanged(Core core, ProxyConfig cfg, RegistrationState state, String message) {
                switch (state) {
                    case Ok:
                        registerStatus = 2;
                        listener.onRegisterOK();
                        break;
                    case Progress:
                        registerStatus = 1;
                        listener.onRegisterProgress();
                        break;
                    case Cleared:
                    case None:
                    case Failed:
                        registerStatus = 0;
                        listener.onRegisterFailed();
                        break;
                }
            }

        };

        try {
            // Let's copy some RAW resources to the device
            // The default config file must only be installed once (the first time)
            InputStream linphonerc_default = getAssets().open("linphonerc_default");
            InputStream linphonerc_factory = getAssets().open("linphonerc_factory");
            copyIfNotExist(linphonerc_default, basePath + "/.linphonerc");
            // The factory config is used to override any other setting, let's copy it each time
            copyFromPackage(linphonerc_factory, "linphonerc");
        } catch (IOException ioe) {
            Log.e(ioe);
        }

        // Create the Core and add our listener
        mCore = Factory.instance()
                .createCore(basePath + "/.linphonerc", basePath + "/linphonerc", this);
        mCore.addListener(mCoreListener);
        // Core is ready to be configured
        configureCore();


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // If our Service is already running, no need to continue
        if (sInstance != null) {
            return START_NOT_STICKY;
        }

        // Our Service has been started, we can keep our reference on it
        // From now one the Launcher will be able to call onServiceReady()
        sInstance = this;

        // Core must be started after being created and configured
        mCore.start();
        // We also MUST call the iterate() method of the Core on a regular basis
        TimerTask lTask =
                new TimerTask() {
                    @Override
                    public void run() {
                        mHandler.post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mCore != null) {
                                            mCore.iterate();
                                        }
                                    }
                                });
                    }
                };
        mTimer = new Timer("Linphone scheduler");
        mTimer.schedule(lTask, 0, 20);

        listener.onCreated();
        this.initializeNotification();
        return START_NOT_STICKY ;
    }

    @Override
    public void onDestroy() {
        mCore.removeListener(mCoreListener);
        mTimer.cancel();
        mCore.stop();
        // A stopped Core can be started again
        // To ensure resources are freed, we must ensure it will be garbage collected
        mCore = null;
        // Don't forget to free the singleton as well
        sInstance = null;

        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // For this sample we will kill the Service at the same time we kill the app
        stopSelf();

        super.onTaskRemoved(rootIntent);
    }

    private void configureCore() {
        // We will create a directory for user signed certificates if needed
        String basePath = getFilesDir().getAbsolutePath();
        String userCerts = basePath + "/user-certs";
        File f = new File(userCerts);
        if (!f.exists()) {
            if (!f.mkdir()) {
                Log.e(userCerts + " can't be created.");
            }
        }
        mCore.setUserCertificatesPath(userCerts);
    }

    private void copyIfNotExist(InputStream c, String target) throws IOException {
        File lFileToCopy = new File(target);

        if (!lFileToCopy.exists()) {
            copyFromPackage(c, lFileToCopy.getName());
        }
    }

    private void copyFromPackage(InputStream c, String target) throws IOException {

        FileOutputStream lOutputStream = openFileOutput(target, 0);
        InputStream lInputStream = c;
        int readByte;
        byte[] buff = new byte[8048];
        while ((readByte = lInputStream.read(buff)) != -1) {
            lOutputStream.write(buff, 0, readByte);
        }
        lOutputStream.flush();
        lOutputStream.close();
        lInputStream.close();
    }

    static public void addListener(LinphoneServiceListener l) {
        listener = l;
    }

    public Call CallTo(String username) {


        Address address;
        Call call = null;
        try {
            address = getCore().interpretUrl(username + "@" + host);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        address.setDisplayName(username);
        CallParams params = getCore().createCallParams(null);

        params.enableVideo(false);

        try {
            call = getCore().inviteAddressWithParams(address, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return call;
    }

    public void navigateCallActivity() {
        Intent intent = new Intent(LinphoneService.this, CallingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void navigateIncomingCallActivity() {
        Intent intent = new Intent(LinphoneService.this, IncomingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    static public Call getCall() {
        return getInstance() != null ? getInstance().call : null;
    }

    private static void setProxyConfig(String username, String password) {
        LinphoneService.getCore().clearProxyConfig();
        AccountCreator mAccountCreator = LinphoneService.getCore().createAccountCreator(null);
        mAccountCreator.setUsername(username);
        mAccountCreator.setDomain(host);
        mAccountCreator.setPassword(password);
        mAccountCreator.setTransport(TransportType.Udp);


        // This will automatically create the proxy config and auth info and add them to the Core
        ProxyConfig cfg = mAccountCreator.createProxyConfig();
        // Make sure the newly created one is the default
        LinphoneService.getCore().setDefaultProxyConfig(cfg);
    }

    static public void setCredentials(String u, String p) {
        LinphoneService lp = getInstance();

        username = u;
        password = p;
        anonimous = false;
        listener.onCredentialsSaved(username, password);
        if (lp != null)
            setProxyConfig(username, password);



    }

    static public void setCredentialsAnonimous(User u) {
        LinphoneService lp = getInstance();
        if (lp == null) {
            return;
        }
        username = u.username;
        password = u.password;
        anonimous = true;
        setProxyConfig(username, password);


    }

    static public void register() {
        LinphoneService lp = getInstance();
        if (lp == null || username == null || lp.username.length() > 0) {
            return;
        }

        setProxyConfig(lp.username, lp.password);
    }


    static public void unregister() {
        LinphoneService.getCore().clearProxyConfig();
    }

    static public Boolean hasUserData() {
        LinphoneService lp = getInstance();
        if (lp == null || username == null || username.length() == 0 || lp.anonimous) {
            return false;
        }
        return true;
    }

    public static void deleteUserData() {
        if (hasUserData()) {
            LinphoneService lp = getInstance();
            unregister();
            lp.username = null;
            lp.password = null;
            listener.onUserDataDeleted();
        }
    }

    public void initializeNotification() {

        if (!hasUserData()){
            return;
        }
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(
                this,
                NOTIF_CHANNEL_ID
        )
                .setContentTitle("Servicio de Yo Te Escucho")
                .setContentText("Para apagar, en el boton de la App")
                .setSmallIcon(R.drawable.app_icon)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1,notification);

    }
}

