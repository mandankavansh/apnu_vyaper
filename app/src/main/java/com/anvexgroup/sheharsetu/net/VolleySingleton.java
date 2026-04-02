package com.anvexgroup.sheharsetu.net;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;

public class VolleySingleton {
    private static volatile VolleySingleton instance;

    private final RequestQueue queue;

    private VolleySingleton(Context ctx) {
        Context appCtx = ctx.getApplicationContext();
        queue = Volley.newRequestQueue(appCtx, new HurlStack());
    }

    public static VolleySingleton getInstance(Context ctx) {
        if (instance == null) {
            synchronized (VolleySingleton.class) {
                if (instance == null) instance = new VolleySingleton(ctx);
            }
        }
        return instance;
    }

    public static RequestQueue queue(Context ctx) {
        return getInstance(ctx).getQueue();
    }

    public RequestQueue getQueue() {
        return queue;
    }

    public <T> void add(Request<T> req) {
        queue.add(req);
    }
}