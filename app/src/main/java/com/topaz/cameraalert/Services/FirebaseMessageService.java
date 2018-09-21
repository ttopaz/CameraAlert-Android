package com.topaz.cameraalert.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.topaz.cameraalert.MainActivity;
import com.topaz.cameraalert.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Map;

public class FirebaseMessageService extends FirebaseMessagingService
{
    private static final String TAG = FirebaseMessageService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // ...

        TimeZone utc = TimeZone.getTimeZone("UTC");

        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        f.setTimeZone(utc);

        GregorianCalendar cal = new GregorianCalendar(utc);

        //Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0)
        {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            try
            {
                java.util.Map<String, String> map = remoteMessage.getData();

                String eventTime = map.get("date");
                int cameraId = Integer.parseInt(map.get("cameraId"));
                String cameraName = map.get("cameraName");

                cal.setTime(f.parse(eventTime));
                Date eventDate = cal.getTime();

                CreateAlert(String.format("Motion detected at %s", cameraName), cameraId, eventDate);
            }
            catch (ParseException ex)
            {
                Log.d(TAG, "Parse error");
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    private void CreateAlert(String text, int cameraId, Date time)
    {
        Log.d(TAG, "Creating alert");
        NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder ncomp = new NotificationCompat.Builder(this);
        ncomp.setContentTitle("Camera Alert");
        ncomp.setContentText(text);
        ncomp.setTicker(text);
        ncomp.setSmallIcon(R.mipmap.ic_launcher);
        ncomp.setAutoCancel(true);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        String sound = settings.getString("sound", "");
        if (sound != "")
            ncomp.setSound(Uri.parse(sound));

        ncomp.setDefaults(Notification.DEFAULT_ALL);
        ncomp.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
        ncomp.setPriority(Notification.PRIORITY_HIGH);
        //        ncomp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra("notifyMsg", text);
        resultIntent.putExtra("notifyTime", time);
        resultIntent.putExtra("notifyCameraId", cameraId);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        ncomp.setContentIntent(resultPendingIntent);

        nManager.notify(1001, ncomp.build());
    }
}
