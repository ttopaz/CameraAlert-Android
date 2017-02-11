package com.topaz.cameraalert.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.topaz.cameraalert.R;

import org.apache.http.HttpStatus;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Tony on 2/11/2017.
 */
public class ImageDownloader extends AsyncTask<String, Void, Bitmap>
{
    private final WeakReference<ImageView> imageViewReference;

    public ImageDownloader(ImageView imageView)
    {
        imageViewReference = new WeakReference<ImageView>(imageView);
    }

    @Override
    protected Bitmap doInBackground(String... params)
    {
//        if (Integer.toString(cameraId) != params[1])
  //          return null;
        return downloadBitmap(params[0]);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap)
    {
        if (isCancelled())
        {
            bitmap = null;
        }

        if (imageViewReference != null)
        {
            ImageView imageView = imageViewReference.get();
            if (imageView != null)
            {
                if (bitmap != null)
                {
                    imageView.setImageBitmap(bitmap);
                }
                else
                {
                    Drawable placeholder = imageView.getContext().getResources().getDrawable(R.drawable.placeholder);
                    imageView.setImageDrawable(placeholder);
                }
            }
        }
    }

    private Bitmap downloadBitmap(String url)
    {
        HttpURLConnection urlConnection = null;
        try
        {
            URL uri = new URL(url);

            urlConnection = (HttpURLConnection) uri.openConnection();
            urlConnection.setRequestProperty("Authorization", RESTMgr.getInstance().getAuth());
            urlConnection.connect();
            int statusCode = urlConnection.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK)
            {
                return null;
            }

            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream != null)
            {
                try
                {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4;
                    return BitmapFactory.decodeStream(inputStream, null, options);
                }
                catch (Exception ex)
                {
                    return null;
                }
            }
        }
        catch (Exception e)
        {
            urlConnection.disconnect();
            Log.w("ImageDownloader", "Error downloading image from " + url);
        }
        finally
        {
            if (urlConnection != null)
            {
                urlConnection.disconnect();
            }
        }
        return null;
    }
}
