package net.mssc.sip;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.Bundle;
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

    String username = "voldymolt";
    String domain = "sip.antisip.com";
    String password = "caballito";

    Button btnLlamar;

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
                SipAudioCall call = sipManager.makeAudioCall(sipProfile.getUriString(), "sip:caballito@sip.antisip.com", listener, 30);
                Log.d("CALL", call.getState()+"");
            } catch (SipException e) {
                e.printStackTrace();
            }
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
        intent.setAction("android.SipDemo.INCOMING_CALL");
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

}