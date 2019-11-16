package de.fwinkel.android_stunnel;

public enum SSLVersion implements StunnelValue {
    ALL("all"),
    SSLv2("SSLv2"),
    SSLv3("SSLv3"),
    TLSv1("TLSv1"),
    TLSv1_1("TLSv1.1"),
    TLSv1_2("TLSv1.2"),
    TLSv1_3("TLSv1.3");

    public final String value;

    SSLVersion(String value) {
        this.value = value;
    }

    @Override
    public String toStunnelValue() {
        return value;
    }
}