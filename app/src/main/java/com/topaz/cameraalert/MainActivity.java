package com.topaz.cameraalert;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.TabLayout;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.topaz.cameraalert.Activities.SettingsActivity;
import com.topaz.cameraalert.Activities.VideoViewActivity;
import com.topaz.cameraalert.ListViewSwipe.ListViewAdapter;
import com.topaz.cameraalert.ListViewSwipe.OnItemClickListener;
import com.topaz.cameraalert.ListViewSwipe.RecyclerViewAdapter;
import com.topaz.cameraalert.ListViewSwipe.SwipeToDismissTouchListener;
import com.topaz.cameraalert.ListViewSwipe.SwipeableItemClickListener;
import com.topaz.cameraalert.Services.CameraEventService;
import com.topaz.cameraalert.Utils.ImageManager;
import com.topaz.cameraalert.Utils.RESTMgr;
import com.topaz.cameraalert.Model.Camera;
import com.topaz.cameraalert.Model.CameraFile;
import com.topaz.cameraalert.Services.NotifyService;
import com.topaz.cameraalert.Adapters.FileAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, TabLayout.OnTabSelectedListener
{
    private SwipeRefreshLayout swipeRefreshLayout;
    private ArrayList<Camera> cameras;
    private ArrayList<CameraFile> cameraFiles = new ArrayList<CameraFile>();
    private int cameraIndex = 0;
    private int cameraId = 0;
//    private NotificationReceiver nReceiver;
    private boolean showChangedOnly = false;
    private int switchedIndex = -1;
    private RESTMgr _service;
    private SwipeToDismissTouchListener<RecyclerViewAdapter> touchListener;
    private static final int TIME_TO_AUTOMATICALLY_DISMISS_ITEM = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        _service = RESTMgr.getInstance();

        _service.updateSettings(this);

        cameras = new ArrayList<Camera>();

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        final RecyclerView lv = (RecyclerView) findViewById(R.id.cameraFiles);
        registerForContextMenu(lv);

        initListView(lv);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        showChangedOnly = settings.getBoolean("showChangedOnly", false);

        NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nManager.cancel(1001);

/*        nReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.topaz.cameraalert.NOTIFICATION_LISTENER");
        registerReceiver(nReceiver, filter);

        String notificationListenerString = Settings.Secure.getString(this.getContentResolver(), "enabled_notification_listeners");
        //Check notifications access permission
        if (notificationListenerString == null || !notificationListenerString.contains(getPackageName()))
        {
            requestListenerPermission();
        }
        else
        {
            Intent mServiceIntent = new Intent(this, NotifyService.class);
            startService(mServiceIntent);
        }
*/

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        tabLayout.setOnTabSelectedListener(this);

        if (settings.getBoolean("monitorEvents", true) && !CameraEventService.isRunning(this))
        {
            try {
                startService(new Intent(getBaseContext(), CameraEventService.class));
            }
            catch (Exception ex)
            {

            }
        }

        loadCameras();
    }

    private void loadCameras()
    {
        try
        {
            _service.setToken(FirebaseInstanceId.getInstance().getToken(), null);
            _service.getCameras(new RESTMgr.OnTaskCompleted()
            {
                @Override
                public void onTaskCompleted(Object result)
                {
                    loadCamerasResults(result);
                }
            });
        }
        catch (Exception ex)
        {
        }
    }

    private void loadCamerasResults(Object result)
    {
        cameras = new ArrayList<Camera>();
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        try
        {
            tabLayout.removeAllTabs();

            JSONArray ticketArray = (JSONArray) result;
            for (int i = 0; i < ticketArray.length(); i++)
            {
                JSONObject tickObj = (JSONObject) ticketArray.get(i);
                Camera camera = new Camera(tickObj);
                cameras.add(camera);
                tabLayout.addTab(tabLayout.newTab().setText(camera.Name));
            }
        }
        catch (JSONException ex)
        {

        }
        onRefresh();
    }

    private void initListView(final RecyclerView listView)
    {
        final FileAdapter adapter = new FileAdapter(getApplicationContext(), cameraFiles);

        listView.setLayoutManager(new LinearLayoutManager(this));

        listView.setAdapter(adapter);

        touchListener = new SwipeToDismissTouchListener<>(
                        new RecyclerViewAdapter(listView),
                        new SwipeToDismissTouchListener.DismissCallbacks<RecyclerViewAdapter>()
        {
            @Override
            public void onStartSwipe(RecyclerViewAdapter recyclerView, int position)
            {

            }

            @Override
            public boolean canDismiss(int position, boolean swipingRight)
            {
                int firstPos = ((LinearLayoutManager)listView.getLayoutManager()).findFirstVisibleItemPosition();
                View row = listView.getChildAt(position - firstPos);
                if (row != null)
                {
                    TextView text = (TextView) row.findViewById(R.id.txt_mark);

                    CameraFile file = cameraFiles.get(position);
                    if (file != null)
                    {
                        if (file.Changed)
                            text.setText("MARK  ");
                        else
                            text.setText("UNMARK  ");
                    }
                }
                return true;
            }

            @Override
            public void onPendingDismiss(RecyclerViewAdapter recyclerView, int position, boolean swipingRight)
            {
            }

            @Override
            public void onDismiss(RecyclerViewAdapter view, int position, boolean swipingRight)
            {
                final RecyclerView lv = (RecyclerView) findViewById(R.id.cameraFiles);
                CameraFile obj = ((FileAdapter)lv.getAdapter()).getItem(position);

                if (swipingRight)
                {
                    Toast.makeText(MainActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                    deleteFile(obj, false);
                }
                else
                {
                    if (obj.Changed)
                        markRead(obj);
                    else
                        markUnread(obj);
                }
            }
        });

        touchListener.setDismissDelayRight(TIME_TO_AUTOMATICALLY_DISMISS_ITEM);
        touchListener.setDismissDelayLeft(10);

        listView.setOnTouchListener(touchListener);
        listView.setOnScrollListener((RecyclerView.OnScrollListener)touchListener.makeScrollListener());
        listView.addOnItemTouchListener(new SwipeableItemClickListener(this,
            new OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position)
                {
                    if (view.getId() == R.id.txt_delete)
                    {
                        touchListener.processPendingDismisses(true);
                    }
                    else if (view.getId() == R.id.txt_undo)
                    {
                        touchListener.undoPendingDismiss(true);
                    }
                    else
                    {
                        playFile(cameraFiles.get(position));
                    }
                }
            }));
    }

    private boolean isListenerServiceRunnning()
    {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if ("NotifyService".equals(service.service.getClassName()))
            {
                return true;
            }
        }
        return false;
    }

    private void requestListenerPermission()
    {
        Intent requestIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        requestIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(requestIntent);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
//        unregisterReceiver(nReceiver);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        _service.updateSettings(this);

        if (cameras.size() == 0)
        {
            loadCameras();
        }
        else
        {
            onRefresh();
        }
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab)
    {
        cameraIndex = tab.getPosition();
        cameraId = cameras.get(cameraIndex).Id;
        onRefresh();
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab)
    {
        cameraIndex = tab.getPosition();
        cameraId = cameras.get(cameraIndex).Id;
        onRefresh();
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab)
    {
    }

    @Override
    public void onRefresh()
    {
        swipeRefreshLayout.setRefreshing(true);
        loadFiles();
        swipeRefreshLayout.setRefreshing(false);
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (touchListener != null
                && (touchListener.existPendingDismisses(true) || touchListener.existPendingDismisses(false)))
        {
            return;
        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.file_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final RecyclerView lv = (RecyclerView) findViewById(R.id.cameraFiles);
        CameraFile obj = ((FileAdapter)lv.getAdapter()).getItem(info.position);
        switch (item.getItemId())
        {
            case R.id.play_file:
                playFile(obj);
                return true;
            case R.id.mark_done:
                markRead(obj);
                return true;
            case R.id.mark_undone:
                markUnread(obj);
                return true;
            case R.id.delete:
                deleteFile(obj, false);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.show_done).setChecked(showChangedOnly);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

            return true;
        }
        else if (id == R.id.action_live)
        {
            showLiveVideo();
            return true;
        }
        else if (id == R.id.marl_all_done)
        {
            markRead(null);
            return true;
        }
        else if (id == R.id.marl_all_undone)
        {
            markUnread(null);
            return true;
        }
        else if (id == R.id.show_done)
        {
            showChangedOnly ^= true;
            item.setChecked(showChangedOnly);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("showChangedOnly", showChangedOnly);
            editor.commit();
            loadFiles();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showLiveVideo()
    {
        if (cameraIndex >= cameras.size())
            return;
        Camera camera = cameras.get(cameraIndex);

        Intent intent = new Intent(getApplicationContext(), VideoViewActivity.class);

        String liveUrl = camera.LiveIp;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (settings.getBoolean("debugMode", false) == false)
        {
            int sindex = liveUrl.indexOf("@");
            int eindex = liveUrl.lastIndexOf(":");

            liveUrl = liveUrl.substring(0, sindex + 1) + settings.getString("prodServerIP", "ttopaz.duckdns.org") +
                    liveUrl.substring(eindex);
        }

        Bundle bundle = new Bundle();
        bundle.putString("url", liveUrl);
        intent.putExtras(bundle);
        MainActivity.this.startActivity(intent);

      /*  Uri uri = Uri.parse(camera.LiveIp);

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setDataAndType(uri, "video/*");
        MainActivity.this.startActivity(intent);*/
    }

    private void deleteFile(final CameraFile obj, final boolean removeFromList)
    {
        _service.deleteCameraFile(obj.CameraId, obj.File, new RESTMgr.OnTaskCompleted()
        {
            @Override
            public void onTaskCompleted(Object result)
            {
                final RecyclerView lv = (RecyclerView) findViewById(R.id.cameraFiles);
                ((FileAdapter) lv.getAdapter()).remove(obj);
                cameraFiles.remove(obj);
                lv.getAdapter().notifyDataSetChanged();
            }
        });
    }

    private void markRead(CameraFile obj)
    {
        String key = "Read" + Integer.toString(cameras.get(cameraIndex).Id);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Set<String> readFiles = settings.getStringSet(key, new HashSet<String>());

        final RecyclerView lv = (RecyclerView) findViewById(R.id.cameraFiles);

        if (obj == null)
        {
            if (showChangedOnly)
            {
                for (int index = cameraFiles.size() - 1; index >= 0; index--)
                {
                    String code = Integer.toString(cameraFiles.get(index).File.hashCode());
                    if (!readFiles.contains(code))
                    {
                        readFiles.add(code);
                    }
                    ((FileAdapter)lv.getAdapter()).remove(cameraFiles.get(index));
                }
                lv.getAdapter().notifyDataSetChanged();
            }
            else
            {
                readFiles = new HashSet<String>();
                for (int index = 0; index < cameraFiles.size(); index++)
                {
                    readFiles.add(Integer.toString(cameraFiles.get(index).File.hashCode()));
                    cameraFiles.get(index).Changed = false;
                    int firstPos = ((LinearLayoutManager)lv.getLayoutManager()).findFirstVisibleItemPosition();
                    View rowView = lv.getChildAt(index - firstPos);
                    if (rowView != null)
                    {
                        TextView date = (TextView) rowView.findViewById(R.id.file_date);
                        date.setTypeface(null, Typeface.NORMAL);
                        TextView time = (TextView) rowView.findViewById(R.id.file_time_range);
                        time.setTypeface(null, Typeface.NORMAL);
                    }
                }
            }
        }
        else
        {
            String code = Integer.toString(obj.File.hashCode());
            if (!readFiles.contains(code))
            {
                readFiles.add(code);
            }
            obj.Changed = false;

            int index = cameraFiles.indexOf(obj);
            int firstPos = ((LinearLayoutManager)lv.getLayoutManager()).findFirstVisibleItemPosition();
            View rowView = lv.getChildAt(index - firstPos);
            if (rowView != null)
            {
                if (showChangedOnly)
                {
                    ((FileAdapter) lv.getAdapter()).remove(obj);
                    lv.getAdapter().notifyDataSetChanged();
                }
                else
                {
                    TextView date = (TextView) rowView.findViewById(R.id.file_date);
                    date.setTypeface(null, Typeface.NORMAL);
                    TextView time = (TextView) rowView.findViewById(R.id.file_time_range);
                    time.setTypeface(null, Typeface.NORMAL);
                }
            }
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(key, readFiles);
        editor.commit();
    }

    private void markUnread(CameraFile obj)
    {
        String key = "Read" + Integer.toString(cameras.get(cameraIndex).Id);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Set<String> readFiles = settings.getStringSet(key, new HashSet<String>());

        final RecyclerView lv = (RecyclerView) findViewById(R.id.cameraFiles);

        if (obj == null)
        {
            readFiles = new HashSet<String>();

            for (int index = 0; index < cameraFiles.size(); index++)
            {
                cameraFiles.get(index).Changed = true;
                int firstPos = ((LinearLayoutManager)lv.getLayoutManager()).findFirstVisibleItemPosition();
                View rowView = lv.getChildAt(index - firstPos);
                if (rowView != null)
                {
                    TextView date = (TextView) rowView.findViewById(R.id.file_date);
                    date.setTypeface(null, Typeface.BOLD);
                    TextView time = (TextView) rowView.findViewById(R.id.file_time_range);
                    time.setTypeface(null, Typeface.BOLD);
                }
            }
        }
        else
        {
            String code = Integer.toString(obj.File.hashCode());
            if (readFiles.contains(code))
            {
                readFiles.remove(code);
            }
            obj.Changed = true;
            int index = cameraFiles.indexOf(obj);
            int firstPos = ((LinearLayoutManager)lv.getLayoutManager()).findFirstVisibleItemPosition();
            View rowView = lv.getChildAt(index - firstPos);
            if (rowView != null)
            {
                TextView date = (TextView) rowView.findViewById(R.id.file_date);
                date.setTypeface(null, Typeface.BOLD);
                TextView time = (TextView) rowView.findViewById(R.id.file_time_range);
                time.setTypeface(null, Typeface.BOLD);
            }
        }
        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(key, readFiles);
        editor.commit();
    }

    private void playFile(final CameraFile file)
    {
        _service.getCameraFileUrl(file, new RESTMgr.OnTaskCompleted()
        {
            @Override
            public void onTaskCompleted(Object result)
            {
                String video = null;

                if (file.VideoUrl != null)
                {
                    video = (String)result;
                }
                else if (file.VideoUrlProvider != null)
                {
                    try
                    {
                        JSONObject urlObj = (JSONObject) result;
                        video = urlObj.getString("Url");
                    }
                    catch (JSONException ex)
                    {

                    }
                }

                if (video == null)
                {
                    return;
                }
                Uri uri = Uri.parse(video);

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                if (settings.getBoolean("internalPlayer", false) == true)
                {
                    Intent intent = new Intent(getApplicationContext(), VideoViewActivity.class /*WebViewActivity.class*/);

                    Bundle bundle = new Bundle();
                    bundle.putString("url", video);
                    intent.putExtras(bundle);
                    MainActivity.this.startActivity(intent);
                }
                else
                {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setDataAndType(uri, "video/mp4");
                    MainActivity.this.startActivity(intent);
                }

                if (settings.getBoolean("markWatched", true) == true)
                    markRead(file);
            }
        });
    }

    private void loadFiles()
    {
        if (cameraIndex >= cameras.size())
            return;

        cameraFiles = new ArrayList<CameraFile>();
        final RecyclerView lv = (RecyclerView) findViewById(R.id.cameraFiles);
        lv.setTag(null);
        lv.setAdapter(null);

        ImageManager.getInstance().cancelDownloads();

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        final String cameraId = Integer.toString(cameras.get(cameraIndex).Id);
        final int days = Integer.parseInt(settings.getString("days", "2"));

        _service.getCameraFiles(cameraId, days, new RESTMgr.OnTaskCompleted()
        {
            @Override
            public void onTaskCompleted(Object result)
            {
                loadCameraFilesResults(result, cameraId, settings);
            }
        });
    }

    private void loadCameraFilesResults(Object result, final String cameraId, final SharedPreferences settings)
    {
        switchedIndex = 0;
        cameraFiles = new ArrayList<CameraFile>();
        try
        {
            Set<String> readFiles = settings.getStringSet("Read" + cameraId, new HashSet<String>());

            JSONArray ticketArray = (JSONArray) result;
            for (int i = 0; i < ticketArray.length(); i++)
            {
                JSONObject tickObj = (JSONObject) ticketArray.get(i);
                CameraFile file = new CameraFile(cameraId, tickObj);

                String code = Integer.toString(file.File.hashCode());

                if (readFiles.contains(code))
                    file.Changed = false;

                if (file.File.endsWith(".mp4") || file.VideoUrlProvider != null)
                {
                    if (!showChangedOnly || file.Changed)
                        cameraFiles.add(file);
                }
            }

        }
        catch (JSONException ex)
        {

        }

        Collections.sort(cameraFiles, new Comparator<CameraFile>()
        {
            public int compare(CameraFile o1, CameraFile o2)
            {
                return o2.Date.compareTo(o1.Date);
            }
        });

        final RecyclerView lv = (RecyclerView) findViewById(R.id.cameraFiles);
        ViewCompat.setNestedScrollingEnabled(lv, true);

        RecyclerView.Adapter adapter = lv.getAdapter();

        if (adapter == null)
        {
            adapter = new FileAdapter(getApplicationContext(), cameraFiles);
            lv.setAdapter(adapter);
        }
        else
        {
            ((FileAdapter)adapter).setData(cameraFiles);
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

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    public void onStop()
    {
        super.onStop();
    }

    class NotificationReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String message = intent.getStringExtra("reolink_notification_event");
            CreateAlert(message, 1, new Date());
            onRefresh();
        }
    }
}
