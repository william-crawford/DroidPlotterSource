package com.rjl.droidplotter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.*;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainPlot extends AppCompatActivity
        implements Frag_BlueToothSetting.BlueInterface, Frag_Plotter.PlotInterface {

    private PlotFragAdapter fragAdapter;

    static Handler btHandler;
    private static final int RECIEVE_MESSAGE = 1;
    private static final UUID my_Bt_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static BluetoothAdapter blue_Adapter;              // Bluetooth adapter for the device
    private ArrayList<String> pair_Device_List = new ArrayList<>();   // array adapter for paired devices
    private BroadcastReceiver blue_State;               // broadcast receiver for status of the bluetooth in the device
    private BluetoothSocket blueSocket;                 // Socket for connecting bluetooth
    private BlueToothThread bt_connect_thread;               //  thread class (see BlueThread.java)

    private StringBuilder sb = new StringBuilder();
    private String logTag = "RJLbt";                    // log name for debugging

    private boolean checkBtSupport;
    private double prev;
    private boolean checkConnected = false;
    private boolean logEnabled = false;
    private double plotCount = 1;
    private double plotlen = 121;
    private double plotRes = 1;

    @SuppressLint("HandlerLeak")
    private void InitPlot() {
        blue_Adapter = BluetoothAdapter.getDefaultAdapter();
        fragAdapter = new PlotFragAdapter(getSupportFragmentManager());
        fragAdapter.AddFragments(new Frag_BlueToothSetting());
        fragAdapter.AddFragments(new Frag_Plotter());
        ViewPager viewPage = (ViewPager) findViewById(R.id.page_container);
        viewPage.setAdapter(fragAdapter);

        blue_State = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int btCurrentState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                //  int prevBtState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);

                //check state of the bluetooth and display a message of the current state of the bluetooth
                switch (btCurrentState) {
                    case (BluetoothAdapter.STATE_TURNING_ON): {
                        // Toast.makeText(getBaseContext(), "Bluetooth is turning ON", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case (BluetoothAdapter.STATE_ON): {
                        Toast.makeText(getBaseContext(), "Bluetooth is ON", Toast.LENGTH_SHORT).show();
                        AddToListView();
                        // bluetooth_ON = true;
                        break;
                    }
                    case (BluetoothAdapter.STATE_TURNING_OFF): {
                        // Toast.makeText(getBaseContext(), "Bluetooth is turning OFF", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case (BluetoothAdapter.STATE_OFF): {
                        Toast.makeText(getBaseContext(), "Bluetooth is OFF", Toast.LENGTH_SHORT).show();
                        //  bluetooth_ON = false;
                        break;
                    }
                    case (BluetoothAdapter.STATE_CONNECTED): {
                        Toast.makeText(getBaseContext(), "Bluetooth is Connected ", Toast.LENGTH_SHORT).show();
                        checkConnected = true;
                        break;
                    }
                    case (BluetoothAdapter.STATE_DISCONNECTED): {
                        Toast.makeText(getBaseContext(), "Bluetooth Disconnected ", Toast.LENGTH_SHORT).show();
                        checkConnected = false;
                        break;
                    }

                }
            }
        };

        btHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {

                    case RECIEVE_MESSAGE:                                             // if receive massage
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);           // create string from bytes array
                        sb.append(strIncom);                                          // append string
                        int endOfLineIndex = sb.indexOf("\r\n");                      // determine the end-of-line
                        if (endOfLineIndex > 0) {                                     // if end-of-line,
                            String sbprint = sb.substring(0, endOfLineIndex);         // extract string
                            double dbl;
                            String[] strSplit;
                            if (sbprint.contains("-l")) {                   // change this delimiter depending on your application
                                strSplit = sbprint.split("-l");            // change this delimiter depending on your application
                                try {
                                    dbl = Double.parseDouble(strSplit[1]);
                                    prev = scaler(dbl, 0, 3.3, -3.3, 3.3);  // change this value depending on your application
                                    if (logEnabled) {
                                        if (plotCount <= plotlen) {
                                            setPlot(plotCount, prev);
                                            plotCount = plotCount + plotRes;
                                        } else {
                                            clearGraph();
                                            plotCount = 0;
                                        }
                                    }
                                } catch (NumberFormatException nfe) {
                                    prev = 0;
                                }
                            }
                            sb.delete(0, sb.length()); // clear
                        }
                        break;
                }
            }
        };
    }

    // scale raw input from arduino to whatever value you want, this was taken directly from
    // map function in arduino and implemented to this android application as method
    private double scaler(double x, double in_min, double in_max, double out_min, double out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }


    // pass the data that is used to plot the graph
    private void setPlot(double timeX, double dataY) {
        Fragment frgs = fragAdapter.getItem(1);
        if (frgs instanceof Frag_Plotter) {
            ((Frag_Plotter) frgs).plotValue(timeX, dataY);
        }
    }

    // reset graph call by stream data to remove the previous plotted data
    private void clearGraph() {
        Fragment frgs = fragAdapter.getItem(1);
        if (frgs instanceof Frag_Plotter) {
            ((Frag_Plotter) frgs).ResetGraph();
        }
    }

    // check if this device supports bluetooth connectivity
    private boolean bt_support_check() {
        if (blue_Adapter == null) {
            Toast.makeText(getBaseContext(), "This device does not support Bluetooth", Toast.LENGTH_LONG).show();
            return false;
        } else {
            if (!blue_Adapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth is OFF", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getBaseContext(), "Bluetooth is ON", Toast.LENGTH_LONG).show();
            }
            return true;
        }
    }

    // get the list of paired devices from bluetooth adapter
    private ArrayList<String> get_bt_devices() {
        Set<BluetoothDevice> setBtDevice = blue_Adapter.getBondedDevices();
        if (setBtDevice.size() > 0) {
            for (BluetoothDevice btxDevice : setBtDevice) {
                pair_Device_List.add(btxDevice.getName() + "\n" + btxDevice.getAddress());
            }
        }
        return pair_Device_List;
    }

    // pass the list of paired device to the fragment
    private void AddToListView() {
        try {
            Fragment frg = fragAdapter.getItem(0);
            if (frg instanceof Frag_BlueToothSetting)
                ((Frag_BlueToothSetting) frg).SetDeviceList(get_bt_devices());
            else
                Toast.makeText(getBaseContext(), "Fragment Error", Toast.LENGTH_LONG).show();
        } catch (NullPointerException Npoint) {
            Toast.makeText(getBaseContext(), "Cannot get paired device list", Toast.LENGTH_LONG).show();
        }
    }

    /*======================================================================//
    ||           C R E A T E    B L U E T O O T H    S O C K E T            ||
    ========================================================================*/
    private BluetoothSocket create_bt_socket(BluetoothDevice btDevice) throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                final Method m = btDevice.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
                return (BluetoothSocket) m.invoke(btDevice, my_Bt_UUID);
            } catch (Exception e) {
                Log.e(logTag, "Could not create Insecure RFComm Connection", e);
            }
        }
        return btDevice.createRfcommSocketToServiceRecord(my_Bt_UUID);
    }

    /*===========================================================
   ||    B L U E T O O T H   C O N N E C T   M E T H O D        ||
   *============================================================*/
    private void connect_bt_device(String bt_mac_Addr) {

        //create a bluetooth device using the selected mac address;
        BluetoothDevice btDeviceConnect = blue_Adapter.getRemoteDevice(bt_mac_Addr);
        // try to connect device to socket if it is available
        try {
            blueSocket = create_bt_socket(btDeviceConnect);
        } catch (IOException ebt) {
            // if cannot connect to the said device display a message
            Toast.makeText(getBaseContext(), "Cannot connect to device", Toast.LENGTH_LONG).show();
            //Log.i(logTag, "Something Wrong here - connect remote");
        }

        // disable discovery, it is resource intensive task
        blue_Adapter.cancelDiscovery();

        //display in log cat when trying to establish a connection
        //Log.i(logTag, "Establishing Connection....");
        Toast.makeText(getBaseContext(), "Establishing Connection....", Toast.LENGTH_LONG).show();

        try {
            blueSocket.connect();
            // Log.i(logTag, "Connected...");
            Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG).show();
            checkConnected = true;
        } catch (IOException Esocket) {
            try {
                blueSocket.close();
            } catch (IOException e) {
                Log.i(logTag, "FATAL ERROR: unable to close socket during " +
                        "connection failure - connect_bt_device() method " + e.getMessage());
            }
        }
        // Initialize Bluetooth Thread
        bt_connect_thread = new BlueToothThread(blueSocket);
        bt_connect_thread.start();                             //start bluetooth Thread
    }

    /*===================================================================
   ||               I N T E R F A C E    M E T H O D                   ||
   ||          I M P L E M E N T A T I O N   S E C T I O N             ||
   ===================================================================*/

    @Override
    public void onSelectBlueToothDevice(String d) {
        final String devices = d;
        if (blue_Adapter.isEnabled() && !checkConnected) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Confirmation");
            builder.setMessage("Do you want to connect to this device");
            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    // get the device address
                    connect_bt_device(devices);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    bt_connect_thread.SendData("a");
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing
                    dialog.dismiss();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        } else if (blue_Adapter.isEnabled() && checkConnected) {
            Toast.makeText(getBaseContext(), "Already connected to a device", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getBaseContext(), "Turn on Bluetooth before connecting to this device", Toast.LENGTH_SHORT).show();
        }

    }


    // interface method for turning on bluetooth
    @Override
    public void SetBluetoothEnabled(boolean bl) {

        if (checkBtSupport) {
            if (bl && !blue_Adapter.isEnabled()) {
                // get_bt_devices();
                IntentFilter btStateChange = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                // string for enabling the bluetooth
                String eN_btIntent = BluetoothAdapter.ACTION_REQUEST_ENABLE;
                registerReceiver(blue_State, btStateChange);
                startActivityForResult(new Intent(eN_btIntent), 0);
            } else if (blue_Adapter.isEnabled()) {
                blue_Adapter.disable();
                checkConnected = false;
            }
        }
    }


    @Override
    public void onSetLoggingEnabled(boolean b) {
        if (b) {
            logEnabled = true;
            Toast.makeText(getBaseContext(), "Start Log", Toast.LENGTH_SHORT).show();
        } else {
            logEnabled = false;
            Toast.makeText(getBaseContext(), "End Log", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (blue_Adapter.isEnabled()) {
            blue_Adapter.disable();
        }
    }

    /*===========================================================
    ||                  O N   C R E A T E                       ||
     ===========================================================*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        InitPlot();
        checkBtSupport = bt_support_check();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (blue_Adapter.isEnabled()) {
            blue_Adapter.disable();
        }
        try {
            unregisterReceiver(blue_State);
        } catch (IllegalArgumentException illegal) {
            Toast.makeText(getBaseContext(), " Broadcast Receiver wasn't registered ", Toast.LENGTH_SHORT).show();
        }
    }

}
