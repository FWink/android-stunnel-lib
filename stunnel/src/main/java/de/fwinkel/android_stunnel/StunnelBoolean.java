package de.fwinkel.android_stunnel;

public class StunnelBoolean implements StunnelValue {
    protected final boolean value;

    StunnelBoolean(boolean value) {
        this.value = value;
    }

    @Override
    public String toStunnelValue() {
        return this.value ? "yes" : "no";
    }
}
