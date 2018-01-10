package io.rhizomatic.kernel.spi.util;

import org.jetbrains.annotations.Nullable;

/**
 * Utility for casting.
 */
public class Cast {

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public static <T> T cast(@Nullable Object list) {
        return (T) list;
    }

    private Cast() {
    }
}

