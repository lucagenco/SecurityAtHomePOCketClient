package helha.luca.testsocket;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

import tech.gusavila92.websocketclient.WebSocketClient;

public class MainActivity extends AppCompatActivity {
    //DECLARATION VARIABLES
    private WebSocketClient webSocketClient;
    private static String URL = "ws://10.0.2.2:8080/SocketSendToken/actions";
    private JSONObject json_connexion;
    private JSONObject json_message;
    private JSONObject json_received;
    //DECLARATION WIDGETS
    private EditText et_connexion_token;
    private EditText et_connexion_type;
    private Button btn_connexion;

    private EditText et_message_content;
    private EditText et_message_type;
    private Button btn_message_envoyer;

    private TextView tv_message_text;
    private TextView tv_connexion_status;
    //NOTIFICATION
    private String CHANNEL_ID = "my_channel_01";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //SET VIEW
        setContentView(R.layout.activity_main);
        //CREATE WEB SOCKET
        createWebSocketClient();
        // SET WIDGETS
        /*CONNEXION*/
        et_connexion_token = findViewById(R.id.et_connexion_token);
        et_connexion_type = findViewById(R.id.et_connexion_type);
        btn_connexion = findViewById(R.id.btn_connexion);
        /*MESSAGES*/
        et_message_content = findViewById(R.id.et_message_content);
        et_message_type = findViewById(R.id.et_message_type);
        btn_message_envoyer = findViewById(R.id.btn_message_envoyer);

        tv_message_text = findViewById(R.id.tv_message_text);
        tv_connexion_status = findViewById(R.id.tv_connexion_status);

        //LISTENERS

        /*CONNEXION*/
        btn_connexion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //CONSTRUCT JSON CONNEXION
                try {
                    json_connexion = new JSONObject();
                    json_connexion.put("action", "initSession");
                    json_connexion.put("token", et_connexion_token.getText().toString());
                    json_connexion.put("type", et_connexion_type.getText().toString());
                    //SEND TO WEB SOCKET
                    webSocketClient.send(json_connexion.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        /*MESSAGE*/
        btn_message_envoyer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //CONSTRUCT JSON MESSAGE
                try {
                    json_message = new JSONObject();
                    json_message.put("action", "sendMessageToType");
                    json_message.put("message", et_message_content.getText().toString());
                    json_message.put("type", et_message_type.getText().toString());
                    //SEND TO WEB SOCKET
                    webSocketClient.send(json_message.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void createWebSocketClient() {
        //CREATE URI
        URI uri;
        try {
            uri = new URI(URL);
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        //CONSTRUCT WEB SOCKET
        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {

            }

            @Override
            public void onTextReceived(final String message) {
                //RUN UI THREAD BECAUSE WEB SOCKET TAKE CURRENT THREAD
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //CREATE JSON RECEIVED FROM MESSAGE
                        try {
                            json_received = new JSONObject(message);
                            if(json_received.getString("action").equals("initSession")){
                                //SET TEXTVIEW
                                tv_connexion_status.setTextColor(getResources().getColor(R.color.colorPrimary));
                                tv_connexion_status.setText("Connecté");
                            }else if(json_received.getString("action").equals("sendMessageToType")){
                                //SET TEXTVIEW
                                tv_message_text.setTextColor(getResources().getColor(R.color.colorPrimary));
                                tv_message_text.setText("Action : " + json_received.getString("action") + "\n" + "Message : " + json_received.getString("message") + "\n" + "Type : " + json_received.getString("type"));
                                sendNotification();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onBinaryReceived(byte[] data) {
                System.out.println("onBinaryReceived");
            }

            @Override
            public void onPingReceived(byte[] data) {
                System.out.println("onPingReceived");
            }

            @Override
            public void onPongReceived(byte[] data) {
                System.out.println("onPongReceived");
            }

            @Override
            public void onException(Exception e) {
                Log.d("erroe_message", e.getMessage());
            }

            @Override
            public void onCloseReceived() {
                System.out.println("onCloseReceived");
            }
        };

        webSocketClient.setConnectTimeout(10000);
        webSocketClient.setReadTimeout(60000);
        webSocketClient.enableAutomaticReconnection(5000);
        webSocketClient.connect();
    }

    public void sendNotification() throws JSONException {
        int NOTIFICATION_ID = 234;

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            CharSequence name = "my_channel";
            String Description = "This is my channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(Description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Nouvelle alerte !")
                .setContentText(json_received.getString("message"));

        Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
