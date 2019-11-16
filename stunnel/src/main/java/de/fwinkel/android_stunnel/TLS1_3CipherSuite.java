package de.fwinkel.android_stunnel;

public enum TLS1_3CipherSuite implements StunnelValue {
    TLS_AES_256_GCM_SHA384("TLS_AES_256_GCM_SHA384"),
    TLS_CHACHA20_POLY1305_SHA256("TLS_CHACHA20_POLY1305_SHA256"),
    TLS_AES_128_GCM_SHA256("TLS_AES_128_GCM_SHA256");

    public final String value;

    TLS1_3CipherSuite(String value) {
        this.value = value;
    }

    @Override
    public String toStunnelValue() {
        return value;
    }
}
