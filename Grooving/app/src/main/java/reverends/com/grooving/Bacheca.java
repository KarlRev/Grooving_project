package reverends.com.grooving;


import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


import static reverends.com.grooving.MainActivity.player;


public class Bacheca extends Fragment {
    View v;
    ArrayList<Groove> grooves = new ArrayList<Groove>();
    String url = "http://18.191.156.47:8080/wall";
    String url_streaming = "http://18.191.156.47:8080/requeststream";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.activity_bacheca, null);
        Getresponse(new VolleyCallback() {
            @Override
            public void onVolleyCompleted(ArrayList<Groove> arrayList) {
                grooves=arrayList;
                ListView listView = (ListView) v.findViewById(R.id.lista_groove);
                CustomAdapter custom_adapter = new CustomAdapter();
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

    //Utilizzo un' interfaccia VolleyCallback per restituire l'arraylist di grooves creato
    public void Getresponse(final VolleyCallback callback){

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    ArrayList<Groove> array=new ArrayList<Groove>();
                    JSONObject jsonObject = new JSONObject(response);
                    for (int i = 0; i < jsonObject.getJSONArray("results").length(); i++) {
                        String nome_groove = jsonObject.getJSONArray("results").getJSONObject(i).getString("Nome_groove");
                        String autore_groove = jsonObject.getJSONArray("results").getJSONObject(i).getString("Autore");
                        String categoria_groove = jsonObject.getJSONArray("results").getJSONObject(i).getString("Categoria");
                        String upload_groove = jsonObject.getJSONArray("results").getJSONObject(i).getString("UploadDate");
                        String upload_strumenti = jsonObject.getJSONArray("results").getJSONObject(i).getString("Strumenti");

                        array.add(i, new Groove(nome_groove, autore_groove, categoria_groove, upload_strumenti, upload_groove));
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
        });
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(stringRequest);
    }

    //Questo adapter servirà a visualizzare le informazioni richieste
    public class CustomAdapter extends BaseAdapter {
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
            final TextView autore_groove=(TextView)convertView.findViewById(R.id.autore_groove_text);
            final TextView nome_groove=(TextView)convertView.findViewById(R.id.nome_groove_text);
            TextView genere_groove=(TextView)convertView.findViewById(R.id.genere_groove_text);
            TextView upload_groove=(TextView)convertView.findViewById(R.id.uploaddate_groove_text);
            TextView strumenti_groove=(TextView)convertView.findViewById(R.id.strumenti_groove_text);
            autore_groove.setText(grooves.get(position).Get_autore());
            nome_groove.setText(grooves.get(position).Get_nome());
            genere_groove.setText(grooves.get(position).Get_genere());
            upload_groove.setText(grooves.get(position).Get_data_di_upload());
            strumenti_groove.setText(grooves.get(position).Get_strumenti());
            return convertView;
        }
    }

    //Utilizzo questa funzione per ritornare la stringa codificata in Base64 dal server
    public void Get_Response_volley(final OnVolleyAudioResponse onVolleyAudioResponse, final String grooveauthor, final String groovename) {
        //Avvio la richiesta al server
        StringRequest request = new StringRequest(Request.Method.POST, url_streaming,
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

//Utilizzo questa funzione per trasformare la stringa ricevuta in input dal server in un array di byte,
// la decodifico tramite decode e creo un file nella memoria temporanea riproducibile tramite mediaplayer

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




