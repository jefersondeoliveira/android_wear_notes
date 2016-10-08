package com.wellingtonbarbosa.notes.activity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WearableRecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.wellingtonbarbosa.notes.R;
import com.wellingtonbarbosa.notes.adapter.RecyclerViewAdapter;
import com.wellingtonbarbosa.notes.model.Note;
import com.wellingtonbarbosa.notes.utils.ConfirmationUtils;
import com.wellingtonbarbosa.notes.utils.Constants;
import com.wellingtonbarbosa.notes.utils.SharedPreferencesUtils;

import java.util.ArrayList;
import java.util.List;

public class WearMainActivity extends WearableActivity implements RecyclerViewAdapter.ItemSelectedListener,
        DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = WearMainActivity.class.getSimpleName();

    private static final int REQUEST_CODE = 1001;

    private RecyclerViewAdapter mAdapter;
    private List<Note> myDataSet = new ArrayList<>();

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_main);

        //Realiza conexão com o GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        configLayoutComponents();
    }

    private void configLayoutComponents() {
        WearableRecyclerView recyclerView = (WearableRecyclerView) findViewById(R.id.wearable_recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);

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
                        SharedPreferencesUtils.saveNote(note, textView.getContext());
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
    protected void onDestroy() {
        super.onDestroy();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onItemSelected(int position) {
        Intent intent = new Intent(getApplicationContext(), DeleteActivity.class);
        intent.putExtra(Constants.ITEM_POSITION, position);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && requestCode == REQUEST_CODE && resultCode == RESULT_OK) {

            if (data.hasExtra(Constants.ITEM_POSITION)) {
                int position = data.getIntExtra(Constants.ITEM_POSITION, -1);

                if (position > -1) {
                    Note note = myDataSet.get(position);
                    notifyMobileDataChanged(note, Constants.ACTION_DELETE);
                    updateData(note, Constants.ACTION_DELETE);
                }
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Now you can use the Data Layer API
        Log.d(TAG, "Connected to Google Api Service");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Todo - Mostrar uma mensagem para o usuário
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Todo - Mostrar uma mensagem para o usuário
        Log.d(TAG, "onConnectionFailed");
    }


    /**
     * Método responsável por receber os dado e os eventos da Data Layer API.
     *
     * @param dataEvents DataEventBuffer
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged " + dataEvents);

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();

                //Valida se o path do DataItem é igual
                // ao path esperado "/mobile-data-changed"
                if (item.getUri().getPath()
                        .compareTo(Constants.PATH_MOBILE) == 0) {

                    DataMap dataMap = DataMapItem.fromDataItem(item)
                            .getDataMap();

                    prepareUpdate(dataMap.getString(Constants.NOTE_ID),
                            dataMap.getString(Constants.NOTE_TITLE),
                            dataMap.getInt(Constants.ACTION));
                }
            }
        }
    }

    private void prepareUpdate(String id, String title, int action) {
        if (!(TextUtils.isEmpty(id) && TextUtils.isEmpty(title))) {
            Note note = createNote(id, title);
            updateData(note, action);
        }
    }

    private void updateData(Note note, int action) {
        if (action == Constants.ACTION_ADD) {
            SharedPreferencesUtils.saveNote(note, this);
            ConfirmationUtils.showMessage(getString(R.string.note_salvo), this);
        } else if (action == Constants.ACTION_DELETE) {
            SharedPreferencesUtils.removeNote(note.getId(), this);
            ConfirmationUtils.showMessage(getString(R.string.note_removido), this);
        }
        updateAdapter();
    }

    /**
     * Notifica a Data Layer API que os dados foram modificados.
     */
    private void notifyMobileDataChanged(final Note note, final int action) {
        //Cria o PutDataMapRequest com o path "/wear-data-changed""
        PutDataMapRequest putDataMapReq
                = PutDataMapRequest.create(Constants.PATH_WEAR);

        //Adiciona os dados no DataMap do PutDataMapRequest
        putDataMapReq.getDataMap()
                .putString(Constants.NOTE_ID, note.getId());
        putDataMapReq.getDataMap()
                .putString(Constants.NOTE_TITLE, note.getTitle());
        putDataMapReq.getDataMap()
                .putInt(Constants.ACTION, action);

        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();

        //Envia o PutDataRequest pela Wearable DataApi Layer
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);

        //Callback com o resultado do envio dos dados pela Data Layer API
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(@NonNull final DataApi.DataItemResult result) {
                if (result.getStatus().isSuccess()) {
                    Log.d(TAG, "Data item sent: " + result.getDataItem().getUri());
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
}
