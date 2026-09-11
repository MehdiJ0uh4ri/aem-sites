package com.mysite.core.models.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.dam.cfm.ContentFragment;

import com.mysite.core.models.MjThesisItem;

/**
 * Plain value object for one numbered belief.
 */
class MjThesisItemImpl implements MjThesisItem {

    private final String ordinal;
    private final String title;
    private final String body;

    /**
     * Builds a belief from a Thesis content fragment.
     *
     * <p>The dialog multifield and the fragment carry the same three things under
     * different names, so the mapping lives here and the section never learns which
     * source it is rendering.</p>
     */
    static MjThesisItemImpl fromFragment(Resource resource, int index) {
        ContentFragment fragment = FragmentReader.of(resource);
        return new MjThesisItemImpl(
                FragmentReader.text(fragment, resource, "ordinal"),
                FragmentReader.text(fragment, resource, "title"),
                FragmentReader.text(fragment, resource, "rationale", "statement"),
                index);
    }

    private MjThesisItemImpl(String ordinal, String title, String body, int index) {
        this.ordinal = StringUtils.isNotBlank(ordinal) ? ordinal : String.format("%02d", index + 1);
        this.title = title;
        this.body = body;
    }

    MjThesisItemImpl(Resource resource, int index) {
        ValueMap properties = resource.getValueMap();
        String authored = properties.get("ordinal", String.class);
        // Authors should not have to renumber the list when they reorder it, so the
        // ordinal is derived unless one was deliberately typed in.
        this.ordinal = StringUtils.isNotBlank(authored) ? authored : String.format("%02d", index + 1);
        this.title = properties.get("title", String.class);
        this.body = properties.get("body", String.class);
    }

    boolean hasContent() {
        return StringUtils.isNotBlank(title) || StringUtils.isNotBlank(body);
    }

    @Override
    public String getOrdinal() {
        return ordinal;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getBody() {
        return body;
    }
}
