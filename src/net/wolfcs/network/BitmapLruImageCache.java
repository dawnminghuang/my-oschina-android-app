package net.wolfcs.network;

import com.android.volley.toolbox.ImageLoader.ImageCache;

import android.graphics.Bitmap;

/**
 * Basic LRU Memory cache.
 * 
 * @author Trey Robinson
 * 
 */
public class BitmapLruImageCache extends LruCache<String, Bitmap> implements ImageCache {

    public BitmapLruImageCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getRowBytes() * value.getHeight();
    }

    @Override
    public Bitmap getBitmap(String url) {
        return get(url);
    }

    @Override
    public void putBitmap(String url, Bitmap bitmap) {
        put(url, bitmap);
    }
}
