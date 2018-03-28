package com.topaz.cameraalert.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.topaz.cameraalert.R;

import org.apache.http.HttpStatus;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class ImageManager
{
    private static ImageManager instance;
    private LruCache<String, Bitmap> memoryCache;
    private ArrayList<WeakReference<ImageDownloader>> downloaders;

    public static ImageManager getInstance()
    {
        if (instance == null)
            instance = new ImageManager();
        return instance;
    }

    private ImageManager()
    {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };

        downloaders = new ArrayList<WeakReference<ImageDownloader>>();
    }

    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            memoryCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemCache(String key) {
        return memoryCache.get(key);
    }

    public void cancelDownloads()
    {
        synchronized (downloaders)
        {
            for (WeakReference<ImageDownloader> taskref: downloaders)
            {
                if (taskref != null)
                {
                    ImageDownloader task = taskref.get();
                    if (task != null)
                    {
                        if (!task.isCancelled())
                            task.cancel(true);
                    }
                }
            }
            downloaders.clear();
        }
    }

    public ImageDownloader getImage(String url, ImageView imageView)
    {
        final String imageKey = String.valueOf(url);

        final Bitmap bitmap = getBitmapFromMemCache(imageKey);
        if (bitmap != null)
        {
            imageView.setImageBitmap(bitmap);
            return null;
        }
        else
        {
            imageView.setImageResource(R.drawable.placeholder);
            ImageDownloader task = new ImageDownloader(imageView);
            synchronized (downloaders)
            {
                downloaders.add(new WeakReference<ImageDownloader>(task));
            }
            task.execute(url);

            return task;
        }
    }

    /**
     * Created by Tony on 2/11/2017.
     */
    public class ImageDownloader extends AsyncTask<String, Void, Bitmap>
    {
        private final WeakReference<ImageView> imageViewReference;

        public ImageDownloader(ImageView imageView)
        {
            this.imageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params)
        {
            if (isCancelled())
            {
                return null;
            }

            Bitmap bitmap = downloadBitmap(params[0]);
            if (bitmap != null)
                addBitmapToMemoryCache(String.valueOf(params[0]), bitmap);
            return bitmap;
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
                        imageView.setImageResource(R.drawable.placeholder);
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
}