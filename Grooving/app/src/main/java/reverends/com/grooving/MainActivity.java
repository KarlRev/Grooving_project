package reverends.com.grooving;

import android.media.MediaPlayer;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener{
    public static MediaPlayer player=new MediaPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navigation = findViewById(R.id.bottom_bar);
        navigation.setOnNavigationItemSelectedListener(this);
        loadFragment(new Bacheca());
        Button button=(Button)findViewById(R.id.stopbutton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(player.isPlaying())
                {
                    player.pause();
                    player.reset();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        if(player.isPlaying())
        {
            player.pause();
            player.reset();
        }
        super.onStop();
    }

    private boolean loadFragment(Fragment fragment){
        if (fragment != null){
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;

        }
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        Fragment fragment=null;
        switch(item.getItemId()){
            case R.id.wall:
                fragment= new Bacheca();
                break;

            case R.id.record:
                fragment= new Registra();
                break;

            case R.id.notifications:
                fragment= new Notifiche();
                break;

            case R.id.profile:
                fragment= new Profilo();
                break;
        }
        return loadFragment(fragment);
    }
}
