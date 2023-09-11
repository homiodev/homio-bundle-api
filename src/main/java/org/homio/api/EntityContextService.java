package org.homio.api;

import static java.lang.String.format;

import java.util.function.Consumer;
import org.homio.api.entity.BaseEntity;
import org.homio.api.model.HasEntityIdentifier;
import org.homio.api.service.EntityService;
import org.homio.api.service.EntityService.ServiceInstance;
import org.homio.api.util.SecureString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface EntityContextService {

    String MQTT_SERVICE = "MQTT";

    @NotNull EntityContext getEntityContext();

    void registerEntityTypeForSelection(@NotNull Class<? extends HasEntityIdentifier> entityClass, @NotNull String type);

    void registerUserRoleResource(String resource);

    default MQTTEntityService getMQTTEntityService(String entityID) {
        return getService(entityID, MQTTEntityService.class);
    }

    EntityService.ServiceInstance getEntityService(String entityID);

    default boolean isHasEntityService(String entityID) {
        return getEntityService(entityID) != null;
    }

    void addEntityService(String entityID, EntityService.ServiceInstance service);

    ServiceInstance removeEntityService(String entityID);

    @Nullable
    private <T> T getService(String entityID, Class<T> serviceClass) {
        BaseEntity entity = getEntityContext().getEntity(entityID);
        if (entity != null && !serviceClass.isAssignableFrom(entity.getClass())) {
            throw new IllegalStateException(format("Entity: '%s' has type: '%s' but require: '%s'", entityID, entity.getType(), serviceClass.getSimpleName()));
        }
        return (T) entity;
    }

    interface MQTTEntityService extends HasEntityIdentifier {

        String getUser();

        SecureString getPassword();

        String getHostname();

        int getPort();

        void publish(String topic, byte[] payload, int qos, boolean retained);

        void addListener(String topic, String discriminator, Consumer<Object> listener);

        void removeListener(String topic, String discriminator);
    }
}
