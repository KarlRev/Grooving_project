package reverends.com.grooving;

import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class Registra extends Fragment {
    View v;
    int recording=0;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
    {

        v=inflater.inflate(R.layout.activity_registra,null);

        Button button=(Button)v.findViewById(R.id.record_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(recording==0) {

                    Toast.makeText(getContext(),"Sto registrando",Toast.LENGTH_SHORT).show();
                    recording=1;
                    }
                    else
                {
                    Toast.makeText(getContext(),"Registrazione stoppata",Toast.LENGTH_SHORT).show();
                    recording=0;
                    Intent intent= new Intent(getContext(), Inserisci_groove.class);
                    startActivity(intent);
                }
            }
        });
        return v;
    }
}