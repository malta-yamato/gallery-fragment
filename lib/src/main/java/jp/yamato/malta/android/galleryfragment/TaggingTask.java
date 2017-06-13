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

import android.os.AsyncTask;

public abstract class TaggingTask<K, Params> extends AsyncTask<Params, Void, TaggingTask.Result> {

    private K mKey;
    private TaggedObject mTaggedObject;
    private Object mTag;
    private OnApplyListener<K> mListener;

    public TaggingTask(K key, TaggedObject taggedObject, OnApplyListener<K> listener) {
        mKey = key;
        mTaggedObject = taggedObject;
        mTag = taggedObject.getTag();
        if (mTag == null) {
            throw new IllegalArgumentException("TaggedObject must have a appropriate tag");
        }
        mListener = listener;
    }

    @Override
    protected void onPostExecute(Result result) {
        if (result == null || isCancelled()) {
            mTaggedObject = null;
            return;
        }

        Result revised;
        if (mListener != null) {
            revised = mListener.OnPostResult(mKey, result);
            if (revised == null) {
                revised = result;
            }
        } else {
            revised = result;
        }

        if (revised.isSuccessful) {
            if (mTaggedObject != null && mTag.equals(mTaggedObject.getTag())) {
                if (mListener != null) {
                    mListener.OnApply(mKey, mTaggedObject, revised);
                }
            }
        }

        mTaggedObject = null;
    }

    @Override
    protected void onCancelled() {
        mTaggedObject = null;
    }

    protected K getKey() {
        return mKey;
    }

    public interface OnApplyListener<K> {
        Result OnPostResult(K key, Result result);

        void OnApply(K key, TaggedObject taggedObject, Result result);
    }

    public static class Result {
        public boolean isSuccessful;
    }

    public interface TaggedObject {
        void setTag(Object tag);

        Object getTag();
    }

}

