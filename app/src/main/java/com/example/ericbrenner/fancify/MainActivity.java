package com.example.ericbrenner.fancify;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.ericbrenner.fancify.interfaces.OnEditSignPostListener;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnEditSignPostListener {

    public static final String IMAGE_NAME = "FancifyImage";
    public static final String IMAGE_DESC = "Image created with the Fancify app";

    public static final String KEY_IS_FIRST_RUN = "KEY_IS_FIRST_RUN";

    private static final int REQUEST_SELECT_PHOTO = 1;
    private static final int PERMISSIONS_REQUEST_WRITE_EXT_STORAGE = 2;
    private static final int MAX_PIXELS_APP = 800*1000;
    private static final int MAX_PIXELS_SAVE = 1400*1800;

    private ImageView mImageView;
    private Button mButton;
    private View mLoadingSpinner;
    private ViewPager mPager;
    private AdjustmentsPagerAdapter mAdjustmentsPagerAdapter;

    private boolean mImageSelected = false;

    private EditTask mEditTask;

    Uri mUri;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_PHOTO:
                    mUri = data.getData();
                    setImage(false, false);
                    displayDirectionsDialogIfFirstRun();
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mImageSelected) {
            toggleImageSelected(false);
        } else {
            super.onBackPressed();
        }
    }

    private void displayDirectionsDialogIfFirstRun() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean isFirst = preferences.getBoolean(KEY_IS_FIRST_RUN, true);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_IS_FIRST_RUN, false);
        editor.commit();
        if (isFirst) {
            displayDialog(getString(R.string.dialog_directions_title),
                    getString(R.string.dialog_directions_message), null);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = (ImageView)findViewById(R.id.image);
        mButton = (Button)findViewById(R.id.button);
        mButton.setOnClickListener(this);
        mLoadingSpinner = findViewById(R.id.loading_spinner);
    }

    private void setUIEnabled(boolean enabled, boolean makePagerInvisible) {
        mButton.setEnabled(enabled);
        if (enabled) {
            mLoadingSpinner.setVisibility(View.GONE);
            mPager.setVisibility(View.VISIBLE);
        } else {
            mLoadingSpinner.setVisibility(View.VISIBLE);
            if (makePagerInvisible) {
                mPager.setVisibility(View.GONE);
            }
        }
    }

    private void displayDialog(String title, String message, Dialog.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setTitle(title).setPositiveButton(getString(R.string.dialog_ok), listener);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onClick(View v) {
        if (mImageSelected) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                setImage(true, true);
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_CONTACTS)) {
                    displayDialog(getString(R.string.dialog_title_perm_required),
                            getString(R.string.dialog_message_perm_required), null);
                }
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_WRITE_EXT_STORAGE);
            }
        } else {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, REQUEST_SELECT_PHOTO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXT_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setImage(true, true);
                } else {
                    displayDialog(getString(R.string.dialog_title_not_saved),
                            getString(R.string.dialog_message_not_saved), null);
                }
            }
        }
    }

    @Override
    public void onEditStarted() {
    }

    @Override
    public void onEditCanceled() {
        setUIEnabled(true, false);
    }

    @Override
    public void onEditFinishedForApp(Bitmap bitmap) {
        setUIEnabled(true, false);
        mImageView.setImageBitmap(bitmap);
    }

    @Override
    public void onEditFinishedForSave(Bitmap bitmap) {
        setUIEnabled(true, false);
        Utilities.insertImage(getContentResolver(), bitmap, IMAGE_NAME, IMAGE_DESC);
        displayDialog(getString(R.string.dialog_title_saved), getString(R.string.dialog_message_saved), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                toggleImageSelected(false);
            }
        });
    }

    private void toggleImageSelected(boolean b) {
        mImageSelected = b;
        if (mImageSelected) {
            mButton.setText(getString(R.string.button_text_save));
        } else {
            mImageView.setImageBitmap(null);
            mButton.setText(getString(R.string.button_text_choose));
            mPager.setVisibility(View.GONE);
        }
    }

    private int getImageOrientation() {
        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
        Cursor cur = getContentResolver().query(mUri, orientationColumn, null, null, null);
        int orientation = -1;
        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
        }
        return orientation;
    }

    private int calculateImageInSampleSize(boolean shouldSave) {
        InputStream imageStream = null;
        try {
            imageStream = getContentResolver().openInputStream(mUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inSampleSize = 1;
        final Bitmap testImage = BitmapFactory.decodeStream(imageStream, null, options);
        try {
            imageStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (shouldSave) {
            return Utilities.calculateInSampleSize(options, MAX_PIXELS_SAVE);
        } else {
            return Utilities.calculateInSampleSize(options, MAX_PIXELS_APP);
        }
    }

    private void setImage(boolean applyAdjustments, boolean shouldSave) {
        try {
            int orientation = getImageOrientation();
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = calculateImageInSampleSize(shouldSave);
            InputStream imageStream = getContentResolver().openInputStream(mUri);
            options.inJustDecodeBounds = false;
            options.inMutable = true;
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream, null, options);
            selectedImage = Bitmap.createBitmap(selectedImage, 0, 0, selectedImage.getWidth(), selectedImage.getHeight(), matrix, true);
            if (applyAdjustments) {
                if (mEditTask != null) {
                    mEditTask.cancel(true);
                }
                setUIEnabled(false, shouldSave);
                EditTaskParams params = mAdjustmentsPagerAdapter.getEditTaskParams();
                mEditTask = new EditTask(params, MainActivity.this, shouldSave);
                mEditTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, selectedImage);
            } else {
                mImageView.setImageBitmap(selectedImage);
                if (!mImageSelected) {
                    toggleImageSelected(true);
                    setUpAdjustmentsPager();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setUpAdjustmentsPager() {
        if (mPager == null) {
            mPager = (ViewPager) findViewById(R.id.pager);
        }
        mAdjustmentsPagerAdapter = new AdjustmentsPagerAdapter();
        mPager.setAdapter(mAdjustmentsPagerAdapter);
        mPager.setVisibility(View.VISIBLE);
    }

    private class AdjustmentsPagerAdapter extends PagerAdapter {

        private final float defaultProgress = 5;

        private ArrayList<View> views = new ArrayList<View>();
        LayoutInflater inflater = getLayoutInflater();
        float[] mParmas;

        public AdjustmentsPagerAdapter() {
            mParmas = new float[7];
            addAdjustmentItem(0, getString(R.string.exposure), false);
            addAdjustmentItem(1, getString(R.string.saturation), false);
            addAdjustmentItem(2, getString(R.string.contrast), false);
            addAdjustmentItem(3, getString(R.string.warmth), false);
            addAdjustmentItem(4, getString(R.string.hue), false);
            addAdjustmentItem(5, getString(R.string.structure), false);
            addAdjustmentItem(6, getString(R.string.color_rotation), true);
        }

        @Override
        public int getItemPosition (Object object)
        {
            int index = views.indexOf(object);
            if (index == -1)
                return POSITION_NONE;
            else
                return index;
        }

        @Override
        public Object instantiateItem (ViewGroup container, int position)
        {
            View v = views.get(position);
            container.addView(v);
            return v;
        }

        @Override
        public void destroyItem (ViewGroup container, int position, Object object)
        {
            container.removeView(views.get(position));
        }

        @Override
        public int getCount ()
        {
            return views.size();
        }

        @Override
        public boolean isViewFromObject (View view, Object object)
        {
            return view == object;
        }

        public int addView (View v)
        {
            return addView(v, views.size());
        }

        public int addView (View v, int position)
        {
            views.add(position, v);
            return position;
        }

        public View getView (int position)
        {
            return views.get (position);
        }

        private void setSeekBarListener(SeekBar seekBar, final int i) {
            mParmas[i] = seekBar.getProgress();
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mParmas[i] = progress;
                    setImage(true, false);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }

        private void addAdjustmentItem(int i, String title, boolean setProgressToZero) {
            RelativeLayout v = (RelativeLayout) inflater.inflate(R.layout.layout_adjustment, null);
            ((TextView)v.findViewById(R.id.title)).setText(title);
            SeekBar seekBar = (SeekBar)v.findViewById(R.id.slider);
            if (setProgressToZero) {
                seekBar.setProgress(0);
            }
            setSeekBarListener(seekBar, i);
            addView(v);
        }

        public EditTaskParams getEditTaskParams() {
            EditTaskParams params = new EditTaskParams();
            params.exposure = mParmas[0]/defaultProgress;
            params.saturation = mParmas[1]/defaultProgress;
            params.contrast = mParmas[2]/defaultProgress;
            params.warmth = mParmas[3]/defaultProgress;
            params.hue = mParmas[4]/defaultProgress;
            params.structure = mParmas[5]/defaultProgress;
            params.colorRotation = mParmas[6]/10f*360;
            return params;
        }

    }
}
