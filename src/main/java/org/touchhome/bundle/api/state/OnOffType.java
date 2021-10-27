package org.touchhome.bundle.api.state;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public class OnOffType implements State {

    public enum OnOffTypeEnum {
        Off, On;

        public boolean boolValue() {
            return this == On;
        }
    }

    public static final OnOffType ON = new OnOffType(true);
    public static final OnOffType OFF = new OnOffType(false);

    public static OnOffType of(boolean on) {
        return on ? ON : OFF;
    }

    @Getter
    private final boolean value;

    @Getter
    @Setter
    private Boolean oldValue;

    private OnOffType(boolean value) {
        this(value, value);
    }

    public OnOffType(boolean value, Boolean oldValue) {
        this.value = value;
        this.oldValue = oldValue;
    }

    @Override
    public String toString() {
        return value ? "ON" : "OFF";
    }

    @Override
    public boolean equalToOldValue() {
        return Objects.equals(value, oldValue);
    }

    @Override
    public float floatValue() {
        return this == ON ? 1 : 0;
    }

    @Override
    public int intValue() {
        return this == ON ? 1 : 0;
    }

    @Override
    public RawType toRawType() {
        return null;
    }

    @Override
    public boolean boolValue() {
        return this == ON;
    }

    @Override
    public String stringValue() {
        return String.valueOf(intValue());
    }

    public <T extends State> T as(Class<T> target) {
        if (target == DecimalType.class) {
            return target.cast(this == ON ? new DecimalType(1) : DecimalType.ZERO);
        } else if (target == HSBType.class) {
            return target.cast(this == ON ? HSBType.WHITE : HSBType.BLACK);
        } else {
            return State.super.as(target);
        }
    }
}
