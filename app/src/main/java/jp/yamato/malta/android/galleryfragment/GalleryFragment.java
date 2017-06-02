package jp.yamato.malta.android.galleryfragment;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by malta on 2017/05/02.
 * GalleryFragment
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class GalleryFragment extends Fragment {
    @SuppressWarnings("unused")
    private static final String TAG = "GalleryFragment";

    private static final String ARG_RESOURCE = "arg_resource";
    private static final String ARG_SPAN_COUNT = "arg_span_count";
    private static final String ARG_DATA = "arg_data";

    private static final String SAVE_RESOURCE = "save_resource";
    private static final String SAVE_SPAN_COUNT = "save_span_count";
    private static final String SAVE_DATA = "save_data";

    private ImageAdapter.OnItemClickListener mOnItemClickListener;
    private RecyclerView mRecyclerView;
    private ImageAdapter mAdapter;

    private int mSpanCount = 2;

    // adapter
    private int mResource;

    private DeferredOperations mDeferredOperations;

    public GalleryFragment() {
        mDeferredOperations = new DeferredOperations();
    }

    public static GalleryFragment newInstance(int resource) {
        GalleryFragment instance = new GalleryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_RESOURCE, resource);
        instance.setArguments(args);
        return instance;
    }

    public static GalleryFragment newInstance(int resource, int spanCount) {
        GalleryFragment instance = new GalleryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_RESOURCE, resource);
        args.putInt(ARG_SPAN_COUNT, spanCount);
        instance.setArguments(args);
        return instance;
    }

    public static GalleryFragment newInstance(int resource, int spanCount, ArrayList<Uri> data) {
        GalleryFragment instance = new GalleryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_RESOURCE, resource);
        args.putInt(ARG_SPAN_COUNT, spanCount);
        args.putStringArrayList(ARG_DATA, GalleryFragmentUtils.toStringArrayList(data));
        instance.setArguments(args);
        return instance;
    }

    public void setAdapterData(final ArrayList<Uri> data) {
        mDeferredOperations.offer(new Runnable() {
            @Override
            public void run() {
                mAdapter.setAdapterData(data);
            }
        });
    }

    public ArrayList<Uri> getAdapterData() {
        if (!mDeferredOperations.isReleased()) {
            throw new IllegalStateException("get field is not ready");
        }
        return mAdapter.getAdapterData();
    }

    public Uri getAdapterDataItem(int position) {
        if (!mDeferredOperations.isReleased()) {
            throw new IllegalStateException("get field is not ready");
        }
        return mAdapter.getAdapterDataItem(position);
    }

    public void setResource(int resource) {
        mResource = resource;

        if (mAdapter == null) {
            return;
        }

        ArrayList<Uri> data = mAdapter.getAdapterData();
        ImageAdapter.LoadTask.BitmapLoader bitmapLoader = mAdapter.getBitmapLoader();
        Map<String, ImageAdapter.Formatter> formatter = mAdapter.getFormatter();

        int position = 0;
        View child = mRecyclerView.getChildAt(0);
        if (child != null) {
            position = mRecyclerView.getChildAdapterPosition(child);
        }

        mAdapter.destroy();

        mAdapter = new ImageAdapter(getContext(), mResource, data, mOnItemClickListener);
        mAdapter.setBitmapLoader(bitmapLoader);
        mAdapter.swapFormatter(formatter);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.scrollToPosition(position);
    }

    public void setLayoutManager(final RecyclerView.LayoutManager layoutManager){
        mDeferredOperations.offer(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setLayoutManager(layoutManager);
            }
        });
    }

    public void setBitmapLoader(final ImageAdapter.LoadTask.BitmapLoader loader) {
        mDeferredOperations.offer(new Runnable() {
            @Override
            public void run() {
                mAdapter.setBitmapLoader(loader);
            }
        });
    }

    public void setFormatter(final String tag, final ImageAdapter.Formatter formatter) {
        mDeferredOperations.offer(new Runnable() {
            @Override
            public void run() {
                mAdapter.setFormatter(tag, formatter);
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ImageAdapter.OnItemClickListener) {
            mOnItemClickListener = (ImageAdapter.OnItemClickListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        ArrayList<String> data = null;
        if (savedInstanceState != null) {
            mResource = savedInstanceState.getInt(SAVE_RESOURCE);
            mSpanCount = savedInstanceState.getInt(SAVE_SPAN_COUNT, 2);
            data = savedInstanceState.getStringArrayList(SAVE_DATA);
        } else {
            Bundle args = getArguments();
            if (args != null) {
                mResource = args.getInt(ARG_RESOURCE);
                mSpanCount = args.getInt(ARG_SPAN_COUNT, 2);
                data = args.getStringArrayList(ARG_DATA);
            }
        }

        // create recycler view
        mRecyclerView =
                (RecyclerView) inflater.inflate(R.layout.simple_recyclerview, container, false);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getActivity(), mSpanCount);
        mRecyclerView.setLayoutManager(layoutManager);

        // set adapter
        if (data != null) {
            mAdapter = new ImageAdapter(getContext(), mResource,
                    GalleryFragmentUtils.toUriArrayList(data), mOnItemClickListener);
        } else {
            mAdapter = new ImageAdapter(getContext(), mResource, null, mOnItemClickListener);
        }
        mRecyclerView.setAdapter(mAdapter);

        mDeferredOperations.release();

        return mRecyclerView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_RESOURCE, mResource);
        outState.putInt(SAVE_SPAN_COUNT, mSpanCount);
        outState.putStringArrayList(SAVE_DATA,
                GalleryFragmentUtils.toStringArrayList(mAdapter.getAdapterData()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // destroy
        mAdapter.destroy();
    }

}
