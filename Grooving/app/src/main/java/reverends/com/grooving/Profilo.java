package reverends.com.grooving;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static reverends.com.grooving.MainActivity.player;

public class Profilo extends Fragment {
    View v;
    String url="http://52.15.100.48:8080/logout";
    String get_my_grooves="http://52.15.100.48:8080/my_grooves";
    String stream="http://52.15.100.48:8080/requeststream";
    ArrayList<Groove> grooves = new ArrayList<Groove>();
    String email;
    String autore_groove;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
    {
        v= inflater.inflate(R.layout.activity_profilo,null);
        TextView textView=(TextView)v.findViewById(R.id.logout_text);

        //Recupero l'email dell'utente corrente
        SharedPreferences utente_corrente=getContext().getSharedPreferences("utente_corrente", MODE_PRIVATE);
        email=utente_corrente.getString("email",null);

        //Quando l'utente clicca su disconnetti torno alla pagina di login
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnectuser();
                }
        });

        //Recupero i dati restituiti da Volley e costruisco la Listview con i miei groove
        GetMyGrooves(new VolleyCallback() {
            @Override
            public void onVolleyCompleted(ArrayList<Groove> arrayList) {
                grooves=arrayList;
                ListView listView = (ListView) v.findViewById(R.id.imieigroove_list);
                TextView nickname=(TextView)v.findViewById(R.id.nickname_text);
                nickname.setText(autore_groove);
                GrooveAdapter custom_adapter =new GrooveAdapter();
                listView.setAdapter(custom_adapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String autore=grooves.get(position).Get_autore();
                        String nomegroove=grooves.get(position).Get_nome();
                        Get_Response_volley(new OnVolleyAudioResponse() {
                            @Override
                            public void OnVolleyAudioResponse(String risposta) {
                                try {
                                    Log.d("RISPOSTA", risposta);
                                    CreateTempFile(risposta);
                                } catch (IOException e) {
                                    Log.d("ERRORE", e.toString());
                                }
                            }
                        },autore, nomegroove);
                    }
                });
            }
        });
        return v;
    }

    //Utilizzo questa funzione per disconnettere l'utente dal server
    public void Disconnectuser(){
        StringRequest request = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {

                        //Se la risposta ha il tag errore non eseguo il logout
                        try{
                            JSONObject object=new JSONObject(response);
                            if(object.has("error"))
                            {
                                Toast.makeText(getContext(),response,Toast.LENGTH_SHORT).show();

                            }

                            //Se la risposta ha il tag status significa che è OK e disconnetto l'untente
                            else if(object.has("status"))
                            {
                                Intent intent=new Intent();
                                intent.setClass(getActivity(), Authentication.class);
                                getActivity().startActivity(intent);
                                getActivity().finish();
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
                        Log.e("Volley", "Error");
                    }
                }
        ){
            @Override
            protected Map<String, String> getParams(){
                Map<String, String> params = new HashMap<String, String>();
                params.put("email", email);
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(request);
    }

    //Utilizzo un' interfaccia VolleyCallback per restituire l'arraylist di oggetti groove dell'utente corrente
    public void GetMyGrooves(final VolleyCallback callback){

        StringRequest stringRequest = new StringRequest(Request.Method.POST, get_my_grooves, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    ArrayList<Groove> array=new ArrayList<Groove>();
                    JSONObject jsonObject = new JSONObject(response);
                    for (int i = 0; i < jsonObject.getJSONArray("results").length(); i++) {
                        String nome_groove = jsonObject.getJSONArray("results").getJSONObject(i).getString("Nome_groove");
                        autore_groove = jsonObject.getJSONArray("results").getJSONObject(i).getString("Autore");
                        String categoria_groove = jsonObject.getJSONArray("results").getJSONObject(i).getString("Categoria");
                        String upload_groove = jsonObject.getJSONArray("results").getJSONObject(i).getString("UploadDate");
                        String strumenti_groove = jsonObject.getJSONArray("results").getJSONObject(i).getString("Strumenti");

                        array.add(i, new Groove(nome_groove, autore_groove, categoria_groove, strumenti_groove, upload_groove));
                        callback.onVolleyCompleted(array);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }


        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("VOLLEYERROR", error.toString());
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(stringRequest);
    }

    //Questo adapter servirà a visualizzare le informazioni richieste
    public class GrooveAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return grooves.size();
        }

        @Override
        public Object getItem(int position) {return null;}

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView=getLayoutInflater().inflate(R.layout.groove, null);
            TextView autore_groove=(TextView)convertView.findViewById(R.id.autore_groove_text);
            TextView nome_groove=(TextView)convertView.findViewById(R.id.nome_groove_text);
            TextView genere_groove=(TextView)convertView.findViewById(R.id.genere_groove_text);
            TextView upload_groove=(TextView)convertView.findViewById(R.id.uploaddate_groove_text);
            TextView strumenti_groove =(TextView)convertView.findViewById(R.id.strumenti_groove_text);
            autore_groove.setText(grooves.get(position).Get_autore());
            nome_groove.setText(grooves.get(position).Get_nome());
            genere_groove.setText(grooves.get(position).Get_genere());
            upload_groove.setText(grooves.get(position).Get_data_di_upload());
            strumenti_groove.setText(grooves.get(position).Get_strumenti());
            return convertView;
        }
    }

    //Utilizzo questa funzione per recuperare l'audio codificato in Base64 dal server
    public void Get_Response_volley(final OnVolleyAudioResponse onVolleyAudioResponse, final String grooveauthor, final String groovename) {
        //Avvio la richiesta al server
        StringRequest request = new StringRequest(Request.Method.POST, stream,
                new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        String risposta = "";

                        //Se la risposta ha il tag error non accedo e mostro l'errore
                        try {
                            JSONObject object = new JSONObject(response);
                            if (object.has("$binary")) {
                                risposta = object.getString("$binary");
                            }

                            //Se la risposta ha il tag status significa che è OK ed accedo
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        onVolleyAudioResponse.OnVolleyAudioResponse(risposta);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    // Handles errors that occur due to Volley
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Volley", error.toString());
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("author_groove", grooveauthor);
                params.put("groove_name", groovename);
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());
        requestQueue.add(request);
    }


    //Decodifico la stringa e creo il file temporaneo per riprodurlo

    public void CreateTempFile(String s) throws IOException {
        byte[] bytes= Base64.decode(s,0);

        try {

            File file= new File(getActivity().getCacheDir()+File.separator+"stream.wav");
            if (file.exists()){
                file.delete();
                player.stop();
                player.reset();
                Log.d("CANCEllATO","HO CANCELLATO UN FILE");
            }
            file=new File(getActivity().getCacheDir()+File.separator+"stream.wav");
            FileOutputStream fileOuputStream = new FileOutputStream(file,true);
            fileOuputStream.write(bytes);
            fileOuputStream.flush();
            fileOuputStream.close();
            Log.d("HO CREATO UN FILE", file.toString());
            Log.d("PROVO LA RIPRODUZIONE AUDIO", "PLAY");
            player.setDataSource(file.getPath());
            player.prepare();
            player.start();
            player.setLooping(true);


        }catch (IOException e) {
            e.printStackTrace();
        }

    }
}
