package de.fwinkel.android_stunnel;

import java.util.List;

import androidx.annotation.NonNull;

public class PreSharedKey {
    @NonNull
    protected final String identity;
    @NonNull
    protected final String key;

    public PreSharedKey(@NonNull String identity, @NonNull String key) {
        this.identity = identity;
        this.key = key;
    }

    @NonNull
    public String getIdentity() {
        return identity;
    }

    @NonNull
    public String getKey() {
        return key;
    }

    /**
     * Formats the PSK into a {@link String} suitable for the PSKSecrets config file
     * (identity:key)
     * @return
     */
    @NonNull
    public String toConfigString() {
        //TODO escaping?
        return identity + ":" + key;
    }

    /**
     * Formas the given PSK secrets into a config file as required by
     * {@link StunnelBuilder.ServiceBuilder#pskSecrets(List)}.
     * @param pskSecrets
     * @return
     * @see #toConfigString()
     */
    @NonNull
    public static String makePskSecretsFile(List<PreSharedKey> pskSecrets) {
        StringBuilder str = new StringBuilder();

        for (PreSharedKey pskSecret : pskSecrets) {
            if(str.length() > 0)
                str.append('\n');
            str.append(pskSecret.toConfigString());
        }

        return str.toString();
    }
}
