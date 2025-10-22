package app.utils;

import io.javalin.http.Context;

public class ResponseUtil {

    public static void disableCache(Context ctx) {
        ctx.header("Cache-Control", "no-cache, no-store, must-revalidate");
        ctx.header("Pragma", "no-cache");
        ctx.header("Expires", "0");
    }
}
