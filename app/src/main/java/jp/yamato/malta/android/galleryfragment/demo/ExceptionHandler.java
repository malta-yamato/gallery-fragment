package jp.yamato.malta.android.galleryfragment.demo;

/**
 * Created by malta on 2017/05/06.
 * ExceptionHandler
 */

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