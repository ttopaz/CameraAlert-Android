package com.topaz.cameraalert.Model;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by Tony on 1/14/2017.
 */
public class Camera
{
    public int Id;
    public String Name;
    public String LiveIp;
    public String Files;

    public Camera()
    {

    }

    public Camera(JSONObject json)
    {
        try {
            this.Id = json.getInt("Id");
            this.Name = json.getString("Name");
            this.LiveIp = json.getString("LiveIp");
            this.Files = json.getString("Files");
        }
        catch (JSONException ex)
        {

        }
    }
}
