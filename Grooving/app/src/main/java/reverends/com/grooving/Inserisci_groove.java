package reverends.com.grooving;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Inserisci_groove extends AppCompatActivity {
    String nome_groove;
    String categoria_groove;
    String strumenti;
    String email;
    String url = "http://52.15.100.48:8080/upload";
    File audio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inserisci_groove);
        Button button = (Button) findViewById(R.id.submit_recorded_groove_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Recupero l'email inserita al login e i dati inseriti quando si compilano gli EdiText
                SharedPreferences utente_corrente = Inserisci_groove.this.getSharedPreferences("utente_corrente", MODE_PRIVATE);
                email = utente_corrente.getString("email", null);

                EditText editText = (EditText) findViewById(R.id.recorded_groove_name);
                EditText editText1 = (EditText) findViewById(R.id.recorded_groove_category);
                EditText editText2=(EditText)findViewById(R.id.recorded_groove_instruments);
                nome_groove = editText.getText().toString();
                categoria_groove = editText1.getText().toString();
                strumenti=editText2.getText().toString();

                StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        //Se la risposta ha il tag error non inserisco il groove e mostro l'errore
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.has("error")) {
                                response = jsonObject.getString("error");
                                Toast.makeText(Inserisci_groove.this, response, Toast.LENGTH_SHORT).show();
                            }

                            //Se la risposta ha il tag status inserisco il groove
                            else if (jsonObject.has("status")) {
                                response = jsonObject.getString("status");
                                Toast.makeText(Inserisci_groove.this, response, Toast.LENGTH_SHORT).show();
                                finish();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Volley", "Error");

                    }
                })

                {
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        params.put("email", email);
                        params.put("groove_name", nome_groove);
                        params.put("category", categoria_groove);
                        params.put("instrument",strumenti);
                        try {
                            params.put("groove", Audio_to_String(audio));//Invio il file trasformato in una stringa al server
                        } catch (IOException exception) {
                            Log.e("Eccezione", exception.toString());
                        }
                        return params;
                    }
                };
                stringRequest.setRetryPolicy(new DefaultRetryPolicy( 50000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                RequestQueue requestQueue = Volley.newRequestQueue(Inserisci_groove.this);
                requestQueue.add(stringRequest);
            }
        });
    }

        //Questa funzione prende in input un file esistente caricato sulla SDCard e lo trasforma
        //in un array di byte. Dopo si usa la funzione Base64 per convertire l'array di byte in una stringa
        private String Audio_to_String (File audio) throws IOException {
            String encodedaudio;
            Log.d("AUDIOTOSTRINGFUNC", "Sono entrato nella funzione");
            File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "Music");
            if (dir.exists()) {
                Log.d("CARTELLA MUSIC", "La cartella esiste");
                audio = new File(dir.getAbsoluteFile() + File.separator + nome_groove + ".wav");
                if (audio.exists()) {
                    Log.d("FILE", "Il file esiste");
                }
            }
            Log.d("ESEGUO AUDIOTOSTRING", "Provo la conversione");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            FileInputStream fileInputStream = new FileInputStream(audio);
            byte[] buff = new byte[1024];
            try {
                for (int read; (read = fileInputStream.read(buff)) != -1; ) {
                    byteArrayOutputStream.write(buff, 0, read);
                    Log.d("CONVERSIONE ", (read + " bytes,"));
                }
            } catch (IOException ex) {
                Log.d("CONVERSIONE NON RIUSCITA", "Error");
            }
            byte[] audioBytes = byteArrayOutputStream.toByteArray();
            encodedaudio = Base64.encodeToString(audioBytes,Base64.DEFAULT);//Prova con new String in utf8
            Log.d("AUDIO IN STRINGA", encodedaudio);
            return encodedaudio;
        }

    }
