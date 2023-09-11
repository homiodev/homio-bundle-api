package org.homio.api.ui.field.selection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * File selection
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UIFieldTreeNodeSelection {

    // is allowed to edit with keyboard
    boolean rawInput() default true;

    // prefix to show on UI and distinguish if multiple select inputs available
    String prefix() default "";

    String icon() default "fas fa-folder-open";

    String iconColor() default "";

    String IMAGE_PATTERN = ".*(jpg|jpeg|png|gif)";

    String LOCAL_FS = "LOCAL_FS";

    /**
     * @return If set - uses only local file system, otherwise uses all possible file systems
     */
    String rootPath() default "";

    boolean allowMultiSelect() default false;

    boolean allowSelectDirs() default false;

    boolean allowSelectFiles() default true;

    String pattern() default ".*";

    /**
     * Specify select file/folder dialog title
     *
     * @return - dialog title
     */
    String dialogTitle() default "";

    /**
     * @return Specify file systems ids. All available if not specified
     */
    String[] fileSystemIds() default {UIFieldTreeNodeSelection.LOCAL_FS};
}
