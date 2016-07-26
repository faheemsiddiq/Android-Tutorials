package com.simpleware.jonathan.loaderexample;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by JDavis on 7/25/2016.
 */
public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {

    private List<MusicItem> mMusic;
    private BitmapDrawable mDrawable;

    public MusicAdapter(List<MusicItem> music, Context context) {
        mMusic = new ArrayList<>();
        mMusic.addAll(music);
        mDrawable = (BitmapDrawable)  context.getResources().getDrawable(R.drawable.ic_music_note_black_48dp);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View layout = inflater.inflate(R.layout.list_item, parent, false);
        ViewHolder holder = new ViewHolder(layout);
        return holder;
    }

    @Override
    public int getItemCount() {
        return mMusic.size();
    }

    /**
     * Adds a list of music to this adapter.
     * @param music
     */
    public void addMusic(List<MusicItem> music) {
        // Add the new data set to the internal list. Clear the old music first.
        mMusic.clear();
        mMusic.addAll(music);
        notifyDataSetChanged();
    }

    /**
     * Clears the music represented by this adapter.
     */
    public void clearMusic() {
        mMusic.clear();
        notifyDataSetChanged();
    }

    /**
     * Custom viewholder for represent a RecyclerView item.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView artist;

        public ViewHolder(View v) {
            super(v);
            icon = (ImageView) v.findViewById(R.id.icon);
            title = (TextView) v.findViewById(R.id.title);
            artist = (TextView) v.findViewById(R.id.subtitle);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MusicItem item = mMusic.get(position);
        holder.title.setText(item.getTitle());
        holder.artist.setText(item.getArtist());

        // Execute a new task for loading albumar artwork based on the Album ID.
        loadArtwork(holder.icon, item.getAlbumId());
    }

    /**
     * Helper method for loading the album artwork.
     *
     * NOTE THIS METHOD IS DEEPLY FLAWED AND SHOULD PERFORM CACHE MATCHING FOR PREVIOUSLY LOADED ARTWORK.
     *
     * @param imageView
     * @param albumId
     */
    private void loadArtwork(ImageView imageView, long albumId) {
        // Try to retrieve the LoadArworkTask from the iamgeView.
        LoadArtworkTask task = getArtworkLoadingTask(imageView);
        // Cancel the LoadArtworkTask task if it is not loading album art that corresponds to this album id..
        if(cancelledArtworkTask(task, albumId)) {
            // Load a new LoadArtworkTask to try and load the album art.
            task = new LoadArtworkTask(imageView);
            AsyncDrawable asyncDrawable = new AsyncDrawable(imageView.getContext().getResources(), mDrawable.getBitmap(), task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(albumId);
        }
    }

    /**
     * Helper method for cancelling an LoadArtworkTask if it is not loading the album art for the specified album id.
     * @param task
     * @param albumId
     * @return
     */
    private boolean cancelledArtworkTask(LoadArtworkTask task, long albumId) {
        if(task == null) {
            return true;
        }
        // if the albumId equals the defualt value or anything other than the specified albumId cancel the task.
        if(task.albumId == -1 || task.albumId != albumId){
            task.cancel(true);
            return true;
        }
        return false;
    }

    /**
     * Helper method for pulling a LoaderArtworkTask from an ImageView.
     *
     * @param imgView
     * @return
     */
    private static LoadArtworkTask getArtworkLoadingTask(ImageView imgView) {
        Drawable drawable = imgView.getDrawable();
        // If the drawable is an instance of AsyncDrawable get the LoadArtworkTask stored inside of it.
        // Even if the drawable is a AsyncDrawable it could still return null because the task completed and was garbage collected.
        if(drawable instanceof AsyncDrawable) {
            return ((AsyncDrawable) drawable).getLoadArtworkTask();
        }
        return null;
    }

    /**
     * Custom Asynctask for loading album art.
     */
    private static class LoadArtworkTask extends AsyncTask<Long, Void, Bitmap> {

        private Context mContext;
        // Weak reference to hold the ImageView this task is bound to.
        WeakReference<ImageView> imageViewWeakReference;

        // The default URI of where album art is stored on the device.
        // This constant was lifted from the MediaStore source code and is liable to change/break in future api.
        Uri albumArtworkURI = Uri.parse("content://media/external/audio/albumart");
        long albumId = -1;

        public LoadArtworkTask(ImageView img) {
            mContext = img.getContext().getApplicationContext();
            // Store the ImageView inside of a weak reference so it can still be garbage collected.
            imageViewWeakReference = new WeakReference<ImageView>(img);
        }

        @Override
        protected Bitmap doInBackground(Long... params) {
            // Get the album id from the varags.
            albumId = params[0];
            // Append the album id to the end of the albumrt URI and it should point to the correct album art
            Uri artworkUri = ContentUris.withAppendedId(albumArtworkURI, albumId);
            Bitmap bmp;
            try {
                ContentResolver cr = mContext.getContentResolver();
                // If there is no album art for the album Id, a file not found exception is thrown.
                bmp = MediaStore.Images.Media.getBitmap(cr, artworkUri);
                // scale bitmap
                bmp = Bitmap.createScaledBitmap(bmp, 48, 48, true);
            }
            catch (FileNotFoundException e) {
                bmp = null;
            }
            catch (IOException e) {
                bmp = null;
            }
            return bmp;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            // Return if the bitmap is null or the .
            if(isCancelled() || bitmap == null){
                return;
            }
            // Check to make sure the imageview was not garbage collected.
            if(imageViewWeakReference != null && imageViewWeakReference.get() != null) {
                ImageView imgVw = imageViewWeakReference.get();
                Drawable img = imgVw.getDrawable();
                // Since views can be recycled make sure this LoadArtWorkTask belongs to this imageview and is not an old instance.
                if(img instanceof AsyncDrawable) {
                    // Check to see if this is the same task
                    if(this == ((AsyncDrawable) img).getLoadArtworkTask()){
                        imgVw.setImageBitmap(bitmap);
                    }
                }
            }
        }
    }

    /**
     * Custom drawable that holds a LoadArtworkTask/
     */
    private static class AsyncDrawable extends BitmapDrawable {
        WeakReference<LoadArtworkTask> loadArtworkTaskWeakReference;

        public AsyncDrawable(Resources resources, Bitmap bitmap, LoadArtworkTask task) {
            super(resources, bitmap);
            // Store the LoadArtwork task inside of a weak reference so it can still be garbage collected.
            loadArtworkTaskWeakReference = new WeakReference<LoadArtworkTask>(task);
        }

        public LoadArtworkTask getLoadArtworkTask() {
            return loadArtworkTaskWeakReference.get();
        }
    }

}
