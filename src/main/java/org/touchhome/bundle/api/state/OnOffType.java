package org.touchhome.bundle.api.state;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public class OnOffType implements State {

    public static final OnOffType ON = new OnOffType(true);
    public static final OnOffType OFF = new OnOffType(false);

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

    public static OnOffType of(boolean on) {
        return on ? ON : OFF;
    }

    public OnOffType invert() {
        return value ? OFF : ON;
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
        return value ? "1" : "0";
    }

    @Override
    public String toString() {
        return value ? "ON" : "OFF";
    }

    public <T extends State> T as(Class<T> target) {
        if (target == DecimalType.class) {
            return target.cast(this == ON ? new DecimalType(1) : DecimalType.ZERO);
        } else {
            return State.super.as(target);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OnOffType onOffType = (OnOffType) o;

        return value == onOffType.value;
    }

    @Override
    public int hashCode() {
        return (value ? 1 : 0);
    }

    public enum OnOffTypeEnum {
        Off, On;

        public boolean boolValue() {
            return this == On;
        }
    }
}
