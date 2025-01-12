package com.example.iot_project_test;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.jcraft.jsch.Buffer;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
// import org.eclipse.paho.client.mqttv3.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.StreamGobbler;


public class MainActivity extends AppCompatActivity {

    private boolean locked = false;
    private static final String SERVER_URI = "tcp://192.168.0.20:1883";
    private static final String TAG = "MainActivity";
    private final String T_MOTION = "home/motion";
    private final String T_DOOR = "home/door";
    private static final String BROKER_URI = "tcp://192.168.0.20:1883";

    private void setupMQTT() {
        String clientId = "App";

        try {
            MqttClient mqtt_client = new MqttClient(SERVER_URI, clientId, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            mqtt_client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e("MQTT", "Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    if (locked) {
                        String payload = new String(message.getPayload());
                        Log.d("MQTT", "Message: " + payload + ", Topic " + topic);
                        if (topic.equals(T_MOTION)) {
                            String state = new String(message.getPayload());
                            runOnUiThread(() -> updateMotion(state));
                        }
                        if (topic.equals(T_DOOR)) {
                            String doorState = new String(message.getPayload());
                            runOnUiThread(() -> updateDoor(doorState));
                        }
                    }

                    /* Todo: Fix autoupdate
                    else {
                        runOnUiThread(() -> updateMotion(""));
                        runOnUiThread(() -> updateDoor(""));
                    }
                     */
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Successful delivery: " + token.getMessageId());
                }
            });

            mqtt_client.connect(options);
            mqtt_client.subscribe(T_MOTION, 1);
            mqtt_client.subscribe(T_DOOR, 1);
            Log.d("MQTT", "Connected & subscribed");
        } catch (MqttException e) {
            e.printStackTrace();
            Log.d("MQTT", "Connection error: " + e.getMessage());
        }
    }

    private void updateMotion(String state) {
        runOnUiThread(() -> {
            TextView txv_temp = findViewById(R.id.txv_motion);
            txv_temp.setText(state);
        });
    }

    private void updateDoor(String state) {
        TextView txv_door = findViewById(R.id.door_state);
        txv_door.setText(state);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        setupMQTT();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void handleRemoteSwitch(View view) {
        final Button lock_btn = findViewById(R.id.lock_button);
        final ImageView shield = findViewById(R.id.shield);
        if (locked) {

            new AsyncTask<Integer, Void, Void>() {
                @Override
                protected Void doInBackground(Integer... params) {
                    run("tdtool --off 2");
                    return null;
                }

                @Override
                protected void onPostExecute(Void v) {
                    locked = false;
                    lock_btn.setText("LOCK");
                    lock_btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock_24dp, 0, 0, 0);
                    lock_btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#8A1919")));
                    shield.setImageResource(R.drawable.shield_144dp);

                }

            }.execute(1);

        } else {

            new AsyncTask<Integer, Void, Void>() {
                @Override
                protected Void doInBackground(Integer... params) {
                    run("tdtool --on 2");
                    // run("tdtool-improved -t");
                    return null;
                }

                @Override
                protected void onPostExecute(Void v) {
                    locked = true;
                    lock_btn.setText("UNLOCK");
                    lock_btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock_open_24dp, 0, 0, 0);
                    lock_btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#006600")));
                    shield.setImageResource(R.drawable.shield_locked_144dp);
                    // txv_motion.setText(terminalOutput);
                }
            }.execute(1);
        }
    }

    public void run(String command) {
        String hostname = "192.168.0.20";
        String username = "USERNAME";
        String password = "PASSWORD";

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, hostname, 22);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            channel.setInputStream(null);
            InputStream is = channel.getInputStream();
            channel.connect();

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

            StringBuilder output = new StringBuilder();
            output.append(line);
            // terminalOutput = output.toString();

            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}