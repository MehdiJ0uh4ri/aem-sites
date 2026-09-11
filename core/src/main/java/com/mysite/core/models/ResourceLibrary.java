package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Downloads / Resource Library.
 *
 * <p>Gated documents are never linked directly. The form posts to this component's own
 * {@code .download} servlet, which records the lead and then streams the file from a DAM
 * folder that is not publicly readable — so a visitor cannot skip the form by guessing the
 * asset URL, which is the failure mode of every gate built purely in the front end.</p>
 */
public interface ResourceLibrary extends Component {

    /** @return optional heading rendered above the list */
    String getHeading();

    /** @return the HTML element used for the heading, defaulting to {@code h2} */
    String getHeadingElement();

    /** @return introductory copy shown above the list, or {@code null} */
    String getDescription();

    /** @return the documents on offer, never {@code null} */
    List<ResourceEntry> getEntries();

    /** @return {@code true} when at least one document requires the lead form */
    boolean isAnyGated();

    /** @return the URL the gated download form posts to */
    String getFormAction();

    /**
     * @return the consent wording shown beside the marketing opt-in. Always populated,
     *         because an opt-in with no explanation of what is being consented to is not
     *         consent.
     */
    String getConsentText();

    /** @return {@code true} when there are no documents to render */
    boolean isEmpty();
}
