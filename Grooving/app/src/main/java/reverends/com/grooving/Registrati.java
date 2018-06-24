package reverends.com.grooving;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class Registrati extends AppCompatActivity {
    String email;
    String password;
    String username;
    String url = " http://18.191.156.47:8080/register";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrati);
        Button register = findViewById(R.id.register_button);

        //Quando clicco il pulsante Registrati connettiti al server e invia i dati
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText1 = (EditText) findViewById(R.id.email_register);
                EditText editText2 = (EditText) findViewById(R.id.password_register);
                EditText editText3 = (EditText) findViewById(R.id.username_register);

                //Assegno le editText alle variabili email, password e username
                email = editText1.getText().toString();
                password = editText2.getText().toString();
                username = editText3.getText().toString();

                //Avvio la richiesta al server
                StringRequest request = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {

                            @Override
                            public void onResponse(String response) {

                                //Se la risposta ha il tag error non accedo e mostro l'errore
                                try {
                                    JSONObject object = new JSONObject(response);
                                    if (object.has("error")) {
                                        response = object.getString("error");
                                        TextView textView = findViewById(R.id.register_result);
                                        textView.setText(response);

                                    }

                                    //Se la risposta ha il tag status significa che è OK e passo alla schermata di Login
                                    else if (object.has("status")) {
                                        Intent intent = new Intent(Registrati.this, Authentication.class);
                                        startActivity(intent);
                                        finish();
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override

                            public void onErrorResponse(VolleyError error) {
                                Log.e("Volley", "Error");
                            }
                        }
                ) {
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("email", email);
                        params.put("password", password);
                        params.put("username", username);
                        return params;
                    }
                };
                RequestQueue requestQueue = Volley.newRequestQueue(Registrati.this);
                requestQueue.add(request);
            }
        });
    }
}
