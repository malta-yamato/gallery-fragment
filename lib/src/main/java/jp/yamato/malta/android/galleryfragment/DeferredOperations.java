package jp.yamato.malta.android.galleryfragment;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Takeru on 2016/03/06.
 * DeferredOperations
 */
public class DeferredOperations {

    private Queue<Runnable> mQueue;
    private boolean mIsReleased;

    public DeferredOperations() {
        mQueue = new LinkedList<>();
        mIsReleased = false;
    }

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
