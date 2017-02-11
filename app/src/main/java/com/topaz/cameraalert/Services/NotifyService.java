package com.topaz.cameraalert.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.topaz.cameraalert.MainActivity;
import com.topaz.cameraalert.R;

import java.util.Calendar;

public class NotifyService extends NotificationListenerService {

    private String TAG = this.getClass().getSimpleName();
    private NotifyServiceReceiver nlservicereciver;
    @Override
    public void onCreate() {
        super.onCreate();
        nlservicereciver = new NotifyServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.topaz.cameraalert.NOTIFICATION_LISTENER_SERVICE");
        registerReceiver(nlservicereciver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nlservicereciver);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn.getPackageName().toLowerCase().indexOf("reolink") != -1)
        {
            String msg = sbn.getNotification().extras.getString("android.text");
            CreateAlert(msg);

            try
            {
                cancelNotification(sbn.getKey());
            }
            catch (Exception ex) {}
            try
            {
                cancelNotification(sbn.getPackageName(), sbn.getTag(), sbn.getId());
            }
            catch (Exception ex) {}
/*            Intent i = new Intent("com.example.tony.cameraalert.NOTIFICATION_LISTENER");
            i.putExtra("reolink_notification_event", msg);
            sendBroadcast(i);*/
        }
      /*  if (sbn.getPackageName().toLowerCase().indexOf("reolink") != -1)
        {
            Intent i = new  Intent("com.example.tony.cameraalert.NOTIFICATION_LISTENER");
            i.putExtra("reolink_notification_event", sbn.getNotification().tickerText);
            sendBroadcast(i);
//            NotifyUser(sbn);
//            cancelNotification(sbn.getKey());
        } */
    }

    private void CreateAlert(String text)
    {
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
        ncomp.setPriority(Notification.PRIORITY_HIGH);

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra("notifyMsg", text);
        Calendar c = Calendar.getInstance();
        int seconds = c.get(Calendar.SECOND);
        resultIntent.putExtra("notifyTime", seconds);

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

        @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
/*        Log.i(TAG,"********** onNOtificationRemoved");
        Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText +"\t" + sbn.getPackageName());
        Intent i = new  Intent("com.example.tony.cameraalert.NOTIFICATION_LISTENER");
        i.putExtra("notification_event","onNotificationRemoved :" + sbn.getPackageName() + "\n");

        sendBroadcast(i);*/
    }

    class NotifyServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra("command").equals("clearall")){
                NotifyService.this.cancelAllNotifications();
            }
            else if(intent.getStringExtra("command").equals("list")){
                Intent i1 = new  Intent("com.topaz.cameraalert.NOTIFICATION_LISTENER");
                i1.putExtra("notification_event","=====================");
                sendBroadcast(i1);
                int i=1;
                for (StatusBarNotification sbn : NotifyService.this.getActiveNotifications()) {
                    Intent i2 = new  Intent("com.topaz.cameraalert.NOTIFICATION_LISTENER");
                    i2.putExtra("notification_event",i +" " + sbn.getPackageName() + "\n");
                    sendBroadcast(i2);
                    i++;
                }
                Intent i3 = new  Intent("com.topaz.cameraalert.NOTIFICATION_LISTENER");
                i3.putExtra("notification_event","===== Notification List ====");
                sendBroadcast(i3);

            }

        }
    }

}
