package com.mysite.core.models.impl;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;

/**
 * Reads the child nodes of a composite Granite multifield into a list of Sling Models.
 *
 * <p>Every component in this project that owns a repeatable list does the same four things:
 * tolerate a missing multifield node, adapt each child, drop the entries an author left
 * blank, and hand back an immutable list. This collects that into one place.</p>
 */
final class MultifieldUtil {

    /** Sentinel for {@link #readItems(Resource, Class, Predicate, int)} meaning "no cap". */
    static final int UNLIMITED = -1;

    private MultifieldUtil() {
        // static helper
    }

    /**
     * @param parent the multifield node, may be {@code null} when nothing was authored
     * @param type   the model interface each child is adapted to
     * @param keep   which adapted items survive; entries that fail to adapt are always dropped
     * @param <T>    the item model type
     * @return an immutable list of items, never {@code null}
     */
    static <T> List<T> readItems(Resource parent, Class<T> type, Predicate<T> keep) {
        return readItems(parent, type, keep, UNLIMITED);
    }

    /**
     * @param limit the maximum number of items to return, or {@link #UNLIMITED}. Used where a
     *              layout has a fixed number of slots and extra entries would break it rather
     *              than wrap.
     * @see #readItems(Resource, Class, Predicate)
     */
    static <T> List<T> readItems(Resource parent, Class<T> type, Predicate<T> keep, int limit) {
        if (parent == null) {
            return Collections.emptyList();
        }
        Stream<T> items = StreamSupport.stream(parent.getChildren().spliterator(), false)
                .map(child -> child.adaptTo(type))
                .filter(Objects::nonNull)
                .filter(keep);
        if (limit >= 0) {
            items = items.limit(limit);
        }
        return items.collect(Collectors.toUnmodifiableList());
    }
}
