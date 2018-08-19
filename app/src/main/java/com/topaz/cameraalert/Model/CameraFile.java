package com.topaz.cameraalert.Model;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by Tony on 1/13/2017.
 */
public class CameraFile
{
    public String CameraId;
    public String File;
    public String Path;
    public String ImagePath;
    public Date Date;
    public Date CreateDate;
    public long Size;
    public boolean Changed;

    public CameraFile()
    {

    }

    public CameraFile(String cameraId, JSONObject json)
    {
        try
        {
            this.File = json.getString("File");
            this.Path = json.getString("Path");

            if (json.has("ImagePath"))
            {
                this.ImagePath = json.getString("ImagePath");
            }

            TimeZone utc = TimeZone.getTimeZone("UTC");
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            f.setTimeZone(utc);

            GregorianCalendar cal = new GregorianCalendar(utc);
            cal.setTime(f.parse(json.getString("Date")));
            this.Date = cal.getTime();

            parseCreateDate();

            this.Size = json.getLong("Size");
            this.Changed = true;
            this.CameraId = cameraId;
        }
        catch (ParseException ex)
        {

        }
        catch (JSONException ex)
        {

        }
    }

    private void parseCreateDate()
    {
        this.CreateDate = this.Date;

        try
        {
            Calendar cal = Calendar.getInstance();

            int year = Integer.parseInt(this.File.substring(3, 7));
            int mon = Integer.parseInt(this.File.substring(7, 9));
            int day = Integer.parseInt(this.File.substring(9, 11));
            int hr = Integer.parseInt(this.File.substring(11, 13));
            int mn = Integer.parseInt(this.File.substring(13, 15));
            int sc = Integer.parseInt(this.File.substring(15, 17));

            cal.set(year, mon - 1, day, hr, mn, sc);
            this.CreateDate = cal.getTime();
        }
        catch (Exception ex) {}
    }
}
