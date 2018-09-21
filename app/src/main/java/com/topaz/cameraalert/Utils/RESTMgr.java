package com.topaz.cameraalert.Utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.topaz.cameraalert.Model.CameraFile;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

/**
 * Created by Tony on 1/13/2017.
 */
public class RESTMgr
{
    public interface OnTaskCompleted {
        void onTaskCompleted(Object result);
    }

    private static RESTMgr instance;
    private String serverIP;
    private int serverPort;
    private Context appContext;

    // need to change to actual values
    private String USERNAME = "admin";
    private String PASSWORD = "admin";

    public static RESTMgr getInstance()
    {
        if (instance == null)
            instance = new RESTMgr();
        return instance;
    }

    public void updateSettings(Context context)
    {
        appContext = context;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if (settings.getBoolean("debugMode", false) == true)
            serverIP = settings.getString("debugServerIP", "192.168.1.120");
        else
            serverIP = settings.getString("prodServerIP", "99.99.99.99");
        serverPort = Integer.parseInt(settings.getString("serverPort", "3500"));
    }

    private String getServerAddress()
    {
        return String.format("http://%s:%s@%s:%d", USERNAME, PASSWORD, serverIP, serverPort);
    }

    public void login(String user, String pass, OnTaskCompleted listener)
    {
        new GetTask(listener)
                .addHeader("username", user)
                .addHeader("password", pass)
                .execute(getServerAddress() + "/login");
    }
    public void register(String name, String user, String pass, OnTaskCompleted listener)
    {
        JSONObject data = new JSONObject();
        try {
            data.put("name", name);
            data.put("username", user);
            data.put("password", pass);
            new PostTask(listener)
                    .setData(data)
                    .execute(getServerAddress() + "/user");
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }
    public String getAuth()
    {
        String userPass = String.format("%s:%s", USERNAME, PASSWORD);
        return "Basic " + Base64.encodeToString(userPass.getBytes(), Base64.NO_WRAP);
    }
    public void getCameras(OnTaskCompleted listener)
    {
        final String basicAuth = getAuth();
        new GetTask(listener)
                .addHeader("Authorization", basicAuth)
                .execute(getServerAddress() + "/Cameras");
    }
    public void getCameraFiles(String cameraId, int days, OnTaskCompleted listener)
    {
        final String basicAuth = getAuth();
        new GetTask(listener)
                .addHeader("Authorization", basicAuth)
                .execute(getServerAddress() + "/CameraFiles?Id=" + cameraId + "&Days=" + days + "&Filter=.mp4");
    }
    public void getCameraFileStream(String cameraId, String file, OnTaskCompleted listener)
    {
        new GetTask(listener)
                .execute(getServerAddress() + "/PlayCameraFile?Id=" + cameraId + "&File=" + file);
    }

    public void getCameraFileUrl(CameraFile file, OnTaskCompleted listener)
    {
        if (file.VideoUrl != null)
        {
            listener.onTaskCompleted(getServerAddress() + file.VideoUrl);
        }
        else if (file.VideoUrlProvider != null)
        {
            final String basicAuth = getAuth();
            new GetTask(listener).addHeader("Authorization", basicAuth)
                    .execute(getServerAddress() + file.VideoUrlProvider);
        }

/*        try {
            file = URLEncoder.encode(file, "utf-8");
        } catch (UnsupportedEncodingException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return getServerAddress() + "/PlayCameraFile?Id=" + cameraId + "&File=" + file;*/
    }
    public String getCameraImageUrl(String cameraId, String file)
    {
        try {
            file = URLEncoder.encode(file, "utf-8");
        } catch (UnsupportedEncodingException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return getServerAddress() + "/GetCameraImage?Id=" + cameraId + "&File=" + file;
    }

    public void getCameraEvent(String cameraId, OnTaskCompleted listener)
    {
        final String basicAuth = getAuth();
        new GetTask(listener)
                .addHeader("Authorization", basicAuth)
                .execute(getServerAddress() + "/GetCameraEvent?Id=" + cameraId);
    }

    public void getEvents(OnTaskCompleted listener)
    {
        final String basicAuth = getAuth();
        new GetTask(listener, true)
                .addHeader("Authorization", basicAuth)
                .execute(getServerAddress() + "/GetEvents");
    }

    public void getCameraImage(String url, OnTaskCompleted listener)
    {
        final String basicAuth = getAuth();
        new GetTask(listener)
                .addHeader("Authorization", basicAuth)
                .execute(url);
    }

    public void setToken(String token, OnTaskCompleted listener)
    {
        final String basicAuth = getAuth();
        JSONObject data = new JSONObject();
        try
        {
            data.put("Token", token);
            new PostTask(listener)
                    .addHeader("Authorization", basicAuth)
                    .setData(data)
                    .execute(getServerAddress() + "/SetToken");
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    public void deleteCameraFile(String cameraId, String file, OnTaskCompleted listener)
    {
        final String basicAuth = getAuth();
        JSONObject data = new JSONObject();
        try
        {
            data.put("Id", cameraId);
            data.put("File", file);
            new PostTask(listener)
                    .addHeader("Authorization", basicAuth)
                    .setData(data)
                    .execute(getServerAddress() + "/DeleteCameraFile");
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    public class GetTask extends AsyncTask<String, String, String>
    {
        private OnTaskCompleted listener = null;
        private String responseString = null;
        private HashMap<String, String> headers;
        private ProgressDialog progressDialog;
        private boolean showProgess;

        public GetTask(OnTaskCompleted listener, boolean... noProgress)
        {
            this.listener = listener;
            headers = new HashMap<String, String>();

            if (noProgress.length > 0)
            {
                showProgess = !noProgress[0];
            }
            else
            {
                showProgess = true;
            }
        }

        public GetTask addHeader(String key, String value)
        {
            headers.put(key, value);
            return this;
        }

        @Override
        protected void onPreExecute() {
            if (showProgess)
            {
                progressDialog = new ProgressDialog(appContext);
                progressDialog.setMessage("Please wait...");
                progressDialog.show();
            }
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... urls)
        {
            try
            {
                URL uri = new URL(urls[0]);

                HttpURLConnection urlConnection = (HttpURLConnection) uri.openConnection();
                urlConnection.setConnectTimeout(2000);
                urlConnection.setRequestMethod("GET");
                for (String key : headers.keySet())
                    urlConnection.setRequestProperty(key, headers.get(key));
                urlConnection.setDoInput(true);
                urlConnection.connect();

                int statusCode = urlConnection.getResponseCode();
                if (statusCode != HttpURLConnection.HTTP_OK)
                {
                    return null;
                }

                InputStream inputStream = urlConnection.getInputStream();
                if (inputStream != null)
                {
                    BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuffer sb = new StringBuffer();
                    String line;

                    while ((line = rd.readLine()) != null) {
                        sb.append(line);
                    }
                    responseString = sb.toString();
                    rd.close();
                }
            }
            catch (IOException e)
            {

            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);

            if (showProgess)
            {
                if (progressDialog.isShowing())
                    progressDialog.dismiss();
            }
            try
            {
                if (this.responseString != null)
                {
                    Object json = new JSONTokener(this.responseString).nextValue();
                    // call callback
                    if (listener != null)
                        listener.onTaskCompleted(json);
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
    }

    public class PostTask extends AsyncTask<String, String, String>
    {
        private OnTaskCompleted listener = null;
        private String responseString = null;
        private HashMap<String, String> headers;
        private JSONObject data = null;

        public PostTask(OnTaskCompleted listener)
        {
            this.listener = listener;
            headers = new HashMap<String, String>();
        }

        public PostTask addHeader(String key, String value)
        {
            headers.put(key, value);
            return this;
        }
        public PostTask setData(JSONObject data)
        {
            this.data = data;
            return this;
        }

        @Override
        protected String doInBackground(String... urls)
        {
            try
            {
                URL uri = new URL(urls[0]);

                HttpURLConnection urlConnection = (HttpURLConnection) uri.openConnection();
                urlConnection.setConnectTimeout(2000);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-type", "application/json");
                for (String key : headers.keySet())
                    urlConnection.setRequestProperty(key, headers.get(key));

                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);

                if (data != null)
                {
                    OutputStream os = urlConnection.getOutputStream();
                    os.write(data.toString().getBytes("UTF-8"));
                    os.close();
                }

                urlConnection.connect();

                int statusCode = urlConnection.getResponseCode();
                if (statusCode != HttpURLConnection.HTTP_OK)
                {
                    return null;
                }

                InputStream inputStream = urlConnection.getInputStream();
                if (inputStream != null)
                {
                    BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuffer sb = new StringBuffer();
                    String line;

                    while ((line = rd.readLine()) != null) {
                        sb.append(line);
                    }
                    responseString = sb.toString();
                    rd.close();
                }
            }
            catch (IOException e)
            {

            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);
            try
            {
                if (this.responseString != null)
                {
                    Object json = new JSONTokener(this.responseString).nextValue();
                    // call callback
                    if (listener != null)
                        listener.onTaskCompleted(json);
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
    }
}
