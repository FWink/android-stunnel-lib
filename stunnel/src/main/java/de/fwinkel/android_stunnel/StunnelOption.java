package de.fwinkel.android_stunnel;

import androidx.annotation.NonNull;

/**
 * Represents a key-value-pair that is used to specify options in a Stunnel config file.
 * They formatted like this in the config files:<br/>
 * key = value<br/>
 * If T implements {@link StunnelValue} then {@link StunnelValue#toStunnelValue()} is called
 * to format the valued, otherwise {@link Object#toString()} is used.
 * @param <T>
 */
public class StunnelOption<T> {
    @NonNull protected final String name;
    @NonNull protected final T value;

    public StunnelOption(@NonNull String name, @NonNull T value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Formats the option as a {@link String} suitable for a Stunnel config file ("key = value")
     * @return
     */
    public String toConfigString() {
        String str;
        if(value instanceof StunnelValue)
            str = ((StunnelValue) value).toStunnelValue();
        else
            str = value.toString();

        //TODO escaping?
        return name + " = " + str;
    }
}
