package com.example.ericbrenner.fancify.interfaces;

import android.graphics.Bitmap;

/**
 * Created by ericbrenner on 2/4/16.
 */
public interface OnEditSignPostListener {

    public void onEditStarted();
    public void onEditCanceled();
    public void onEditFinishedForApp(Bitmap bitmap);
    public void onEditFinishedForSave(Bitmap bitmap);
}
