package org.homio.api.entity.device;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import org.homio.api.model.device.ConfigDeviceDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * Uses in case if we don't need device endpoint configurations
 */
public interface DeviceEndpointsBehaviourContractStub extends DeviceEndpointsBehaviourContract {

    @JsonIgnore
    default @NotNull String getDeviceFullName() {
        return "-";
    }

    default @NotNull List<ConfigDeviceDefinition> findMatchDeviceConfigurations() {
        return List.of();
    }
}
