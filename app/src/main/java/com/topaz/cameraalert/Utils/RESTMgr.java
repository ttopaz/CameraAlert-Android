package com.topaz.cameraalert.Utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Base64;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

    private static RESTMgr Instance;
    private String serverIP;
    private int serverPort;
    private Context appContext;

    private String USERNAME = "admin";
    private String PASSWORD = "admin";

    public static RESTMgr getInstance()
    {
        if (Instance == null)
            Instance = new RESTMgr();
        return Instance;
    }

    public void updateSettings(Context context)
    {
        appContext = context;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if (settings.getBoolean("debugMode", false) == true)
            serverIP = settings.getString("debugServerIP", "192.168.1.120");
        else
            serverIP = settings.getString("prodServerIP", "ttopaz.duckdns.org");
        serverPort = Integer.parseInt(settings.getString("serverPort", "3000"));
    }

    private String getServerAddress()
    {
 //       return "http://admin:admin@192.168.1.120:3000";
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
    public String getCameraFileUrl(String cameraId, String file)
    {
        try {
            file = URLEncoder.encode(file, "utf-8");
        } catch (UnsupportedEncodingException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return getServerAddress() + "/PlayCameraFile?Id=" + cameraId + "&File=" + file;
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

    public void getCameraImage(String url, OnTaskCompleted listener)
    {
        final String basicAuth = getAuth();
        new GetTask(listener)
                .addHeader("Authorization", basicAuth)
                .execute(url);
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

        public GetTask(OnTaskCompleted listener)
        {
            this.listener = listener;
            headers = new HashMap<String, String>();
        }

        public GetTask addHeader(String key, String value)
        {
            headers.put(key, value);
            return this;
        }

        @Override
        protected void onPreExecute() {
            progressDialog= new ProgressDialog(appContext);
            progressDialog.setMessage("Please wait...");
            progressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            try
            {
                HttpGet get = new HttpGet(uri[0]);
                for (String key : headers.keySet())
                    get.addHeader(key, headers.get(key));
                response = httpclient.execute(get);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK)
                {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    responseString = out.toString();
                } else
                {
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            }
            catch (ClientProtocolException e)
            {
                //TODO Handle problems..
            }
            catch (IOException e)
            {
                //TODO Handle problems..
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);
            if (progressDialog.isShowing())
                progressDialog.dismiss();
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
        protected String doInBackground(String... uri)
        {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            try
            {
                HttpPost post = new HttpPost(uri[0]);
                for (String key : headers.keySet())
                    post.addHeader(key, headers.get(key));
                post.setHeader("Content-type", "application/json");
                if (data != null)
                {
                    StringEntity se = new StringEntity(data.toString());
                    se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                    post.setEntity(se);
                }
                response = httpclient.execute(post);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK)
                {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    responseString = out.toString();
                }
                else
                {
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            }
            catch (ClientProtocolException e)
            {
                //TODO Handle problems..
            }
            catch (IOException e)
            {
                //TODO Handle problems..
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
    public class DeleteTask extends AsyncTask<String, String, String> {

        private OnTaskCompleted listener = null;
        private String responseString = null;
        private HashMap<String, String> headers;

        public DeleteTask(OnTaskCompleted listener)
        {
            this.listener = listener;
            headers = new HashMap<String, String>();
        }

        public DeleteTask addHeader(String key, String value)
        {
            headers.put(key, value);
            return this;
        }
        public DeleteTask setData(JSONObject data)
        {
            return this;
        }

        @Override
        protected String doInBackground(String... uri)
        {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            try {
                HttpDelete del = new HttpDelete(uri[0]);
                for (String key : headers.keySet())
                    del.addHeader(key, headers.get(key));
                del.setHeader("Content-type", "application/json");
                response = httpclient.execute(del);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK)
                {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    responseString = out.toString();
                }
                else
                {
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            }
            catch (ClientProtocolException e)
            {
                //TODO Handle problems..
            }
            catch (IOException e)
            {
                //TODO Handle problems..
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
