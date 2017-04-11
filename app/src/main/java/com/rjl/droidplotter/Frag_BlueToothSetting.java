package com.rjl.droidplotter;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class Frag_BlueToothSetting extends Fragment {

    private TextView txtDeviceName, txtDeviceAddress;
    private ListView lvDeviceList;
    private BlueInterface BlueFace;


    interface BlueInterface {
        void onSelectBlueToothDevice(String d);
        void SetBluetoothEnabled(boolean bl);
    }
    private void InitBlueSettings(View v) {
        txtDeviceName = (TextView) v.findViewById(R.id.txtDevice);
        txtDeviceAddress = (TextView) v.findViewById(R.id.txtMacAddr);
        lvDeviceList = (ListView) v.findViewById(R.id.listPairedDevice);

        Switch swEnableBlue = (Switch) v.findViewById(R.id.SwEnableBt);
        swEnableBlue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                    BlueFace.SetBluetoothEnabled(true);
                else
                    BlueFace.SetBluetoothEnabled(false);
            }
        });
     lvDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
         @Override
         public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

             String btdevice = (String) adapterView.getItemAtPosition(i);

             String[] splitStr;
             if (btdevice.contains("\n")) {
                 splitStr = btdevice.split("\n");
                 txtDeviceName.setText(splitStr[0]);
                 txtDeviceAddress.setText(splitStr[1]);
                 String addr = splitStr[1];
                 BlueFace.onSelectBlueToothDevice(addr);
             }
         }
     });
    }

    void SetDeviceList(ArrayList<String> devices) {
        Set<String> hs = new HashSet<>();
        ArrayList<String> arrayListItem = new ArrayList<>();
        arrayListItem.addAll(devices);
        hs.addAll(arrayListItem);
        arrayListItem.clear();
        arrayListItem.addAll(hs);

        ArrayAdapter<String> itemListAdapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_list_item_1, arrayListItem){
            @Override
            public View getView(int position, View convertView, ViewGroup parent){
                // Get the Item from ListView
                View view = super.getView(position, convertView, parent);
                // Initialize a TextView for ListView each Item
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                // Set the text color of TextView (ListView Item)
                tv.setTextColor(Color.parseColor("#00ea03"));
                // Generate ListView Item using TextView
                return view;
            }
        };
        lvDeviceList.setAdapter(itemListAdapter);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            BlueFace = (BlueInterface) context;
        } catch (ClassCastException ex) {
            throw new ClassCastException(context.toString());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View BLueView = inflater.inflate(R.layout.blue_settings_layout, container, false);

        InitBlueSettings(BLueView);

        return BLueView;
    }
}
