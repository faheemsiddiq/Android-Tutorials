package com.simpleware.jonathan.loaderexample;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.os.Build;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.CursorAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

public class LoaderActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<MusicItem>> {

    // TO use a recyclerview add the following dependency to the build.gradle file 'com.android.support:recyclerview-v7:23.1.+'.
    private RecyclerView mRecyclerView;
    private MusicAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loader);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerVw);
        // This layout manager will lay the content out exactly like a ListView would.
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new MusicAdapter(new ArrayList<MusicItem>(), this);
        mRecyclerView.setAdapter(mAdapter);

        // Use the Support LoaderManager from the support lib. to be back compatible.
        LoaderManager loaderManager = getSupportLoaderManager();
        // Pass in the identifier to identify the particular loader to call, this is only useful if you are loading several loaders at once.
        // null for the second args because their are no special arguments.
        // Reference to this activity.
        loaderManager.initLoader(1, null ,this);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        // Instantiate the MusicLoader.
        return new MusicLoader(this);
    }


    @Override
    public void onLoadFinished(Loader<List<MusicItem>> loader, List<MusicItem> data) {
        // Once the data is loaded add it to the Music Adapter.
        mAdapter.addMusic(data);
    }


    @Override
    public void onLoaderReset(Loader loader) {
        // Clear the old data.
        mAdapter.clearMusic();
    }
}
