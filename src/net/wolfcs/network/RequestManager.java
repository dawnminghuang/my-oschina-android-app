package net.wolfcs.network;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class RequestManager {
    private RequestQueue mRequestQueue;

    private static volatile RequestManager sRequestManager;

    private RequestManager(Context context) {
        mRequestQueue = Volley.newRequestQueue(context);
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
}
