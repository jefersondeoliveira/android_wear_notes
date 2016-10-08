package com.wellingtonbarbosa.notes.utils;

import android.content.Context;
import android.content.Intent;
import android.support.wearable.activity.ConfirmationActivity;

public class ConfirmationUtils {

    public static void showMessage(String message, Context context) {
        Intent intent = new Intent(context, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, message);
        context.startActivity(intent);
    }
}
