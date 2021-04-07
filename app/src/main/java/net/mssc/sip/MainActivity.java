package net.mssc.sip;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import java.text.ParseException;

public class MainActivity extends AppCompatActivity {

    /**
     * SipManager se encarga de:
     * Iniciar sesiones SIP
     * Iniciar y recibir llamadas
     * Registrar y cancelar el registro con un proveedor SIP
     * Verificar la conectividad de la sesión
     **/
    private SipManager sipManager = null;

    /**
     * Un SipProfile define un perfil de SIP, que incluye una cuenta SIP y la información de dominio y servidor.
     **/
    private SipProfile sipProfile = null;
    private SipProfile.Builder builder = null;

    SipAudioCall call = null;
    public IncomingCallReceiver callReceiver;

    String username = "voldymolt";
    String domain = "sip.antisip.com";
    String password = "caballito";

//    String username1 = "grayskull";
//    String domain1 = "sip.antisip.com";
//    String password1 = "shadowprime";


    Button btnLlamar, btnResponder;
    boolean veri = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(SipManager.isVoipSupported(getApplicationContext())){
            Log.d("VOIP:", "Supported!");
        }
        else{
            Log.d("VOIP:", "Not Supported");
        }
        if(SipManager.isApiSupported(getApplicationContext())){
            Log.d("API:", "Supported!");
        }
        else{
            Log.d("API:","NotSupported!");
        }

        btnLlamar = findViewById(R.id.btnLlamar);
        btnLlamar.setOnClickListener(v->{
            try {
                SipAudioCall call = sipManager.makeAudioCall(sipProfile.getUriString(), "sip:grayskull@sip.antisip.com", listener, 30);
                Log.d("CALL", call.getState()+"");
            } catch (SipException e) {
                e.printStackTrace();
            }
        });

        btnResponder = findViewById(R.id.btnResponder);
        btnResponder.setOnClickListener(v->{
            if(veri){
                btnResponder.setText("Colgar");
                veri = false;
            }else {
                btnResponder.setText("Contestar");
                veri = true;
            }
            callReceiver.accept();
        });

        //INICIARLIZAR SIP MANAGER
        if (sipManager == null) {
            sipManager = SipManager.newInstance(this);
        }

        //CONSTRUIR EL PERFIL SIP
        try {
            builder = new SipProfile.Builder(username, domain);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        builder.setPassword(password);
        sipProfile = builder.build();

        /**Abre el perfil local para realizar o recibir llamadas SIP genéricas**/
        Intent intent = new Intent();
        intent.setAction("net.mssc.sip.INCOMING_CALL");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, Intent.FILL_IN_DATA);
        try {
            sipManager.open(sipProfile, pendingIntent, null);
        } catch (SipException e) {
            e.printStackTrace();
        }

        /** Realiza un seguimiento de si el SipProfile se registró correctamente con tu proveedor de servicios SIP **/
        try {
            sipManager.setRegistrationListener(sipProfile.getUriString(), new SipRegistrationListener() {
                @Override
                public void onRegistering(String s) {
                    Log.d("HERA", "Registering with SIP Server...");
                }

                @Override
                public void onRegistrationDone(String s, long l) {
                    Log.d("HERA", "Ready" + s);
                }

                @Override
                public void onRegistrationFailed(String s, int i, String s1) {
                    Log.d("HERA", "Registration failed.  Please check settings.");
                }
            });
        } catch (SipException e) {
            e.printStackTrace();
        }

        /**
         * Filtro que intercepta la transmisión y activa el receptor (IncomingCallReceiver)
         */
        IntentFilter filter = new IntentFilter();
        filter.addAction("net.mssc.sip.INCOMING_CALL");
        callReceiver = new IncomingCallReceiver();
        this.registerReceiver(callReceiver, filter);

    }

    /** Para realizar una llamada de audio, debes configurar un SipAudioCall.Listener.
     * Gran parte de la interacción del cliente con la pila SIP ocurre a través de los objetos de escucha.**/
    SipAudioCall.Listener listener = new SipAudioCall.Listener() {

        @Override
        public void onCallEstablished(SipAudioCall call) {
            Log.d("CALL", "Call started!");
            call.setSpeakerMode(true);
            call.startAudio();
        }

        @Override

        public void onCallEnded(SipAudioCall call) {
            Log.d("CALL", "END");
            //closeLocalProfile();

        }
    };


    /**CERRAR PERFIL PARA LIBERAR MEMORIA**/
    public void closeLocalProfile() {
        if (sipManager == null) {
            return;
        }
        try {
            if (sipProfile != null) {
                sipManager.close(sipProfile.getUriString());
            }
        } catch (Exception ee) {
            Log.d("HERA", "Failed to close local profile.");
        }
    }

    /**
     * Para recibir una llamada se tiene que configurar un BroadcastReceiver
     */
    public static class IncomingCallReceiver extends BroadcastReceiver {
        SipAudioCall incomingCall = null;
        Vibrator v = null;
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("net.mssc.sip.INCOMING_CALL")){
                Toast.makeText(context, "Llamada entrante ", Toast.LENGTH_LONG).show();
                // Get instance of Vibrator from current Context
                v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
//                 Vibrate for 400 milliseconds
                v.vibrate(3000);

            }
            try {
                SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                    @Override
                    public void onRinging(SipAudioCall call, SipProfile caller) {
                        try {
                            call.answerCall(30);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                MainActivity wtActivity = (MainActivity) context;
                incomingCall = wtActivity.sipManager.takeAudioCall(intent, listener);
                wtActivity.call = incomingCall;
                if(incomingCall.isMuted()) {
                    incomingCall.toggleMute();
                }
//                wtActivity.updateStatus(incomingCall);
            } catch (Exception e) {
                if (incomingCall != null) {
                    incomingCall.close();
                }
            }
        }
        boolean ver = true;
        public void accept()
        {
            Log.d("Receiver","Answer");
            v.cancel();
            try {
                if(ver){
                    incomingCall.answerCall(30);
                    incomingCall.startAudio();
                    incomingCall.setSpeakerMode(true);
                    ver = false;
                }else{
                    incomingCall.close();
                    ver = true;
                }
            } catch (SipException e) {
                Log.d("Call Exception",e.toString());
                e.printStackTrace();
            }
            catch (Exception e)
            {
                Log.d("Exception",e.toString());
            }
        }
    }

}