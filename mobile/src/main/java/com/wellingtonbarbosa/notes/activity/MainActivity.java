package com.wellingtonbarbosa.notes.activity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wellingtonbarbosa.notes.R;
import com.wellingtonbarbosa.notes.adapter.RecyclerViewAdapter;
import com.wellingtonbarbosa.notes.model.Note;
import com.wellingtonbarbosa.notes.utils.Constants;
import com.wellingtonbarbosa.notes.utils.SharedPreferencesUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RecyclerViewAdapter.ItemSelectedListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String BROADCAST_ACTION_FILTER = "com.wellingtonbarbosa.notes.activity.BROADCAST_ACTION_FILTER";

    private RecyclerViewAdapter mAdapter;
    private List<Note> myDataSet = new ArrayList<>();
    private GoogleApiClient mGoogleApiClient;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (intent != null && intent.hasExtra(Constants.ACTION)) {

                        int action = intent.getIntExtra(Constants.ACTION, 0);

                        if (action == Constants.ACTION_SYNC) {

                        } else {
                            String noteID = intent.getStringExtra(Constants.NOTE_ID);
                            String noteTitle = intent.getStringExtra(Constants.NOTE_TITLE);

                            Note note = createNote(noteID, noteTitle);
                            updateData(note, action);
                        }
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(BROADCAST_ACTION_FILTER));

        //Realiza conexão com o GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        configLayoutComponents();
    }


    private void configLayoutComponents() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new RecyclerViewAdapter();
        mAdapter.setListNote(myDataSet);
        mAdapter.setListener(this);
        recyclerView.setAdapter(mAdapter);

        EditText editText = (EditText) findViewById(R.id.edit_text);

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int action, KeyEvent keyEvent) {

                if (action == EditorInfo.IME_ACTION_SEND) {
                    String text = textView.getText().toString();
                    if (!TextUtils.isEmpty(text)) {
                        Note note = createNote(null, text);
                        notifyMobileDataChanged(note, Constants.ACTION_ADD);
                        updateData(note, Constants.ACTION_ADD);
                        textView.setText("");
                        return true;
                    }
                }

                return false;
            }
        });
    }

    private void updateAdapter() {
        myDataSet.clear();
        myDataSet.addAll(SharedPreferencesUtils.getAllNotes(this));
        mAdapter.setListNote(myDataSet);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        updateAdapter();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();
    }

    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public void onItemSelected(final int position) {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(R.string.alert_title);
        alertDialog.setCancelable(true);
        alertDialog.setNegativeButton(R.string.alert_negative_button, null);
        alertDialog.setPositiveButton(R.string.alert_positive_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (position > -1) {
                    Note note = myDataSet.get(position);
                    notifyMobileDataChanged(note, Constants.ACTION_DELETE);
                    updateData(note, Constants.ACTION_DELETE);
                }
            }
        });
        alertDialog.show();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
    }

    private void updateData(Note note, int action) {
        if (action == Constants.ACTION_ADD) {
            SharedPreferencesUtils.saveNote(note, this);
            showToastMessage(getString(R.string.note_alvo));
        } else if (action == Constants.ACTION_DELETE) {
            SharedPreferencesUtils.removeNote(note.getId(), this);
            showToastMessage(getString(R.string.note_removido));
        }
        updateAdapter();
    }

    /**
     * Notifica o mobile que os dados mudaram via Wearable DataApi Layer
     */
    private void notifyMobileDataChanged(final Note note, final int action) {
        //Cria o PutDataMapRequest com o path "/mobile-data-changed"
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Constants.PATH_MOBILE);

        //Adiciona os dados no DataMap do PutDataMapRequest
        putDataMapReq.getDataMap().putString(Constants.NOTE_ID, note.getId());
        putDataMapReq.getDataMap().putString(Constants.NOTE_TITLE, note.getTitle());
        putDataMapReq.getDataMap().putInt(Constants.ACTION, action);

        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();

        //Envia o PutDataRequest pela Wearable DataApi Layer
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);

        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(@NonNull final DataApi.DataItemResult result) {
                if (result.getStatus().isSuccess()) {
                    Log.d(TAG, "Data item set: " + result.getDataItem().getUri());
                }
            }
        });
    }

    private Note createNote(String id, String title) {
        if (id == null) {
            id = String.valueOf(System.currentTimeMillis());
        }
        return new Note(id, title);
    }

    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
