package com.example.marcin.ZosiaRemoteControl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.view.View.OnTouchListener;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.io.InputStream;
import java.io.OutputStream;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Looper;
import android.util.Log;

public class MainActivity extends ActionBarActivity {

    final int MESSAGE_READ=1254;
    ImageButton buttonUp;
    Button buttonScan;
    ImageButton buttonLeft;
    ImageButton buttonRight;
    ImageButton buttonDown;
    Button buttonStop;
    SeekBar seekbar;
    static TextView konsola;
    BluetoothLeScanner skaner;
    BluetoothAdapter btAdapter;
    Decoder interpretator;
    //scanCB skanerCallbackObj;
    Set<BluetoothDevice> paired;
    UUID id;//= UUID.randomUUID();
    Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case MESSAGE_READ:
                {
                    byte[] readBuf = (byte[]) msg.obj;
                    byte[] tmp=new byte[msg.arg1];
                    System.arraycopy(readBuf, 0, tmp, 0, msg.arg1);
                    interpretator.receive(tmp.clone());

                    // construct a string from the valid bytes in the buffer
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    //Toast.makeText(getApplicationContext(),readMessage, Toast.LENGTH_SHORT).show();
                    //konsola.append(readMessage);
                    break;
                }
                default:
                {
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Toast.makeText(getApplicationContext(),readMessage, Toast.LENGTH_SHORT).show();
                }
            }
        }
    };


    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                ParcelUuid list[] = device.getUuids();
                id=list[0].getUuid();
                tmp = device.createRfcommSocketToServiceRecord(id);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            btAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
                Toast.makeText(getApplicationContext(),"Connected", Toast.LENGTH_SHORT).show();
            } catch (IOException connectException) {
                Toast.makeText(getApplicationContext(),"Not connected", Toast.LENGTH_SHORT).show();
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    //
                }
                return;
            }
            manageConnectedSocket(mmSocket);
        }
        public void cancel(){
            try{
                mmSocket.close();
            }catch (IOException e){}
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            String action= intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Toast.makeText(getApplicationContext(),device.getName(), Toast.LENGTH_SHORT).show();
                if(device.getName().contains("ZOSIA")){
                    //btAdapter.cancelDiscovery();
                    Toast.makeText(getApplicationContext(),device.getName(), Toast.LENGTH_SHORT).show();
                    ConnectThread connThr=new ConnectThread(device);
                    connThr.run();
                    manageConnectedSocket(connThr.mmSocket);
                }

            }


        }

    };

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(),"IOStreamError", Toast.LENGTH_SHORT).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

       /* public synchronized void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    byte[] tmp=new byte[1024];
                    int index=buff.zapis(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buff.odczyt(index))
                            .sendToTarget();

                    /*mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }*/

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
   /* class bufor{
        int iteracja;
        int rozmiar=1024;
        byte[][] dane=new byte[1024][rozmiar];
        bufor(){
            for(int i=0; i<rozmiar;i++){
                for(int j=0; j<1024; j++){
                    dane[j][i]='0';
                }
            }
        }
        int zapis(byte[] input){
            byte[] tmp=dane[iteracja];
            System.arraycopy(input,0,tmp,0,1024);
            if(iteracja==rozmiar-1){
                iteracja=0;
                return rozmiar-1;
            }
            return iteracja++;
        }
        byte[] odczyt(int index){
            return dane[index];
        }
    }*/
   // bufor buff;
    ConnectedThread cthr=null;
    void manageConnectedSocket(BluetoothSocket socket){
        cthr=new ConnectedThread(socket);
        cthr.start();
    }
/*
    private class scanCB implements LeScanCallback {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord){
            Toast.makeText(getApplicationContext(),"Coś znalazłem", Toast.LENGTH_SHORT).show();
        }
    }
*/
    public void klikButtonUp(View v){
        Log.i("Up","Clicked");
        Integer a;
        a = seekbar.getProgress();
        String b;
        b = a.toString();
        String tmpMessage=":forward_";
        tmpMessage += b + ";";
        if(cthr!=null){
            cthr.write(tmpMessage.getBytes());
        }else{
            Toast.makeText(getApplicationContext(),"First connect to ZOSIA!", Toast.LENGTH_SHORT).show();
        }
    }

    public void klikButtonDown(View v){
        Log.i("Down","Clicked");
        Integer a;
        a = seekbar.getProgress();
        String b;
        b = a.toString();
        String tmpMessage=":backwrd_";
        tmpMessage += b + ";";
        if(cthr!=null){
            cthr.write(tmpMessage.getBytes());
        }else{
            Toast.makeText(getApplicationContext(),"First connect to ZOSIA!", Toast.LENGTH_SHORT).show();
        }
    }

    public void klikButtonLeft(View v){
        Log.i("Left","Clicked");
        Integer a;
        a = seekbar.getProgress();
        String b;
        b = a.toString();
        String tmpMessage=":turn_lt_";
        tmpMessage += b + ";";
        if(cthr!=null){
            cthr.write(tmpMessage.getBytes());
        }else{
            Toast.makeText(getApplicationContext(),"First connect to ZOSIA!", Toast.LENGTH_SHORT).show();
        }

    }
    public void klikButtonRight(View v){
        Log.i("Right","Clicked");
        Integer a;
        a = seekbar.getProgress();
        String b;
        b = a.toString();
        String tmpMessage=":turn_rt_";
        tmpMessage += b + ";";
        if(cthr!=null){
            cthr.write(tmpMessage.getBytes());
        }else{
            Toast.makeText(getApplicationContext(),"First connect to ZOSIA!", Toast.LENGTH_SHORT).show();
        }
    }

    public void klikButtonStop(View v){
        Log.i("Stop","Clicked");
        String tmpMessage=":set_RPS_0;";
        //interpretator.clearAll();
        if(cthr!=null){
            cthr.write(tmpMessage.getBytes());
        }else{
            Toast.makeText(getApplicationContext(),"First connect to ZOSIA!", Toast.LENGTH_SHORT).show();
        }
    }

    public void klikButtonScan(View v){
        Toast.makeText(getApplicationContext(),"Scanning...", Toast.LENGTH_SHORT).show();
        //skaner.startScan(skanerCallbackObj);
        //btAdapter.startLeScan(skanerCallbackObj);
        /*paired=btAdapter.getBondedDevices();
        for(BluetoothDevice device : paired){
            Toast.makeText(getApplicationContext(),device.getName(), Toast.LENGTH_SHORT).show();
        }*/
        IntentFilter filter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        btAdapter.startDiscovery();
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonUp = (ImageButton) findViewById(R.id.imageButtonUp);
        buttonScan = (Button) findViewById(R.id.buttonScan);
        buttonStop = (Button) findViewById(R.id.buttonStop);
        buttonLeft = (ImageButton) findViewById(R.id.imageButtonLeft);
        buttonRight = (ImageButton) findViewById(R.id.imageButtonRight);
        buttonDown = (ImageButton) findViewById(R.id.imageButtonDown);
        seekbar = (SeekBar) findViewById(R.id.seekBar);
        //konsola = (TextView) findViewById(R.id.konsola);
       // buff=new bufor();
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btAdapter.enable();
       // String readMessage = new String(buff.odczyt(0),0,100);
        Log.i("OnCreate MainActivity", "Przed new Decoder");
        interpretator=new Decoder();
        //interpretator.register("MainActivity.DistanceMsg");
        DistanceMsg DistanceRec=new DistanceMsg();
        interpretator.register(DistanceRec);
        //skaner=btAdapter.getBluetoothLeScanner();
        //skanerCallbackObj=new scanCB();



        buttonUp.setOnTouchListener (new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    klikButtonUp(v);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    klikButtonStop(v);
                    return false;
                }
                return true;
            }
        });

        buttonLeft.setOnTouchListener (new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    klikButtonLeft(v);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    klikButtonStop(v);
                    return false;
                }
                return true;
            }
        });

        buttonRight.setOnTouchListener (new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    klikButtonRight(v);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    klikButtonStop(v);
                    return false;
                }
                return true;
            }
        });

        buttonDown.setOnTouchListener (new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    klikButtonDown(v);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    klikButtonStop(v);
                    return false;
                }
                return true;
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    public static class DistanceMsg extends Decoder.Message{
        DistanceMsg(){
            super.start=Decoder.array2AList("Distance is ".getBytes());
            super.stop=Decoder.array2AList("\r\n".getBytes());
            super.len=4;
        }
        public void process(List<Byte> data){
            byte[] tmp= new byte[data.size()];
            for(int i=0; i<data.size(); i++){
                tmp[i]=data.get(i);
            }
            konsola.append(new String(tmp));;
        }
    }


}
