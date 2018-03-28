package com.topaz.cameraalert.Adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.topaz.cameraalert.Utils.ImageManager;
import com.topaz.cameraalert.Utils.RESTMgr;
import com.topaz.cameraalert.Model.CameraFile;
import com.topaz.cameraalert.R;
import com.topaz.cameraalert.Utils.ImageManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Tony on 2/11/2017.
 */
public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder>
{
    private final Context context;
    private ArrayList<CameraFile> itemsArrayList;
    private LayoutInflater inflater;
    private RESTMgr restMgr;

    public FileAdapter(Context context, ArrayList<CameraFile> itemsArrayList)
    {
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
    public FileViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.file_row, null);

        FileViewHolder holder = new FileViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(FileViewHolder holder, int i) {
        CameraFile file = this.itemsArrayList.get(i);

        String dateString = android.text.format.DateFormat.format("MM/dd/yyyy", file.Date).toString();
        String todayString = android.text.format.DateFormat.format("MM/dd/yyyy", new Date()).toString();
        if (dateString.compareTo(todayString) == 0)
            dateString = "Today";

        holder.dateView.setText(dateString);
        String timeText = android.text.format.DateFormat.format("hh:mm:ss a - ", file.CreateDate).toString()
                + android.text.format.DateFormat.format("hh:mm:ss a", file.Date);
        holder.timeView.setText(timeText);
        holder.fileSizeView.setText(Formatter.formatFileSize(this.context, file.Size));
        holder.dateView.setTypeface(null, file.Changed ? Typeface.BOLD : Typeface.NORMAL);
        holder.timeView.setTypeface(null, file.Changed ? Typeface.BOLD : Typeface.NORMAL);

        if (holder.imageView != null)
        {
            String imageUrl = RESTMgr.getInstance().getCameraImageUrl(file.CameraId, file.File.replace(".mp4", ".jpg"));
            if (imageUrl != null)
            {
                ImageManager.getInstance().getImage(imageUrl, holder.imageView);
            }
        }
    }

    @Override
    public int getItemCount() {
        return this.itemsArrayList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public CameraFile getItem(int position) {
        return this.itemsArrayList.get(position);
    }

    public void remove(CameraFile file)
    {
        this.itemsArrayList.remove(file);
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        TextView dateView;
        TextView timeView;
        TextView fileSizeView;
        ImageView imageView;

        public FileViewHolder(View view) {
            super(view);

            this.dateView = (TextView) view.findViewById(R.id.file_date);
            this.timeView = (TextView) view.findViewById(R.id.file_time_range);
            this.fileSizeView = (TextView) view.findViewById(R.id.file_size);
            this.imageView = (ImageView) view.findViewById(R.id.thumbImage);
        }
    }
}
