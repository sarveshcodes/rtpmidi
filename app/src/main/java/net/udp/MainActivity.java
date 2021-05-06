package net.udp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {
    NsdHelper mNsdHelper;
    ListView listView;
    public static final String TAG = "NsdChat";
    String s1[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);


    }

    @Override
    protected void onStart() {
        Log.d(TAG, "Starting.");
        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeNsd();
        mNsdHelper.registerService(5004, Build.MANUFACTURER+" "+Build.DEVICE);
        mNsdHelper.discoverServices();

        super.onStart();
    }

}