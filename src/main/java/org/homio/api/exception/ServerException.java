package org.homio.api.exception;

import org.homio.api.util.FlowMap;
import org.homio.api.util.Lang;
import org.jetbrains.annotations.NotNull;

public class ServerException extends RuntimeException {

    public ServerException(@NotNull String message) {
        super(Lang.getServerMessage(message));
    }

    public ServerException(@NotNull Exception ex) {
        super(ex);
    }

    public ServerException(@NotNull String message, @NotNull Exception ex) {
        super(Lang.getServerMessage(message), ex);
    }

    public ServerException(@NotNull String message, @NotNull FlowMap messageParam) {
        super(Lang.getServerMessage(message, messageParam));
    }

    public ServerException(@NotNull String message, @NotNull String param0, @NotNull Object value0) {
        this(message, FlowMap.of(param0, value0));
    }

    public ServerException(@NotNull String message, @NotNull Object value0) {
        this(Lang.getServerMessage(message, String.valueOf(value0)));
    }
}
