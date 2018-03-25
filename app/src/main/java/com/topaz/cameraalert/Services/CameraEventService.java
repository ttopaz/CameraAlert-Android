package com.topaz.cameraalert.Services;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;

import com.topaz.cameraalert.MainActivity;
import com.topaz.cameraalert.Model.Camera;
import com.topaz.cameraalert.R;
import com.topaz.cameraalert.Utils.RESTMgr;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class CameraEventService extends Service
{
    private Timer _eventTimer;
    private final Handler _timerHandler = new Handler();

    public CameraEventService()
    {
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    public static boolean isRunning(Context context)
    {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if ("com.topaz.cameraalert.Services.CameraEventService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        NotificationCompat.Builder ncomp = new NotificationCompat.Builder(this);
        ncomp.setTicker("Camera Alert");
        ncomp.setContentTitle("Camera Alert");
        ncomp.setContentText("Service is running");
        ncomp.setSmallIcon(R.mipmap.ic_launcher);
        ncomp.setAutoCancel(true);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
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

        Notification noti = ncomp.build();

        noti.flags = Notification.FLAG_FOREGROUND_SERVICE;
        startForeground(1222, noti);

        startEventTimer();

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        stopEventTimer();
        stopForeground(true);
    }

    private void startEventTimer()
    {
        _eventTimer = new Timer();

        TimerTask eventTimer = new TimerTask() {
            public void run() {
                _timerHandler.post(new Runnable() {
                    public void run()
                    {
                        RESTMgr.getInstance().getEvents(new RESTMgr.OnTaskCompleted()
                        {
                            @Override
                            public void onTaskCompleted(Object result)
                            {
                                JSONArray eventData = (JSONArray) result;
                                analyzeFileCreateEvents(eventData);
                            }
                        });
                    }
                });
            }
        };

        _eventTimer.schedule(eventTimer, 5000, 20000);
    }

    private void stopEventTimer()
    {
        if (_eventTimer != null)
        {
            _eventTimer.cancel();
            _eventTimer = null;
        }
    }

    private void analyzeFileCreateEvents(JSONArray eventData)
    {
        TimeZone utc = TimeZone.getTimeZone("UTC");

        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        f.setTimeZone(utc);

        GregorianCalendar cal = new GregorianCalendar(utc);

        for (int i = 0; i < eventData.length(); i++)
        {
            try
            {
                JSONObject cameraEvent = (JSONObject)eventData.get(i);

                String fileName = cameraEvent.getString("file");
                String eventTime = cameraEvent.getString("date");
                int cameraId = Integer.parseInt(cameraEvent.getString("cameraId"));
                String cameraName = cameraEvent.getString("cameraName");

                cal.setTime(f.parse(eventTime));
                Date eventDate = cal.getTime();

                Date currentDate = new Date();

                long different = currentDate.getTime() - eventDate.getTime();
                long elapsedSeconds = different / 1000;

                //                if (elapsedSeconds < 200)
                {
                    String dateString = android.text.format.DateFormat.format("MM/dd/yyyy hh:mm:ss a", eventDate).toString();
                    CreateAlert(String.format("Motion detected at %s on %s", cameraName, dateString), cameraId, eventDate);
                }
            }
            catch (JSONException ex)
            {

            }
            catch (ParseException ex)
            {

            }
        }
    }

    private void CreateAlert(String text, int cameraId, Date time)
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
