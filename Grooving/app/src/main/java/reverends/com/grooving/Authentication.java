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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Authentication extends AppCompatActivity {
    String email;
    String password;
    String url = "http://52.15.100.48:8080/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);
        Button login = findViewById(R.id.login_button);

        //Quando clicco il pulsante Login connettiti al server
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText1 = (EditText) findViewById(R.id.text_login);
                EditText editText2 = (EditText) findViewById(R.id.pass_login);

                //Assegno le editText alle variabili username e password
                email=editText1.getText().toString();
                password=editText2.getText().toString();

                //Avvio la richiesta al server
                StringRequest request = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {

                            @Override
                            public void onResponse(String response) {

                                //Se la risposta ha il tag error non accedo e mostro l'errore
                                try{
                                    JSONObject object=new JSONObject(response);
                                    if(object.has("error"))
                                    {
                                        response=object.getString("error");
                                        TextView textView=findViewById(R.id.login_result);
                                        textView.setText(response);

                                    }

                                    //Se la risposta ha il tag status significa che è OK ed accedo
                                    else if(object.has("status"))
                                    {

                                        SharedPreferences.Editor pulisciSP= getSharedPreferences("utente_corrente", MODE_PRIVATE).edit();
                                        pulisciSP.remove("email");
                                        pulisciSP.apply();
                                        SharedPreferences.Editor utente_corrente=getSharedPreferences("utente_corrente",MODE_PRIVATE).edit();
                                        utente_corrente.putString("email",email);
                                        utente_corrente.apply();
                                        Intent intent=new Intent(Authentication.this, MainActivity.class);
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
                            // Handles errors that occur due to Volley
                            public void onErrorResponse(VolleyError error) {
                                Log.d("Volley", error.toString());
                            }
                        }
                ){
                    @Override
                    protected Map<String, String> getParams(){
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("email", email);
                        params.put("password", password);
                        return params;
                    }
                };

                request.setRetryPolicy(new DefaultRetryPolicy( 50000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                RequestQueue requestQueue = Volley.newRequestQueue(Authentication.this);
                requestQueue.add(request);
            }
        });

        //Quando clicco sulla voce registrati apro l'Activity Registrati
        TextView notregistered =(TextView)findViewById(R.id.notregistered);
    notregistered.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent=new Intent(Authentication.this, Registrati.class);
            startActivity(intent);
            finish();
        }
    });
    }
}