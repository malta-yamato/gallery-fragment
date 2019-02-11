/*
 * Copyright (C) 2017 MALTA-YAMATO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.yamato.malta.android.galleryfragment;

import android.content.Context;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
                return new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false);
            case LINEAR_LAYOUT_VERTICAL:
                return new LinearLayoutManager(context, RecyclerView.VERTICAL, false);
            default:
                throw new IllegalArgumentException("layout not match");
        }
    }

}
