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

    private int mResource;

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

    public void setAdapterData(ArrayList<Uri> data) {
        if (mAdapter == null) {
            throw new IllegalStateException("call at least after onStart");
        }
        mAdapter.setAdapterData(data);
        mAdapter.notifyDataSetChanged();
    }

    public ArrayList<Uri> getAdapterData() {
        if (mAdapter == null) {
            throw new IllegalStateException("call at least after onStart");
        }
        return mAdapter.getAdapterData();
    }

    public void setBitmapLoader(ImageAdapter.LoadTask.BitmapLoader loader) {
        if (mAdapter == null) {
            throw new IllegalStateException("call at least after onStart");
        }
        mAdapter.setBitmapLoader(loader);
    }

    public void setFormatter(String tag, ImageAdapter.Formatter formatter) {
        if (mAdapter == null) {
            throw new IllegalStateException("call at least after onStart");
        }
        mAdapter.setFormatter(tag, formatter);
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

        // cancel tasks
        ImageAdapter.LoadTask.cancelAll();

        // destroy
        mAdapter.destroyImageView();
    }

}
