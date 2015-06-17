package com.example.marcin.ZosiaRemoteControl;

/**
 * Created by Aleksandra on 2015-06-04.
 */
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
import android.util.Log;

import java.util.*;
public class Decoder {
    int index;
    List<Byte> dataBuffer;
    List<Byte> buffer;
    List<Message> available;
    List<Boolean> matching;
    Message currentMsg=null;
    Decoder(){
        dataBuffer=new LinkedList<>();
        buffer=new LinkedList<>();
        available=new ArrayList<>();
        matching=new ArrayList<>();
    }
    boolean register(String msgName){
        Log.i("Register", "Poczatek!");
        try{
            available.add((Message) Class.forName(msgName).newInstance());
        }catch(Exception e){
            Log.i("Error","Nie ma takiej klasy!");
            available.clear();
            //available.add(new MainActivity.DistanceMsg());
            matching.add(new Boolean(true));
            return false;
        }
        matching.add(new Boolean(true));
        return true;
    }

    boolean register(Message msgType){
        boolean res=true;
        res&=available.add(msgType);
        res&=matching.add(new Boolean(true));
        return res;
    }

    void receive(byte[] data){
        if(data.length>0){
            buffer.addAll(array2AList(data));
        }
        Boolean nothingComparesToYou=false;
        if(currentMsg==null){
            for(int i=index+1; i<buffer.size(); i++){
                index++;
                if(buffer.get(index)=='E'){
                    Log.i("Decoder", "znalazlem E");
                }
                for(int j=0; j<available.size(); j++){
                    //Log.i("matching"+Integer.toString(buffer.size())," "+Integer.toString(index));
                    Log.i("Buffer",new String(List2array(buffer), 0, buffer.size()));
                    if(matching.get(j)==false) {
                        continue;
                    }
                    if(index < available.get(j).start.size() && buffer.get(index)==available.get(j).start.get(index)) {
                        if (index == available.get(j).start.size()) {
                            currentMsg = available.get(j);
                            setMatching();
                            buffer.clear();
                            index = 0;
                        }
                    }else{
                        matching.set(j,false);
                        nothingComparesToYou=true;
                    }
                }
                if(nothingComparesToYou){
                    Log.i("NothingComparesToYou?","Checking");
                    nothingComparesToYou=false;
                    for(Boolean b : matching){
                        nothingComparesToYou|=b;
                    }
                    if(!nothingComparesToYou){
                        for(int k=0; k<=index; k++){
                            buffer.remove(k);
                            index=0;
                            receive(new byte[]{});
                        }
                    }
                }
            }
        }else{
            if(currentMsg.len>=0){
                if(buffer.size()>=currentMsg.len){
                    System.arraycopy(buffer, 0, dataBuffer, 0, currentMsg.len);
                    for(int k=0; k<=currentMsg.len; k++){
                        buffer.remove(k);
                        index=0;
                    }
                    currentMsg.process(cloneDataBuffer());
                    dataBuffer.clear();
                    receive(new byte[]{});
                }
            }else{
                for(int k=index+1; k<buffer.size()-currentMsg.stop.size()+1; k++){
                    boolean stop=true;
                    for(int p=0; p<currentMsg.stop.size(); p++){
                        stop&=buffer.get(k+p)==currentMsg.stop.get(p);
                    }
                    if(stop){
                        if(k!=0){
                            for(int p=0; p<k; p++){
                                dataBuffer.add(buffer.remove(p));
                            }
                        }
                        for(int p=0; p<currentMsg.stop.size(); p++){
                            buffer.remove(p);
                        }
                        currentMsg.process(cloneDataBuffer());
                        dataBuffer.clear();
                        index=0;
                        setMatching();
                        receive(new byte[]{});
                        return;
                    }
                    for(int p=0; p<buffer.size(); p++){
                        dataBuffer.add(buffer.remove(p));
                    }
                }
            }
        }
    }

    public static abstract class Message{
        ArrayList<Byte> start;
        ArrayList<Byte> stop;
        public int len;
        public abstract void process(List<Byte> data);
        Message(){
            start=new ArrayList<>();
            stop=new ArrayList<>();
        }
    }

    void setMatching(){
        for(boolean b : matching){
            b=true;
        }
    }

    ArrayList<Byte> cloneDataBuffer(){
        ArrayList<Byte> tmp = new ArrayList<Byte>();
        for(Byte b : dataBuffer){
            tmp.add(b);
        }
        return tmp;
    }

    static ArrayList<Byte> array2AList(byte[] array){
        ArrayList<Byte> tmp = new ArrayList<Byte>();
        for(byte b : array){
            tmp.add(new Byte(b));
        }
        return tmp;
    }

    static byte[] List2array(List<Byte> array){
        byte[] tmp=new byte[array.size()];
        int i=0;
        for(Byte b : array){
            tmp[i]=b;
            i++;
        }
        return tmp;
    }

    public void clearAll(){
        buffer.clear();
        dataBuffer.clear();
        setMatching();
        index=0;
        Log.i("Buffer",new String(List2array(buffer), 0, buffer.size()));
    }
}
