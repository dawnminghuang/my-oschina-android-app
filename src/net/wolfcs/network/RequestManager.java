package net.wolfcs.network;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

public class RequestManager {
    private static final int MAX_CACHE_SIZE = 10 * 1024 * 1024;

    private RequestQueue mRequestQueue;
    private ImageLoader.ImageCache mImageCahce;
    private ImageLoader mImageLoader;

    private static volatile RequestManager sRequestManager;

    private RequestManager(Context context) {
        mRequestQueue = Volley.newRequestQueue(context);
        mImageCahce = new BitmapLruImageCache(MAX_CACHE_SIZE);
        mImageLoader = new ImageLoader(mRequestQueue, mImageCahce);
    }

    public static RequestManager getRequestManager(Context context) {
        if (sRequestManager == null) {
            synchronized (RequestManager.class) {
                if (sRequestManager == null) {
                    sRequestManager = new RequestManager(context);
                }
            }
        }
        return sRequestManager;
    }

    public void executeRequest(Request request) {
        mRequestQueue.add(request);
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }
}
