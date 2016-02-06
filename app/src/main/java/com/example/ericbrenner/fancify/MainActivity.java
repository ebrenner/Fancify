package com.example.ericbrenner.fancify;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
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
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnEditSignPostListener {

    private static final int REQUEST_SELECT_PHOTO = 1;

    private ImageView mImageView;
    private Button mButton;
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
                    setImage(false);
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
    }

    @Override
    public void onClick(View v) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_SELECT_PHOTO);
    }

    @Override
    public void onEditStarted() {

    }

    @Override
    public void onEditCanceled() {

    }

    @Override
    public void onEditFinished(Bitmap bitmap) {
        mImageView.setImageBitmap(bitmap);
    }

    private void setImage(boolean applyAdjustments) {
        try {
            final InputStream imageStream = getContentResolver().openInputStream(mUri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 6;
            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream, null, options);
            if (applyAdjustments) {
                if (mEditTask != null) {
                    mEditTask.cancel(true);
                }
                mEditTask = new EditTask(selectedImage, MainActivity.this);
                EditTaskParams params = mAdjustmentsPagerAdapter.getEditTaskParams();
                mEditTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
            } else {
                mImageView.setImageBitmap(selectedImage);
                if (!mImageSelected) {
                    mImageSelected = true;
                    setUpAdjustmentsPager();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void setUpAdjustmentsPager() {
        mPager = (ViewPager) findViewById(R.id.pager);
        mAdjustmentsPagerAdapter = new AdjustmentsPagerAdapter();
        mPager.setAdapter(mAdjustmentsPagerAdapter);


    }

    private class AdjustmentsPagerAdapter extends PagerAdapter {

        private ArrayList<View> views = new ArrayList<View>();
        LayoutInflater inflater = getLayoutInflater();
        float[] mParmas = new float[3];

        public AdjustmentsPagerAdapter() {
            addAdjustmentItem(0, getString(R.string.exposure));
            addAdjustmentItem(1, getString(R.string.saturation));
            addAdjustmentItem(2, getString(R.string.contrast));
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
            return progress/5.0f;
        }

        private void setSeekBarListener(SeekBar seekBar, final int i) {
            mParmas[i] = convertProgressToMult(seekBar.getProgress());
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mParmas[i] = convertProgressToMult(progress);
                    setImage(true);
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
            return params;
        }

    }
}
