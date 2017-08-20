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

package jp.yamato.malta.android.galleryfragment.demo;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.util.ArrayList;

public class DemoUtils {

    public static void startActivity(Context context, Uri uri) {
        if (uri == null) {
            Toast.makeText(context, "We can't show it because of null uri ", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        context.startActivity(intent);
    }

    @NonNull
    public static ArrayList<Uri> getSampleUriFromExternalContent(Context context, int count) {
        // get image url
        Cursor cursor = context.getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Images.Media._ID}, null, null,
                        MediaStore.Images.Media.DATE_TAKEN + " DESC");
        if (cursor == null) {
            throw new IllegalStateException("We couldn't obtain media cursor!");
        }

        // check count
        int cursorCount = cursor.getCount();
        if (count > cursorCount) {
            Toast.makeText(context, "We don't have enough images requested number. ",
                    Toast.LENGTH_SHORT).show();
        }

        // create uri list
        ArrayList<Uri> list = new ArrayList<>();
        for (int i = 0; i < count && i < cursorCount; i++) {
            cursor.moveToPosition(i);
            long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
            Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    String.valueOf(id));
            list.add(uri);
        }

        cursor.close();

        return list;
    }

}
