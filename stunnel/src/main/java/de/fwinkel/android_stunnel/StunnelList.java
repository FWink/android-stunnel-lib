package de.fwinkel.android_stunnel;

import java.util.List;

import androidx.annotation.NonNull;

/**
 * Stunnel value that formats a list of values in a colon delimited string:<br/>
 * value1:value2...<br/>
 * If T implements {@link StunnelValue} then {@link StunnelValue#toStunnelValue()} is called
 * to format the values, otherwise {@link Object#toString()} is used.
 * @param <T>
 */
public class StunnelList<T> implements StunnelValue {
    protected static final String VALUE_LIST_DELIMITER = ":";

    @NonNull
    protected List<T> values;

    public StunnelList(@NonNull List<T> values) {
        this.values = values;
    }

    @Override
    public String toStunnelValue() {
        StringBuilder serialized = new StringBuilder();

        for (T val : values) {
            if(serialized.length() > 0)
                serialized.append(VALUE_LIST_DELIMITER);

            String str;
            if(val instanceof StunnelValue)
                str = ((StunnelValue) val).toStunnelValue();
            else
                str = val.toString();
            //TODO escaping?
            serialized.append(str);
        }

        return serialized.toString();
    }
}
