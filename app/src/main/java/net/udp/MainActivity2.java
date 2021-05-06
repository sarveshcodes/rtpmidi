package net.udp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Random;


public class MainActivity2 extends AppCompatActivity {
    TextView textView;
    EditText editText;
    String ip ;

    byte [] data;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Intent intent = getIntent();
        ip = new String(intent.getStringExtra("IP"));


        Button button=findViewById(R.id.button);
        editText=findViewById(R.id.editText);
        textView=findViewById(R.id.textView);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textView.append("Initiator: "+ editText.getText() + "\n");
                new InitiateMIDISession(ip).start();
                //sendPacket(editText.getText().toString());

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

    class InitiateMIDISession extends Thread {
        //String stringPacket;
        DatagramSocket socket;
        DatagramPacket sDatagramPacket;
        DatagramPacket rDatagramPacket;
        RtpMidiSession sPacket;
        RtpMidiSession rPacket;
        Timestamp timestamp;
        String ip;
        byte byteArray[];// = new byte[2];
        //RtpMidiSession packet = new RtpMidiSession(new ByteBufferKaitaiStream(byteArray));
//        InitiateMIDISession(String stringPacket){
//            this.stringPacket=stringPacket;
//
//        }
        public InitiateMIDISession(String IP){
            ip = new String(IP);
        }
        @Override
        public void run() {
            //super.run();
            Random ran=new Random();

            long initToken =Math.abs(ran.nextLong());
            long ssrc=Math.abs(ran.nextLong());
                try{

                    socket = new DatagramSocket(5004);
                    byte [] buffer=new byte[1024];

                    sPacket = new RtpMidiSession();
                    sPacket.setValues( new byte[]{'I', 'N'} , initToken,ssrc,"Android" );

                    byte sPacketByteArray[] = sPacket._toByteArray();
                    StringBuilder ss = new StringBuilder(sPacketByteArray.length*2);
                    for (byte b:sPacketByteArray) {
                        ss.append(String.format("%02x",b));
                        ss.append(" ");
                    }

                    Log.v("packetdata",ss.toString());

                    sDatagramPacket=new DatagramPacket(sPacketByteArray , sPacketByteArray.length);
                    rDatagramPacket=new DatagramPacket(buffer , buffer.length);
                    sDatagramPacket.setAddress(InetAddress.getByName(ip));

                    sDatagramPacket.setPort(5004);
                    socket.setSoTimeout(1000);
                    boolean Received=true;
                    socket.send(sDatagramPacket);
                    int count=1;
                    do{
                        try {
                            socket.receive(rDatagramPacket);
                            Received=true;
                        }catch (SocketTimeoutException e) {
                            Received = false;
                            socket.send(sDatagramPacket);
                            count++;
                            textView.append("Initiator attempt: "+count);
                        }
                    }while(!Received && count < 12);



//                    String quote = new String(buffer, 0, rDatagramPacket.getLength());
//                    textView.append("Responder:"+quote);
                    if(Received) {

                        rPacket = new RtpMidiSession(new ByteBufferKaitaiStream(buffer));
                        rPacket._read();
                        if (Arrays.equals(rPacket.command(), new byte[]{'O', 'K'})) {
                            textView.append("Responder: OK");
                            //now start protocol for MIDI port
                            new TimestampSync(ssrc,sDatagramPacket,ip).start();
                        }

                        if (Arrays.equals(rPacket.command(), new byte[]{'N', 'O'}))
                            textView.append("\nResponder: NO");
                        if (Arrays.equals(rPacket.command(), new byte[]{'R', 'L'}))
                            textView.append("\nResponder: RL");
                        if (Arrays.equals(rPacket.command(), new byte[]{'B', 'Y'}))
                            textView.append("\nResponder: BY");
                    }
                    //new TimestampSync(ssrc).start();
                }catch (Exception e){
                    textView.append("Exception at MidiSessionInit:"+e.toString());
                    e.printStackTrace();
                }
                finally {
                    socket.close();
                    textView.append("socket 5004 closed!");
                }
        }
    }
    class TimestampSync extends Thread{
        String ip ;
        long ssrc;
        DatagramSocket socket;

        DatagramPacket sDatagramPacket;
        DatagramPacket rDatagramPacket;
        Timestamp tsPacket;
        Timestamp trPacket;
        byte [] buffer=new byte[1024];
        public TimestampSync(long ssrc, DatagramPacket sDatagramPacket,String ip){
            this.ip = new String(ip);
            this.ssrc=ssrc;
            this.sDatagramPacket=sDatagramPacket;
            tsPacket=new Timestamp();
            rDatagramPacket = new DatagramPacket(buffer, buffer.length);
            tsPacket.setSsrc(ssrc);
            try{
                socket=new DatagramSocket(5005);
            }catch (SocketException socketException){
                socketException.printStackTrace();
                textView.append(socketException.toString());
            }
        }
        @Override
        public void run(){
            try {

                sDatagramPacket.setPort(5005);
                boolean Received = true;
                socket.send(sDatagramPacket);
                int count = 1;
                do {
                    try {
                        socket.receive(rDatagramPacket);
                        Received=true;
                    } catch (SocketTimeoutException e) {
                        Received = false;
                        socket.send(sDatagramPacket);
                        count++;
                        textView.append("Initiator attempt: " + count);
                    }
                } while (!Received && count < 12);

//                    String quote = new String(buffer, 0, rDatagramPacket.getLength());
//                    textView.append("Responder:"+quote);

                RtpMidiSession rPacket = new RtpMidiSession(new ByteBufferKaitaiStream(buffer));
                rPacket._read();
                if (Arrays.equals(rPacket.command(), new byte[]{'O', 'K'})) {
                    textView.append("\nResponder: OK for MIDI  port Session");
                    //start Timestamp Synchronization
                    synchronized (this) {
                        while (true) {

                            tsPacket.setCount(0);
                            tsPacket.setTimestamp1(java.lang.System.currentTimeMillis() * 10);

                            byte sPacketByteArray[] = tsPacket._toByteArray();
                            sDatagramPacket = new DatagramPacket(sPacketByteArray, sPacketByteArray.length);

                            sDatagramPacket.setAddress(InetAddress.getByName(ip));
                            long timestamp3 = 0;

                            sDatagramPacket.setPort(5005);
                            socket.setSoTimeout(10000);
                            Received = true;
                            socket.send(sDatagramPacket);
                            count = 1;
                            do {
                                try {
                                    socket.receive(rDatagramPacket);
                                    Received = true;
                                } catch (SocketTimeoutException e) {
                                    Received = false;
                                    socket.send(sDatagramPacket);
                                    count++;
                                    textView.append("Initiator attempt: " + count);
                                }
                            } while (!Received && count < 12);

                            if (Received) {
                                Timestamp trPacket = new Timestamp(new ByteBufferKaitaiStream(buffer));
                                trPacket._read();
                                textView.append("\nT1 packet received!");
                                //if (trPacket.count() == 1) {
                                tsPacket.setCount(2);
                                tsPacket.setTimestamp1(trPacket.timestamp1());
                                tsPacket.setTimestamp2(trPacket.timestamp2());
                                timestamp3 = java.lang.System.currentTimeMillis() * 10;
                                tsPacket.setTimestamp3(timestamp3);
                                //}
                                sPacketByteArray = tsPacket._toByteArray();
                                sDatagramPacket = new DatagramPacket(sPacketByteArray, sPacketByteArray.length);
                                //now again send the Timestamp
                                sDatagramPacket.setPort(5005);
                                sDatagramPacket.setAddress(InetAddress.getByName(ip));
                                socket.send(sDatagramPacket);
                                //long offset_estimate = ((timestamp3 + trPacket.timestamp1()) / 2) - trPacket.timestamp2();
                                //textView.append("offset_estimate: "+offset_estimate);
                            }

                            //wiat for receive CK time count=0 from the other side the send your time at count=1
                            socket.setSoTimeout(10000);
                            Received = false;
                            try {
                                socket.receive(rDatagramPacket);
                                Received = true;
                            } catch (SocketTimeoutException e) {
                                Received = false;
                                //socket.send(sDatagramPacket);
                                //count++;
                                textView.append("\nDidn't receice CK command from responder timedout: ");
                            }
                            if (Received) {
                                Timestamp trPacket = new Timestamp(new ByteBufferKaitaiStream(buffer));
                                trPacket._read();
                                textView.append("\nFinal packet received!");
                                if (trPacket.count() == 0) {
                                    tsPacket.setCount(1);
                                    tsPacket.setTimestamp1(trPacket.timestamp1());
                                    tsPacket.setTimestamp2(java.lang.System.currentTimeMillis() * 10);
                                    tsPacket.setTimestamp3(0);
                                }
                                sPacketByteArray = tsPacket._toByteArray();
                                sDatagramPacket = new DatagramPacket(sPacketByteArray, sPacketByteArray.length);
                                //now again send the Timestamp
                                sDatagramPacket.setPort(5005);
                                sDatagramPacket.setAddress(InetAddress.getByName(ip));
                                socket.send(sDatagramPacket);
                            }
                            textView.append("Sleeping now..");
                            sleep(10000);
                        }
                    }

                }
                if (Arrays.equals(rPacket.command(), new byte[]{'N', 'O'}))
                    textView.append("\nResponder: NO for MIDI  port Session");


            }catch (Exception e){
                textView.append("Exception at Timestamp:"+e.toString());
                e.printStackTrace();
            }
            finally {
                socket.close();
                textView.append("socket 5005 closed!");
            }
        }
    }
}
