package com.simpleware.jonathan.loaderexample;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JDavis on 7/25/2016.
 */
public class MusicLoader extends AsyncTaskLoader<List<MusicItem>> {


    private List<MusicItem> mCache;
    private MusicObserver mMusicObserver;

    public MusicLoader(Context context) {
        super(context);
    }

    @Override
    public List<MusicItem> loadInBackground() {
        // Get a content resolver so we can access the media database.
        final ContentResolver contentResolver = getContext().getContentResolver();
        // The four columns we want to retrieve.
        String [] projections = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM_ID};
        // Only return media that is music.
        String selection = MediaStore.Audio.Media.IS_MUSIC + " =1";
        // Sort the music by title.
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        // Peform the actual query of the MediaStore.
        Cursor cr = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,projections, selection, null, sortOrder);
        List<MusicItem> music = new ArrayList<>();
        // If the cursor is not null and not empty, build a list of found music.
        if(cr != null && cr.moveToFirst()) {
            int idIndex = cr.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleIndex = cr.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistIndex = cr.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumnIdIndex = cr.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

            do {
                MusicItem item = new MusicItem();
                item.setId(cr.getLong(idIndex));
                item.setTitle(cr.getString(titleIndex));
                item.setArtist(cr.getString(artistIndex));
                item.setAlbumId(cr.getLong(albumnIdIndex));
                music.add(item);
            }
            while (cr.moveToNext());
            // Always close your cursor reference to free precious resources.
            cr.close();
        }
        return music;
    }

    @Override
    public void deliverResult(List<MusicItem> data) {
        // if we've been reset, we want to discard our old data.
        if(isReset()) {
            // This is where you'd close cursors, sockets, or database handles.
            return;
        }
        // Cache the loaded data encase we are asked to load again.,
        mCache = data;
        // If we are started, immediately deliver our results to the UI thread LoaderCallback#onLoadFinished(data).
        if(isStarted()){
            super.deliverResult(data);
        }
    }

    @Override
    protected void onStopLoading() {
        // Tell the load to cancel the asynchronous load taking place if any.
        cancelLoad();
    }

    @Override
    protected void onStartLoading() {
        // If the cache is not null, we want to deliver it to the UI thread.
        if(mCache != null) {
            deliverResult(mCache);
        }
        // Register an observer that will listen for updates ot the MediaStore.
        if(mMusicObserver == null) {
            mMusicObserver = new MusicObserver(this, new Handler());
            getContext().getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mMusicObserver);
        }

        // If the MusicObserver detected any changes or if the cached music is null, force a load of the music data from the MediaStore.
        if(takeContentChanged() || mCache == null) {
            forceLoad();
        }
    }

    @Override
    protected void onReset() {
        // Stop loading the music...
        onStopLoading();

        // We are being reset so clear our cache.
        if(mCache != null) {
            mCache.clear();
            mCache = null;
        }

        // We are reset so we should unregister the music observer so we don't have duplicates registered if this loader is ever restarted.
        if(mMusicObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(mMusicObserver);
            mMusicObserver = null;
        }
    }

    /**
     * Simple observer that notifies the loader when it has detected a change.
     */
    private static class MusicObserver extends ContentObserver {

        private android.support.v4.content.Loader mLoader;

        public MusicObserver(android.support.v4.content.Loader loader, Handler handler) {
            super(handler);
            mLoader = loader;
        }

        @Override
        public void onChange(boolean selfChange) {
            // A change has been detectec notify the Loader.
            mLoader.onContentChanged();
        }
    }
}
