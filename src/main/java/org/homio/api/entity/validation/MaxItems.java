package org.homio.api.entity.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Restriction to max num of items in Set. i.e. WidgetSeriesEntity
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface MaxItems {

  int value();
}
