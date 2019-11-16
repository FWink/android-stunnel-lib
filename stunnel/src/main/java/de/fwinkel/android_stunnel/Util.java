package de.fwinkel.android_stunnel;

import java.io.Closeable;
import java.io.IOException;

class Util {

    /**
     * Silently closes the given {@link Closeable}
     * @param closeable
     */
    public static void close(Closeable closeable) {
        try {
            if(closeable != null)
                closeable.close();
        } catch (IOException e) {
            //ignore
        }
    }
}
