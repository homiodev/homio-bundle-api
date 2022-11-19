package org.touchhome.bundle.api.state;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.touchhome.common.util.CommonUtils;

import java.nio.charset.Charset;
import java.util.Map;

public interface State {

    static State of(Object value) {
        if (value == null || value instanceof State) return (State) value;
        if (value instanceof Map) {
            return new JsonType(CommonUtils.OBJECT_MAPPER.convertValue(value, JsonNode.class));
        }
        if (Number.class.isAssignableFrom(value.getClass())) {
            if (value instanceof Double) {
                return new DecimalType((double) value);
            } else if (value instanceof Integer) {
                return new DecimalType((int) value);
            }
            return new DecimalType((long) value);
        }
        if (value instanceof Boolean) {
            return OnOffType.of((boolean) value);
        }
        if (value instanceof String) {
            return new StringType(value.toString());
        }
        return new ObjectType(value);
    }

    default boolean equalToOldValue() {
        throw new IllegalStateException("Unable to invoke equality for non state class");
    }

    float floatValue();

    int intValue();

    default String toFullString() {
        return stringValue();
    }

    default long longValue() {
        return intValue();
    }

    default RawType toRawType() {
        return RawType.ofPlainText(stringValue());
    }

    default boolean boolValue() {
        String value = stringValue();
        return value.equals("1") || value.equalsIgnoreCase("true");
    }

    default byte[] byteArrayValue() {
        return toString().getBytes(Charset.defaultCharset());
    }

    default String stringValue() {
        return toString();
    }

    default <T extends State> T as(Class<T> target) {
        if (target != null && target.isInstance(this)) {
            return target.cast(this);
        } else {
            return null;
        }
    }

    @SneakyThrows
    default State optional(String value) {
        return StringUtils.isEmpty(value) ? this :
                CommonUtils.findObjectConstructor(this.getClass(), String.class).newInstance(value);
    }
}
