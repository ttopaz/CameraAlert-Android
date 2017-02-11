package com.topaz.cameraalert.Adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.topaz.cameraalert.Utils.RESTMgr;
import com.topaz.cameraalert.Model.CameraFile;
import com.topaz.cameraalert.R;
import com.topaz.cameraalert.Utils.ImageDownloader;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Tony on 2/11/2017.
 */
public class FileAdapter extends ArrayAdapter<CameraFile>
{
    private final Context context;
    private ArrayList<CameraFile> itemsArrayList;
    private LayoutInflater inflater;
    private RESTMgr restMgr;

    public FileAdapter(Context context, ArrayList<CameraFile> itemsArrayList)
    {
        super(context, R.layout.file_row, itemsArrayList);

        this.context = context;
        this.itemsArrayList = itemsArrayList;

        inflater = LayoutInflater.from(context);
    }

    public void setData(ArrayList<CameraFile> itemsArrayList)
    {
        this.itemsArrayList = itemsArrayList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return this.itemsArrayList.size();
    }

    @Override
    public CameraFile getItem(int position) {
        return this.itemsArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        CameraFile file = itemsArrayList.get(position);

        ViewHolder holder;
        if (convertView == null)
        {
            convertView = inflater.inflate(R.layout.file_row, parent, false);

            holder = new ViewHolder();

            holder.dateView = (TextView) convertView.findViewById(R.id.file_date);
            holder.timeView = (TextView) convertView.findViewById(R.id.file_time_range);
            //                holder.fileNameView = (TextView) convertView.findViewById(R.id.file_name);
            holder.fileSizeView = (TextView) convertView.findViewById(R.id.file_size);
            holder.imageView = (ImageView) convertView.findViewById(R.id.thumbImage);
            //                holder.viewSwitcher = (ViewSwitcher)convertView.findViewById(R.id.list_switcher);

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        android.text.format.Formatter formatter = new android.text.format.Formatter();

        String dateString = android.text.format.DateFormat.format("MM/dd/yyyy", file.Date).toString();
        String todayString = android.text.format.DateFormat.format("MM/dd/yyyy", new Date()).toString();
        if (dateString.compareTo(todayString) == 0)
            dateString = "Today";

        holder.dateView.setText(dateString);
        String timeText = android.text.format.DateFormat.format("hh:mm:ss a - ", file.CreateDate).toString()
                + android.text.format.DateFormat.format("hh:mm:ss a", file.Date);
        holder.timeView.setText(timeText);
        //            holder.fileNameView.setText(file.File);
        holder.fileSizeView.setText(formatter.formatFileSize(this.context, file.Size));
        holder.dateView.setTypeface(null, file.Changed ? Typeface.BOLD : Typeface.NORMAL);
        holder.timeView.setTypeface(null, file.Changed ? Typeface.BOLD : Typeface.NORMAL);
        //            holder.fileNameView.setTypeface(null, file.Changed ? Typeface.BOLD : Typeface.NORMAL);

        if (holder.imageView != null) {
            //      Drawable placeholder = holder.imageView.getContext().getResources().getDrawable(R.drawable.placeholder);
            //    holder.imageView.setImageDrawable(placeholder);

            String imageUrl = RESTMgr.getInstance().getCameraImageUrl(file.CameraId, file.File.replace(".mp4", ".jpg"));
            new ImageDownloader(holder.imageView).execute(imageUrl, file.CameraId);
        }
        return convertView;
    }

    class ViewHolder {
        TextView dateView;
        TextView timeView;
        TextView fileNameView;
        TextView fileSizeView;
        ImageView imageView;
        //            ViewSwitcher viewSwitcher;
    }
}
