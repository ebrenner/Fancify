package com.example.ericbrenner.fancify;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.example.ericbrenner.fancify.interfaces.OnEditSignPostListener;

import java.io.InputStream;

/**
 * Created by ericbrenner on 2/4/16.
 */
public class EditTask extends AsyncTask<EditTaskParams, Void, Bitmap> {

    private static float m = 127.5f;
    private static float prodTot = 50f;

    OnEditSignPostListener mListener;
    Bitmap mBitmap;

    public EditTask(Bitmap bitmap, OnEditSignPostListener listener) {
        mListener = listener;
        mBitmap = bitmap;
    }

    @Override
    protected Bitmap doInBackground(EditTaskParams... params) {
        EditTaskParams editTaskParams = params[0];

        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        int[] pix = new int[width * height];
        mBitmap.getPixels(pix, 0, width, 0, 0, width, height);

        int r, g, b, index, R, G, B;
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
                pix[index] = 0xff000000 | (R << 16) | (G << 8) | B;
            }
        }

        //contrast
//        imageAve = imageTot / (width * height);
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                if (isCancelled()) {
//                    pix = null;
//                    break;
//                }
//                index = y * width + x;
//                r = (pix[index] >> 16) & 0xff;
//                g = (pix[index] >> 8) & 0xff;
//                b = pix[index] & 0xff;
//                a = (r + g + b) / 3.0f;
//                rd = r - a;
//                gd = g - a;
//                bd = b - a;
//                ad = a - imageAve;
//                R = (int) (imageAve + ad * editTaskParams.contrast + rd);
//                G = (int) (imageAve + ad * editTaskParams.contrast + gd);
//                B = (int) (imageAve + ad * editTaskParams.contrast + bd);
//                R = assignToExtremeIfOutOfBounds(R);
//                G = assignToExtremeIfOutOfBounds(G);
//                B = assignToExtremeIfOutOfBounds(B);
//                pix[index] = 0xff000000 | (R << 16) | (G << 8) | B;
//            }
//        }

        Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bm.setPixels(pix, 0, width, 0, 0, width, height);
        pix = null;
        return bm;
    }

    @Override
    protected void onPreExecute() {
        mListener.onEditStarted();
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        mListener.onEditFinished(bitmap);
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
