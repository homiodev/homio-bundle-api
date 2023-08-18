package org.homio.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pivovarit.function.ThrowingConsumer;
import com.pivovarit.function.ThrowingFunction;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.homio.api.console.ConsolePlugin;
import org.homio.api.entity.BaseEntity;
import org.homio.api.entity.BaseEntityIdentifier;
import org.homio.api.entity.HasStatusAndMsg;
import org.homio.api.entity.device.DeviceBaseEntity;
import org.homio.api.entity.device.DeviceEndpointsBehaviourContract;
import org.homio.api.exception.ServerException;
import org.homio.api.model.ActionResponseModel;
import org.homio.api.model.Icon;
import org.homio.api.model.Status;
import org.homio.api.setting.SettingPluginButton;
import org.homio.api.ui.UI.Color;
import org.homio.api.ui.action.UIActionHandler;
import org.homio.api.ui.dialog.DialogModel;
import org.homio.api.ui.field.action.ActionInputParameter;
import org.homio.api.ui.field.action.v1.UIInputBuilder;
import org.homio.api.ui.field.action.v1.layout.UIFlexLayoutBuilder;
import org.homio.api.ui.field.action.v1.layout.UILayoutBuilder;
import org.homio.api.util.FlowMap;
import org.homio.api.util.Lang;
import org.homio.api.util.NotificationLevel;
import org.homio.hquery.ProgressBar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.homio.api.util.CommonUtils.getErrorMessage;

@SuppressWarnings("unused")
public interface EntityContextUI {

    Logger log = LogManager.getLogger("asd");

    @NotNull EntityContext getEntityContext();

    @NotNull UIInputBuilder inputBuilder();

    @SneakyThrows
    default <T> T runWithProgressAndGet(@NotNull String progressKey, boolean cancellable,
                                        @NotNull ThrowingFunction<ProgressBar, T, Exception> process,
                                        @Nullable Consumer<Exception> finallyBlock) {
        ProgressBar progressBar = (progress, message, error) -> progress(progressKey, progress, message, cancellable);
        Exception exception = null;
        try {
            progressBar.progress(0, progressKey);
            return process.apply(progressBar);
        } catch (Exception ex) {
            exception = ex;
            throw ex;
        } finally {
            progressBar.done();
            if (finallyBlock != null) {
                try {
                    finallyBlock.accept(exception);
                } catch (Exception ex) {
                    log.error("Error has been occurred in thread finally block", ex);
                }
            }
        }
    }

    @SneakyThrows
    default void runWithProgress(
            @NotNull String progressKey,
            boolean cancellable,
            @NotNull ThrowingConsumer<ProgressBar, Exception> process,
            @Nullable Consumer<Exception> finallyBlock) {
        runWithProgressAndGet(progressKey, cancellable, progressBar -> {
            process.accept(progressBar);
            return null;
        }, finallyBlock);
    }

    /**
     * Register console plugin name. In case if console plugin available only if some entity is created or not enabled by some case we may show disabled console
     * name on UI
     *
     * @param name     - plugin name
     * @param resource - resource name requires if you need restrict access for users with roles
     */
    void registerConsolePluginName(@NotNull String name, @Nullable String resource);

    <T extends ConsolePlugin> void registerConsolePlugin(@NotNull String name, @NotNull T plugin);

    @Nullable <T extends ConsolePlugin> T getRegisteredConsolePlugin(@NotNull String name);

    boolean unRegisterConsolePlugin(@NotNull String name);

    /**
     * Fire open console window to UI
     *
     * @param consolePlugin -
     * @param <T>           -
     */
    <T extends ConsolePlugin<?>> void openConsole(@NotNull T consolePlugin);

    /**
     * Request to reload window to UI
     *
     * @param reason          -
     * @param timeoutToReload - timeout to reload. Range 5..60 seconds
     */
    void reloadWindow(@NotNull String reason, int timeoutToReload);

    default void reloadWindow(@NotNull String reason) {
        reloadWindow(reason, 5);
    }

    void removeItem(@NotNull BaseEntity baseEntity);

    void updateItem(@NotNull BaseEntity baseEntity);

    default void updateItems(@NotNull Class<? extends BaseEntity> baseEntityClass) {
        for (BaseEntity baseEntity : getEntityContext().findAll(baseEntityClass)) {
            updateItem(baseEntity);
        }
    }

    /**
     * Update specific field
     *
     * @param baseEntity  -
     * @param updateField -
     * @param value       -
     */
    void updateItem(@NotNull BaseEntityIdentifier baseEntity, @NotNull String updateField, @Nullable Object value);

    /**
     * Update specific field inside @UIFieldInlineEntities
     *
     * @param parentEntity    - holder entity entity. i.e.: ZigBeeDeviceEntity
     * @param parentFieldName - parent field name that holds Set of destinations. i.e.: 'endpoints'
     * @param innerEntityID   - target field entity ID to update from inside Set
     * @param updateField     - specific field name to update inside innerEntity
     * @param value           - value to send to UI
     */
    void updateInnerSetItem(@NotNull BaseEntityIdentifier parentEntity, @NotNull String parentFieldName, @NotNull String innerEntityID,
                            @NotNull String updateField, @NotNull Object value);

    /**
     * Fire update to ui that entity was changed.
     *
     * @param entity -
     * @param <T>    -
     */
    <T extends BaseEntity> void sendEntityUpdated(@NotNull T entity);

    void progress(@NotNull String key, double progress, @Nullable String message, boolean cancellable);

    /**
     * Remove progress bar from UI
     *
     * @param key - progress id
     */
    default void progressDone(@NotNull String key) {
        progress(key, 100D, null, false);
    }

    default void sendConfirmation(@NotNull String key, @NotNull String title, @NotNull Runnable confirmHandler,
                                  @NotNull Collection<String> messages, @Nullable String headerButtonAttachTo) {
        sendConfirmation(key, title, responseType -> {
            if (responseType == DialogResponseType.Accepted) {
                confirmHandler.run();
            }
        }, messages, 0, headerButtonAttachTo);
    }

    /**
     * * Send confirmation message to ui with back handler
     *
     * @param headerButtonAttachTo - if set - attach confirm message to header button
     * @param key                  -
     * @param title                -
     * @param confirmHandler       -
     * @param messages             -
     * @param maxTimeoutInSec      -
     */
    default void sendConfirmation(@NotNull String key, @NotNull String title,
                                  @NotNull Consumer<DialogResponseType> confirmHandler, @NotNull Collection<String> messages,
                                  int maxTimeoutInSec, @Nullable String headerButtonAttachTo) {
        sendDialogRequest(key, title, (responseType, pressedButton, parameters) -> confirmHandler.accept(responseType),
                dialogModel -> {
                    List<ActionInputParameter> inputs =
                            messages.stream().map(ActionInputParameter::message).collect(Collectors.toList());
                    dialogModel.headerButtonAttachTo(headerButtonAttachTo).submitButton("Confirm", button -> {
                    }).group("General", inputs);
                });
    }

    /**
     * Send request dialog to ui
     *
     * @param dialogModel -
     */
    void sendDialogRequest(@NotNull DialogModel dialogModel);

    default void sendDialogRequest(@NotNull String key, @NotNull String title, @NotNull DialogRequestHandler actionHandler,
                                   @NotNull Consumer<DialogModel> dialogBuilderSupplier) {
        DialogModel dialogModel = new DialogModel(key, title, actionHandler);
        dialogBuilderSupplier.accept(dialogModel);
        sendDialogRequest(dialogModel);
    }

    /**
     * Remove notification block if it has no rows anymore
     *
     * @param key - block id
     */
    void removeEmptyNotificationBlock(@NotNull String key);

    void addNotificationBlock(@NotNull String key, @NotNull String name, @Nullable Icon icon,
                              @Nullable Consumer<NotificationBlockBuilder> builder);

    default void addOrUpdateNotificationBlock(@NotNull String key, @NotNull String name, @Nullable Icon icon,
                                              @NotNull Consumer<NotificationBlockBuilder> builder) {
        if (isHasNotificationBlock(key)) {
            updateNotificationBlock(key, builder);
        } else {
            addNotificationBlock(key, name, icon, builder);
        }
    }

    default void addNotificationBlockOptional(@NotNull String key, @NotNull String name, @Nullable Icon icon) {
        if (!isHasNotificationBlock(key)) {
            addNotificationBlock(key, name, icon, null);
        }
    }

    default void updateNotificationBlock(@NotNull String key, @NotNull BaseEntity entity) {
        updateNotificationBlock(key, builder -> builder.addEntityInfo(entity));
    }

    void updateNotificationBlock(@NotNull String key, @NotNull Consumer<NotificationBlockBuilder> builder);

    boolean isHasNotificationBlock(@NotNull String key);

    void removeNotificationBlock(@NotNull String key);

    // raw
    void sendNotification(@NotNull String destination, @NotNull String param);

    // raw
    void sendNotification(@NotNull String destination, @NotNull ObjectNode param);

    // Add button to ui header
    HeaderButtonBuilder headerButtonBuilder(@NotNull String key);

    /**
     * Remove button from ui header. Header button will be removed only if it has no attached elements
     *
     * @param key -
     */
    default void removeHeaderButton(@NotNull String key) {
        removeHeaderButton(key, null, false);
    }

    /**
     * Remove header button on ui
     *
     * @param key         - id
     * @param icon        - changed icon if btn has attached elements
     * @param forceRemove - force remove even if header button has attached elements
     */
    void removeHeaderButton(@NotNull String key, @Nullable String icon, boolean forceRemove);

    /**
     * Show error toastr message to ui
     *
     * @param message -
     */
    default void sendErrorMessage(@NotNull String message) {
        sendErrorMessage(null, message, null, null);
    }

    /**
     * Show error toastr message to ui
     *
     * @param ex -
     */
    default void sendErrorMessage(@NotNull Exception ex) {
        sendErrorMessage(null, null, null, ex);
    }

    /**
     * Show error toastr message to ui
     *
     * @param ex      -
     * @param message -
     */
    default void sendErrorMessage(@NotNull String message, @NotNull Exception ex) {
        sendErrorMessage(null, message, null, ex);
    }

    /**
     * Show error toastr message to ui
     *
     * @param message -
     * @param title   -
     */
    default void sendErrorMessage(@NotNull String title, @NotNull String message) {
        sendErrorMessage(title, message, null, null);
    }

    default void sendErrorMessage(@NotNull String title, @NotNull String message, @NotNull Exception ex) {
        sendErrorMessage(title, message, null, ex);
    }

    default void sendErrorMessage(@NotNull String message, @NotNull FlowMap messageParam, @NotNull Exception ex) {
        sendErrorMessage(null, message, messageParam, ex);
    }

    default void sendErrorMessage(@NotNull String message, @NotNull FlowMap messageParam) {
        sendErrorMessage(null, message, messageParam, null);
    }

    default void sendErrorMessage(@Nullable String title, @Nullable String message, @Nullable FlowMap messageParam,
                                  @Nullable Exception ex) {
        sendMessage(title, message, NotificationLevel.error, messageParam, ex);
    }

    default void sendInfoMessage(@NotNull String message) {
        sendInfoMessage(null, message, null);
    }

    default void sendInfoMessage(@NotNull String title, @NotNull String message) {
        sendInfoMessage(title, message, null);
    }

    default void sendInfoMessage(@NotNull String message, @NotNull FlowMap messageParam) {
        sendInfoMessage(null, message, messageParam);
    }

    default void sendInfoMessage(@Nullable String title, @NotNull String message, @Nullable FlowMap messageParam) {
        sendMessage(title, message, NotificationLevel.info, messageParam, null);
    }

    default void sendSuccessMessage(@NotNull String message) {
        sendSuccessMessage(null, message, null);
    }

    default void sendSuccessMessage(@NotNull String title, @NotNull String message) {
        sendSuccessMessage(title, message, null);
    }

    default void sendSuccessMessage(@NotNull String message, @NotNull FlowMap messageParam) {
        sendSuccessMessage(null, message, messageParam);
    }

    default void sendSuccessMessage(@Nullable String title, @NotNull String message, @Nullable FlowMap messageParam) {
        sendMessage(title, message, NotificationLevel.success, messageParam, null);
    }

    default void sendWarningMessage(@NotNull String message) {
        sendWarningMessage(null, message, null);
    }

    default void sendWarningMessage(@NotNull String title, @NotNull String message) {
        sendWarningMessage(title, message, null);
    }

    default void sendWarningMessage(@NotNull String message, @NotNull FlowMap messageParam) {
        sendWarningMessage(null, message, messageParam);
    }

    default void sendWarningMessage(@Nullable String title, @NotNull String message, @Nullable FlowMap messageParam) {
        sendMessage(title, message, NotificationLevel.warning, messageParam, null);
    }

    default void sendJsonMessage(@NotNull String title, @NotNull Object json) {
        sendJsonMessage(title, json, null);
    }

    void sendJsonMessage(@Nullable String title, @NotNull Object json, @Nullable FlowMap messageParam);

    default void sendMessage(@Nullable String title, @Nullable String message, @Nullable NotificationLevel level,
                             @Nullable FlowMap messageParam, @Nullable Exception ex) {
        title = title == null ? null : Lang.getServerMessage(title, messageParam);
        String text;
        if (ex instanceof ServerException) {
            text = ex.getMessage();
        } else {
            text = StringUtils.isEmpty(message) ? ex == null ? "Unknown error" : ex.getMessage() : message;
            if (text == null) {
                text = getErrorMessage(ex);
            }
            // try cast text to lang
            text = Lang.getServerMessage(text, messageParam);
        }
        sendMessage(title, text, level);
    }

    void sendMessage(@Nullable String title, @Nullable String message, @Nullable NotificationLevel level);

    enum DialogResponseType {
        Cancelled, Timeout, Accepted
    }

    interface DialogRequestHandler {
        void handle(@NotNull DialogResponseType responseType, @NotNull String pressedButton, @NotNull ObjectNode parameters);
    }

    interface HeaderButtonBuilder {

        @NotNull HeaderButtonBuilder title(@NotNull String title);

        @NotNull HeaderButtonBuilder icon(@NotNull Icon icon);

        /**
         * Set border
         *
         * @param width - default 1
         * @param color - default unset
         * @return this
         */
        @NotNull HeaderButtonBuilder border(int width, @Nullable String color);

        /**
         * Button available duration
         *
         * @param duration time for duration
         * @return this
         */
        @NotNull HeaderButtonBuilder duration(int duration);

        /**
         * Specify HeaderButton only available for specific page
         *
         * @param page - page id
         * @return - this
         */
        @NotNull HeaderButtonBuilder availableForPage(@NotNull Class<? extends BaseEntity> page);

        @NotNull HeaderButtonBuilder clickAction(@NotNull Class<? extends SettingPluginButton> clickAction);

        @NotNull HeaderButtonBuilder clickAction(@NotNull Supplier<ActionResponseModel> clickAction);

        void build();
    }

    interface NotificationBlockBuilder {

        /**
         * Move to entity if click on header block title
         *
         * @param entity - entity to link to
         * @return this
         */
        @NotNull NotificationBlockBuilder linkToEntity(@NotNull BaseEntity entity);

        @NotNull NotificationBlockBuilder visibleForUser(@NotNull String email);

        @NotNull NotificationBlockBuilder blockActionBuilder(@NotNull Consumer<UIInputBuilder> builder);

        @NotNull NotificationBlockBuilder addFlexAction(@NotNull String key, @NotNull Consumer<UIFlexLayoutBuilder> builder);

        @NotNull NotificationBlockBuilder contextMenuActionBuilder(@NotNull Consumer<UIInputBuilder> builder);

        @NotNull NotificationBlockBuilder setNameColor(@Nullable String color);

        default @NotNull NotificationBlockBuilder setDevices(@Nullable Collection<? extends DeviceBaseEntity> devices) {
            if (devices != null) {
                addInfo("sum", new Icon("fas fa-mountain-city", "#CDDC39"), Lang.getServerMessage("TITLE.DEVICES_STAT",
                        FlowMap.of("ONLINE", devices.stream().filter(d -> d.getStatus().isOnline()).count(), "TOTAL", devices.size())));
                if (devices.isEmpty()) {
                    return this;
                }
                contextMenuActionBuilder(contextAction -> {
                    for (DeviceBaseEntity device : devices) {
                        String name = device instanceof DeviceEndpointsBehaviourContract
                                ? ((DeviceEndpointsBehaviourContract) device).getDeviceFullName() :
                                device.getTitle();
                        contextAction.addInfo(name)
                                .setColor(device.getStatus().getColor())
                                .setIcon(device.getEntityIcon())
                                .linkToEntity(device);
                    }
                });
            }
            return this;
        }

        /**
         * Specify whole notification block status and uses for getting border color
         *
         * @param status - block status
         * @return this
         */
        @NotNull NotificationBlockBuilder setStatus(@Nullable Status status);

        /**
         * Run handler on every user fetch url
         */
        @NotNull NotificationBlockBuilder fireOnFetch(@NotNull Runnable handler);

        /**
         * Set 'Update' button if firmware already installing or not
         *
         * @param value - true if need disable 'update' button
         * @return this
         */
        @NotNull NotificationBlockBuilder setUpdating(boolean value);

        /**
         * Specify custom border color. Default takes color from Status if present. If border and status not specified than fetch all rows from block and check
         * if it has status info. If all rows has ONLINE, ...
         *
         * @param color - color in hex format
         * @return this
         */
        @NotNull NotificationBlockBuilder setBorderColor(@Nullable String color);

        /**
         * Set notification block version
         *
         * @param version - version string
         * @return builder
         */
        @NotNull NotificationBlockBuilder setVersion(@Nullable String version);

        /**
         * Add updatable button to ui notification block.
         *
         * @param updateHandler - handler to execute when user press update button. Execution executes inside thread with passing progressBar object and
         *                      selected 'version'
         * @param versions      - list of versions to be able to select from UI select box
         * @return builder
         */
        @NotNull NotificationBlockBuilder setUpdatable(@NotNull BiFunction<ProgressBar, String, ActionResponseModel> updateHandler,
                                                       @NotNull List<String> versions);

        default @NotNull NotificationInfoLineBuilder addInfo(@NotNull String info, @Nullable Icon icon) {
            return addInfo(String.valueOf(info.hashCode()), icon, info);
        }

        default @NotNull NotificationBlockBuilder addErrorStatusInfo(@Nullable String message) {
            if (StringUtils.isNotEmpty(message)) {
                addInfo("status", new Icon("fas fa-exclamation"), message).setTextColor(Color.RED);
            }
            return this;
        }

        @NotNull NotificationInfoLineBuilder addInfo(@NotNull String key, @Nullable Icon icon, @NotNull String info);

        @NotNull NotificationBlockBuilder addEntityInfo(@NotNull BaseEntity entity);

        /**
         * Remove info row
         *
         * @param key - info key
         * @return this
         */
        boolean removeInfo(@NotNull String key);
    }

    interface NotificationInfoLineBuilder {

        @NotNull NotificationInfoLineBuilder setTextColor(@Nullable String color);

        @NotNull NotificationInfoLineBuilder setRightText(@NotNull String text, @Nullable Icon icon, @Nullable String color);

        /**
         * Specify info status. Calculates on UI when discover border color. Also fetch message if status message is not null and hover it on UI
         *
         * @param entity - entity
         * @return this
         */
        @NotNull NotificationInfoLineBuilder setStatus(@NotNull HasStatusAndMsg entity);

        @NotNull
        default NotificationInfoLineBuilder setRightText(@NotNull String text) {
            return setRightText(text, null, null);
        }

        @NotNull
        default NotificationInfoLineBuilder setRightText(Status status) {
            setRightText(status.name(), null, status.getColor());
            return this;
        }

        @NotNull NotificationInfoLineBuilder setRightButton(@Nullable Icon buttonIcon, @Nullable String buttonText,
                                                            @Nullable String confirmMessage, @Nullable UIActionHandler handler);

        @NotNull NotificationInfoLineBuilder setRightSettingsButton(@NotNull Icon buttonIcon, @NotNull Consumer<UILayoutBuilder> assembler);

        @NotNull
        default NotificationInfoLineBuilder setRightSettingsButton(@NotNull Consumer<UILayoutBuilder> assembler) {
            return setRightSettingsButton(new Icon("fas fa-ellipsis-vertical"), assembler);
        }

        @NotNull NotificationInfoLineBuilder setAsLink(@NotNull BaseEntity entity);
    }
}
