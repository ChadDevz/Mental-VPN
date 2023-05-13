package com.chadx.v2ray.ph.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.chadx.v2ray.ph.R;

import java.io.InputStream;
import java.util.ArrayList;
import org.json.JSONObject;

public class CronoksAdapter extends ArrayAdapter<JSONObject> {

    private int spinner_id;

    public CronoksAdapter(Context context, int spinner_id, ArrayList<JSONObject> list) {
        super(context, R.layout.spinner_item, list);
        this.spinner_id = spinner_id;
    }

    @Override
    public JSONObject getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return view(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return view(position, convertView, parent);
    }

    private View view(int position, View convertView, ViewGroup parent) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.spinner_item, parent, false);
        TextView tv = v.findViewById(R.id.itemName);
        ImageView im = v.findViewById(R.id.itemImage);
        TextView configtype = v.findViewById(R.id.tvconfigtype);
        TextView infos = v.findViewById(R.id.tvInfo);
        try {
            tv.setText(getItem(position).getString("Name"));
            if (getItem(position).getString("ConfigType").contains("1")){
                configtype.setText("VMESS");
            }else if (getItem(position).getString("ConfigType").contains("2")){
                configtype.setText("VLESS");
            }else if (getItem(position).getString("ConfigType").contains("3")){
                configtype.setText("TROJAN");
            }else if (getItem(position).getString("ConfigType").contains("4")){
                configtype.setText("SOCKS");
            }else if (getItem(position).getString("ConfigType").contains("5")){
                configtype.setText("SHADOWSOCKS");
            }else {
                configtype.setText("CUSTOM");
            }
             infos.setText(getItem(position).getString("sInfo"));

            InputStream inputStream = getContext().getAssets().open("country/" + getItem(position).getString("Flag"));
            im.setImageDrawable(Drawable.createFromStream(inputStream, getItem(position).getString("Flag")));
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return v;
    }

}
