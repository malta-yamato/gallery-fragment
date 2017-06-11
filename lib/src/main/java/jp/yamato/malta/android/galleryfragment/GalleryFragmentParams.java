package jp.yamato.malta.android.galleryfragment;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;

/**
 * Created by Malta on 2017/05/29.
 * GalleryFragmentParams
 */

public class GalleryFragmentParams {

    public static final int GRID_LAYOUT = 0;
    public static final int LINEAR_LAYOUT_HORIZONTAL = 10;
    public static final int LINEAR_LAYOUT_VERTICAL = 11;

    public static RecyclerView.LayoutManager resolveLayoutManager(Context context, int layout,
            int spanCount) {
        switch (layout) {
            case GRID_LAYOUT:
                return new GridLayoutManager(context, spanCount);
            case LINEAR_LAYOUT_HORIZONTAL:
                return new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            case LINEAR_LAYOUT_VERTICAL:
                return new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
            default:
                throw new IllegalArgumentException("layout not match");
        }
    }


}
