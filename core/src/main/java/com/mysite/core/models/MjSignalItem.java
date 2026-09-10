package com.mysite.core.models;

/**
 * One entry in the {@link MjSignal} ledger: a figure with its label and a short note.
 */
public interface MjSignalItem {

    /** @return the figure, set at display size (e.g. {@code $410M}) */
    String getValue();

    /** @return what the figure counts */
    String getLabel();

    /** @return optional qualifier, e.g. {@code committed since 2019} */
    String getNote();
}
