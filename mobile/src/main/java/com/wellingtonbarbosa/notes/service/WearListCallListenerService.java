package com.wellingtonbarbosa.notes.service;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.wellingtonbarbosa.notes.activity.MainActivity;
import com.wellingtonbarbosa.notes.utils.Constants;

public class WearListCallListenerService extends WearableListenerService {

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo(Constants.PATH_WEAR) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                    update(dataMap.getString(Constants.NOTE_ID),
                            dataMap.getString(Constants.NOTE_TITLE), dataMap.getInt(Constants.ACTION));
                }
            }
        }
    }

    private void update(String id, String title, int action) {

        if (!(TextUtils.isEmpty(id) && TextUtils.isEmpty(title))) {
            Intent intent = new Intent(MainActivity.BROADCAST_ACTION_FILTER);
            intent.putExtra(Constants.NOTE_ID, id);
            intent.putExtra(Constants.NOTE_TITLE, title);
            intent.putExtra(Constants.ACTION, action);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }
}