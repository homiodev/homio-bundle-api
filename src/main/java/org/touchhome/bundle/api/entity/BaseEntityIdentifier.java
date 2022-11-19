package org.touchhome.bundle.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.model.HasEntityIdentifier;

import java.io.Serializable;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

public interface BaseEntityIdentifier<T> extends HasEntityIdentifier, Serializable {

    @JsonIgnore
    String getDefaultName();

    default String getTitle() {
        return defaultIfBlank(getName(), defaultIfBlank(getDefaultName(), getEntityID()));
    }

    default String getType() {
        return this.getClass().getSimpleName();
    }

    String getName();

    default void afterDelete(EntityContext entityContext) {

    }

    default void afterUpdate(EntityContext entityContext, boolean persis) {

    }

    // fires after fetch from db/cache
    default void afterFetch(EntityContext entityContext) {

    }

    @JsonIgnore
    default String refreshName() {
        return getDefaultName();
    }

    @JsonIgnore
    String getEntityPrefix();
}
