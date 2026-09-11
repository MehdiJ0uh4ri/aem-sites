package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Thesis / Point of View section: a numbered set of beliefs about markets
 * and investing, set as a reading column with the ordinals hung in the margin.
 *
 * <p>The beliefs come from one of two places. Authored straight into the dialog they are
 * page copy, which is right for a statement that belongs to one page. Pointed at a folder
 * of Thesis fragments they become structured content that sectors, portfolio companies and
 * insights can reference back — the same section either way, because how a belief is
 * stored is not a reason to render it differently.</p>
 */
public interface MjThesis extends Component {

    String getEyebrow();

    String getHeading();

    List<MjThesisItem> getItems();

    /**
     * @return the element this section's heading is written with, {@code h1}..{@code h4}.
     *         Authorable; left on Auto the opener of the page takes the {@code h1}.
     */
    String getHeadingTag();

    boolean isEmpty();
}
