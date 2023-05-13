package com.chadx.v2ray.ph;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.text.method.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class XCronoksReportCrash2 extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cronoks_crash_report);
        TextView textcrash = findViewById(R.id.CrashReportView);
        TextView brand = findViewById(R.id.BrandID);
        TextView model = findViewById(R.id.ModelID);
        TextView time = findViewById(R.id.TimeID);
        TextView androidverr = findViewById(R.id.AndroidID);
        Button Close = findViewById(R.id.CloseAppCrash);
        Intent crashrep = getIntent();
        String CrashTepresentative = crashrep.getExtras().getString("XReport");
        textcrash.setText(CrashTepresentative);
        textcrash.setMovementMethod(new ScrollingMovementMethod());

        String Manufacturer = Build.BRAND;
        brand.setText(Manufacturer);
        String Model = Build.MODEL;
        model.setText(Model);
        String newFormat = java.text.DateFormat.getDateTimeInstance().format(new Date());
        time.setText(newFormat);
        String androidver = "Android" + " " + Build.VERSION.RELEASE;
        androidverr.setText(androidver);

        Close.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Visit Facebook page or Facebook Group

                // naka ano to sa Fagmmmu group Hanapin mo ung Group naten sa Crazystem TV
                String facebookId = "fb://group/598322564864293";
                String urlPage = "https://www.facebook.com/CrazystemTvOfficial";

                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(facebookId )));
                }catch (Exception e){
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlPage )));
                }
                // TODO: Implement this method
            }


        });
    }

}

