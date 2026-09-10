package com.mysite.core.models;

/**
 * One numbered belief in the {@link MjThesis} statement.
 */
public interface MjThesisItem {

    /** @return the ordinal printed in the margin, e.g. {@code 01} */
    String getOrdinal();

    String getTitle();

    String getBody();
}
