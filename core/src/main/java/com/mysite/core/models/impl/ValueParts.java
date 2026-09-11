package com.mysite.core.models.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Splits an authored figure such as {@code $1.2M}, {@code 98%} or {@code 12,400 kg} into an
 * optional prefix, a number and an optional suffix.
 *
 * <p>Authors type figures the way they should read, decoration and all. Both the count-up
 * animation and the spec table's numeric sort need the bare number out of that string, so the
 * split happens once on the server and the result travels to the client as data attributes.
 * Values carrying no digit at all — {@code N/A}, {@code Classified} — parse to a {@code null}
 * number and are rendered verbatim.</p>
 */
final class ValueParts {

    /**
     * The number may use {@code ,} as a thousands separator and {@code .} as the decimal mark.
     * DOTALL so a value that happens to contain a newline still matches rather than silently
     * losing its suffix.
     */
    private static final Pattern PATTERN =
            Pattern.compile("^([^0-9]*)([0-9][0-9,]*(?:\\.[0-9]+)?)(.*)$", Pattern.DOTALL);

    private final String prefix;
    private final String suffix;
    private final Double number;

    private ValueParts(String prefix, Double number, String suffix) {
        this.prefix = prefix;
        this.number = number;
        this.suffix = suffix;
    }

    /**
     * @param value the authored figure, may be {@code null} or blank
     * @return the split, never {@code null}; {@link #getNumber()} is {@code null} when the
     *         value holds no parsable number
     */
    static ValueParts of(String value) {
        if (StringUtils.isBlank(value)) {
            return new ValueParts(StringUtils.EMPTY, null, StringUtils.EMPTY);
        }
        Matcher matcher = PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            return new ValueParts(StringUtils.EMPTY, null, StringUtils.EMPTY);
        }
        Double parsed;
        try {
            parsed = Double.valueOf(matcher.group(2).replace(",", StringUtils.EMPTY));
        } catch (NumberFormatException e) {
            // A value too large for a double is still rendered, just never animated or sorted.
            parsed = null;
        }
        return new ValueParts(matcher.group(1), parsed, matcher.group(3));
    }

    /** @return the characters before the number, for example {@code $}; never {@code null} */
    String getPrefix() {
        return prefix;
    }

    /** @return the characters after the number, for example {@code %}; never {@code null} */
    String getSuffix() {
        return suffix;
    }

    /** @return the bare number, or {@code null} when the value holds none */
    Double getNumber() {
        return number;
    }
}
