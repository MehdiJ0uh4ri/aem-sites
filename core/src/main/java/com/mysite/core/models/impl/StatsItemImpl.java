package com.mysite.core.models.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.mysite.core.models.StatsItem;

@Model(adaptables = Resource.class,
       adapters = StatsItem.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class StatsItemImpl implements StatsItem {

    /**
     * Splits an authored value into an optional prefix, a number and an optional suffix.
     * The number may use {@code ,} as a thousands separator and {@code .} as the decimal mark.
     * Values without any digit (for example {@code N/A}) simply do not match.
     */
    private static final Pattern VALUE_PATTERN =
            Pattern.compile("^([^0-9]*)([0-9][0-9,]*(?:\\.[0-9]+)?)(.*)$", Pattern.DOTALL);

    @ValueMapValue
    private String value;

    @ValueMapValue
    private String label;

    @ValueMapValue
    private String description;

    private Double number;
    private String prefix = StringUtils.EMPTY;
    private String suffix = StringUtils.EMPTY;

    @PostConstruct
    private void init() {
        if (StringUtils.isBlank(value)) {
            return;
        }
        Matcher matcher = VALUE_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            return;
        }
        prefix = matcher.group(1);
        suffix = matcher.group(3);
        try {
            number = Double.valueOf(matcher.group(2).replace(",", StringUtils.EMPTY));
        } catch (NumberFormatException e) {
            // A value too large for a double is still rendered, just never animated.
            number = null;
        }
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public Double getNumber() {
        return number;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public String getSuffix() {
        return suffix;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
