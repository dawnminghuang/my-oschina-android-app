package net.wolfcs.network;

import java.util.HashMap;
import java.util.Map;

import net.oschina.app.AppContext;
import net.oschina.app.api.ApiClient;
import net.oschina.app.bean.URLs;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.StringRequest;

public class LoginRequest extends StringRequest {
    private AppContext mAppContext;
    private Map<String, String> mParams;

    public LoginRequest(int method, String url, Listener<String> listener, ErrorListener errorListener) {
        super(method, url, listener, errorListener);
    }

    public LoginRequest(String url, Map<String, String> params, AppContext appContext, Listener<String> listener,
            ErrorListener errorListener) {
        super(Method.POST, url, listener, errorListener);
        mAppContext = appContext;
        mParams = params;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> params = new HashMap<String, String>();
        params.putAll(mParams);
        Map<String, String> superParams = super.getParams();
        if (superParams != null) {
            params.putAll(superParams);
        }

        return params;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<String, String>();

        Map<String, String> superHeaders = super.getHeaders();
        if (superHeaders != null) {
            headers.putAll(superHeaders);
        }

        String cookie = ApiClient.getCookie(mAppContext);
        String userAgent = ApiClient.getUserAgent(mAppContext);

        headers.put("Host", URLs.HOST);
        headers.put("Cookie", cookie);
        headers.put("User-Agent", userAgent);

        return headers;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        Map<String, String> responseHeaders = response.headers;
        String cookie = responseHeaders.get("Set-Cookie");
        mAppContext.setProperty("cookie", cookie);
        return super.parseNetworkResponse(response);
    }
}
