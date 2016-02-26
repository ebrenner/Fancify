package com.example.ericbrenner.fancify;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.example.ericbrenner.fancify.interfaces.OnEditSignPostListener;

/**
 * Created by ericbrenner on 2/4/16.
 */
public class EditTask extends AsyncTask<EditTaskParams, Void, Bitmap> {

    private static float m = 127.5f;
    private static float prodTot = 50f;

    OnEditSignPostListener mListener;
    Bitmap mBitmap;
    boolean mShouldSave;

    public EditTask(Bitmap bitmap, OnEditSignPostListener listener, boolean shouldSave) {
        mListener = listener;
        mBitmap = bitmap;
        mShouldSave = shouldSave;
    }

    @Override
    protected Bitmap doInBackground(EditTaskParams... params) {
        EditTaskParams editTaskParams = params[0];

        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        int[] pix = new int[width * height];
        mBitmap.getPixels(pix, 0, width, 0, 0, width, height);

        int r, g, b, index, R, G, B, RY, BY, RYY, GYY, BYY, Y;
        float a, rd, gd, bd, md,  ad, wf, hf, nhf, struct;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isCancelled()) {
                    pix = null;
                    break;
                }
                index = y * width + x;
                r = (pix[index] >> 16) & 0xff;
                g = (pix[index] >> 8) & 0xff;
                b = pix[index] & 0xff;
                a = (r + g + b) / 3.0f;
                rd = r - a;
                gd = g - a;
                bd = b - a;
                md = a - m;
                wf = editTaskParams.warmth - 1;
                hf = editTaskParams.hue - 1;
                if (md < 0) {
                    struct = editTaskParams.structure;
                } else {
                    struct = 1;
                }
                R = (int) (((m + md * editTaskParams.contrast * struct) * editTaskParams.exposure + editTaskParams.saturation * rd) + 0.6f * wf * prodTot + 0.6 * hf * prodTot);
                R = assignToExtremeIfOutOfBounds(R);
                G = (int) (((m + md * editTaskParams.contrast * struct) * editTaskParams.exposure + editTaskParams.saturation * gd) + 0.4f * wf * prodTot - hf * prodTot);
                G = assignToExtremeIfOutOfBounds(G);
                B = (int) (((m + md * editTaskParams.contrast * struct) * editTaskParams.exposure + editTaskParams.saturation * bd) - wf * prodTot + 0.4 * hf * prodTot);
                B = assignToExtremeIfOutOfBounds(B);

                double angle = (3.14159d * (double) editTaskParams.colorRotation) / 180.0d;
                int S = (int) (256.0d * Math.sin(angle));
                int C = (int) (256.0d * Math.cos(angle));

                RY = (70 * R - 59 * G - 11 * B) / 100;
                BY = (-30 * R - 59 * G + 89 * B) / 100;
                Y = (30 * R + 59 * G + 11 * B) / 100;
                RYY = (S * BY + C * RY) / 256;
                BYY = (C * BY - S * RY) / 256;
                GYY = (-51 * RYY - 19 * BYY) / 100;
                R = Y + RYY;
                R = (R < 0) ? 0 : ((R > 255) ? 255 : R);
                G = Y + GYY;
                G = (G < 0) ? 0 : ((G > 255) ? 255 : G);
                B = Y + BYY;
                B = (B < 0) ? 0 : ((B > 255) ? 255 : B);
                pix[index] = 0xff000000 | (R << 16) | (G << 8) | B;
            }
        }

        //Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mBitmap.setPixels(pix, 0, width, 0, 0, width, height);
        //bm.setPixels(pix, 0, width, 0, 0, width, height);
        pix = null;
        return mBitmap;
    }

    @Override
    protected void onPreExecute() {
        mListener.onEditStarted();
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (mShouldSave) {
            mListener.onEditFinishedForSave(bitmap);
        } else {
            mListener.onEditFinishedForApp(bitmap);
        }
    }

    @Override
    protected void onCancelled(Bitmap bitmap) {
        mListener.onEditCanceled();
    }

    private int assignToExtremeIfOutOfBounds(int i) {
        if (i > 255) {
            i = 255;
        } else if (i < 0) {
            i = 0;
        }
        return i;
    }
}
