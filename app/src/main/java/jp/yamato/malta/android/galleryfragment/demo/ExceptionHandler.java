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

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Callback mCallback;
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    public ExceptionHandler(Callback callback) {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        mCallback = callback;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (mCallback != null) {
            mCallback.onUncaughtExceptionThrown(e);
        }
        mDefaultHandler.uncaughtException(t, e);
    }

    interface Callback {
        void onUncaughtExceptionThrown(Throwable e);
    }
}