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

import java.util.LinkedList;
import java.util.Queue;

public class DeferredOperations {

    private Queue<Runnable> mQueue;
    private boolean mIsReleased;

    public DeferredOperations() {
        mQueue = new LinkedList<>();
        mIsReleased = false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isReleased() {
        return mIsReleased;
    }

    public void release() {
        while (true) {
            Runnable runnable = mQueue.poll();
            if (runnable != null) {
                runnable.run();
            } else {
                break;
            }
        }
        mIsReleased = true;
    }

    public boolean offer(Runnable runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException();
        }

        if (mIsReleased) {
            runnable.run();
            return true;
        } else {
            return mQueue.offer(runnable);
        }
    }

}
