package com.example.ericbrenner.fancify;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
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

    private static final int REQUEST_SELECT_PHOTO = 1;
    private static final int PERMISSIONS_REQUEST_WRITE_EXT_STORAGE = 2;
    private static final int IN_SAMPLE_SIZE_APP = 6;
    private static final int IN_SAMPLE_SIZE_SAVE = 3;

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
                    break;
            }
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

    private void setImage(boolean applyAdjustments, boolean shouldSave) {
        try {
            String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
            Cursor cur = getContentResolver().query(mUri, orientationColumn, null, null, null);
            int orientation = -1;
            if (cur != null && cur.moveToFirst()) {
                orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
            }
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
            final InputStream imageStream = getContentResolver().openInputStream(mUri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            if (shouldSave) {
                options.inSampleSize = IN_SAMPLE_SIZE_SAVE;
            } else {
                options.inSampleSize = IN_SAMPLE_SIZE_APP;
            }
            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream, null, options);
            final Bitmap rotImage = Bitmap.createBitmap(selectedImage, 0, 0, selectedImage.getWidth(), selectedImage.getHeight(), matrix, true);
            if (applyAdjustments) {
                if (mEditTask != null) {
                    mEditTask.cancel(true);
                }
                setUIEnabled(false, shouldSave);
                mEditTask = new EditTask(rotImage, MainActivity.this, shouldSave);
                EditTaskParams params = mAdjustmentsPagerAdapter.getEditTaskParams();
                mEditTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
            } else {
                mImageView.setImageBitmap(rotImage);
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

        private final int defaultProgress = 5;

        private ArrayList<View> views = new ArrayList<View>();
        LayoutInflater inflater = getLayoutInflater();
        float[] mParmas = new float[6];

        public AdjustmentsPagerAdapter() {
            addAdjustmentItem(0, getString(R.string.exposure));
            addAdjustmentItem(1, getString(R.string.saturation));
            addAdjustmentItem(2, getString(R.string.contrast));
            addAdjustmentItem(3, getString(R.string.warmth));
            addAdjustmentItem(4, getString(R.string.hue));
            addAdjustmentItem(5, getString(R.string.structure));
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

        private float convertProgressToMult(int progress) {
            return progress/((float)defaultProgress);
        }

        private void setSeekBarListener(SeekBar seekBar, final int i) {
            mParmas[i] = convertProgressToMult(seekBar.getProgress());
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mParmas[i] = convertProgressToMult(progress);
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

        private void addAdjustmentItem(int i, String title) {
            RelativeLayout v = (RelativeLayout) inflater.inflate(R.layout.layout_adjustment, null);
            ((TextView)v.findViewById(R.id.title)).setText(title);
            SeekBar seekBar = (SeekBar)v.findViewById(R.id.slider);
            setSeekBarListener(seekBar, i);
            addView(v);
        }

        public EditTaskParams getEditTaskParams() {
            EditTaskParams params = new EditTaskParams();
            params.exposure = mParmas[0];
            params.saturation = mParmas[1];
            params.contrast = mParmas[2];
            params.warmth = mParmas[3];
            params.hue = mParmas[4];
            params.structure = mParmas[5];
            return params;
        }

    }
}
