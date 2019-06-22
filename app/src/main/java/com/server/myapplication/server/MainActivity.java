package com.server.myapplication.server;

import android.annotation.SuppressLint;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    ServerSocket serverSocket;
    Thread Thread1 = null;

    TextView tvIP, tvPort;
    TextView tvMessages;

    EditText etMessage;
    Button btnSend;

    public static String SERVER_IP = "";
    public static final int SERVER_PORT = 8080;

    String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvIP = findViewById(R.id.tvIP);
        tvPort = findViewById(R.id.tvPort);
        tvMessages = findViewById(R.id.tvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        try {
            SERVER_IP = getLocalIpAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        System.out.println("CREATING THREAD 1 FOR CONNECTION!!!");
        Thread1 = new Thread(new Thread1());
        Thread1.start();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                message = etMessage.getText().toString().trim();
                if (!message.isEmpty()) {
                    System.out.println("STARTING THREAD 3 TO SEND MESSAGES TO THE SERVER");
                    new Thread(new Thread3(message)).start();
                }
            }
        });
    }

    private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array())
                .getHostAddress();
    }

    private PrintWriter out;
    private BufferedReader in;

    class Thread1 implements Runnable {

        @Override
        public void run() {
            System.out.println("THREAD 1 IS RUNNING...");
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        tvMessages.setText("Not connected");
                        tvIP.setText("IP: " + SERVER_IP);
                        tvPort.setText("Port: " + SERVER_PORT);
                    }
                });
                try {
                    socket = serverSocket.accept();

                    out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())),
                            true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvMessages.setText("Connected\n");
                        }
                    });

                    System.out.println("STARTING THREAD 2 FOR READING MESSAGES FROM THE SERVER");
                    new Thread(new Thread2()).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // thread 2 is for reading
    private class Thread2 implements Runnable {
        @Override
        public void run() {
            System.out.println("THREAD 2 IS RUNNING...");
            while (true) {
                try {
                    System.out.println("msg....");
                    System.out.println("reading in message");

                    final String message = in.readLine();

                    System.out.println("message received: " + message);

                    if (message != null && !message.isEmpty()) {
                        System.out.println("msg is not empty");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvMessages.append("client:" + message + "\n");
                            }
                        });
                    } else {
                        System.out.println("message is empty");
                        serverSocket.close();
                        Thread1 = new Thread(new Thread1());
                        Thread1.start();
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // thread 3 is for writing
    class Thread3 implements Runnable {
        private String message;

        Thread3(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            System.out.println("THREAD 3 IS RUNNING...");
            System.out.println("writing out the message: " + message);

            out.println(message);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvMessages.append("server: " + message + "\n");
                    etMessage.setText("");
                }
            });
        }
    }
}
