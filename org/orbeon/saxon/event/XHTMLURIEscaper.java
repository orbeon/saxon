package org.orbeon.saxon.event;

import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.codenorm.Normalizer;

import java.util.HashSet;


/**
 * This class performs URI escaping for the XHTML output method. The logic for performing escaping
 * is the same as the HTML output method, but the way in which attributes are identified for escaping
 * is different, because XHTML is case-sensitive.
 */

public class XHTMLURIEscaper extends HTMLURIEscaper {

    /**
    * Table of attributes whose value is a URL
    */

    private HashSet urlTable;

    private synchronized void buildURIAttributeTable() {
        // Reuse the attribute table for all XHTMLEmitters sharing the same namepool
        NamePool pool = getPipelineConfiguration().getConfiguration().getNamePool();
        urlTable = (HashSet)pool.getClientData(this.getClass());
        if (urlTable == null) {
            urlTable = new HashSet(40);
            pool.setClientData(this.getClass(), urlTable);
        }
        setUrlAttribute(pool, "form", "action");
        setUrlAttribute(pool, "object", "archive");
        setUrlAttribute(pool, "body", "background");
        setUrlAttribute(pool, "q", "cite");
        setUrlAttribute(pool, "blockquote", "cite");
        setUrlAttribute(pool, "del", "cite");
        setUrlAttribute(pool, "ins", "cite");
        setUrlAttribute(pool, "object", "classid");
        setUrlAttribute(pool, "object", "codebase");
        setUrlAttribute(pool, "applet", "codebase");
        setUrlAttribute(pool, "object", "data");
        setUrlAttribute(pool, "button", "datasrc");
        setUrlAttribute(pool, "div", "datasrc");
        setUrlAttribute(pool, "input", "datasrc");
        setUrlAttribute(pool, "object", "datasrc");
        setUrlAttribute(pool, "select", "datasrc");
        setUrlAttribute(pool, "span", "datasrc");
        setUrlAttribute(pool, "table", "datasrc");
        setUrlAttribute(pool, "textarea", "datasrc");
        setUrlAttribute(pool, "script", "for");
        setUrlAttribute(pool, "a", "href");
        setUrlAttribute(pool, "a", "name");       // see second note in section B.2.1 of HTML 4 specification
        setUrlAttribute(pool, "area", "href");
        setUrlAttribute(pool, "link", "href");
        setUrlAttribute(pool, "base", "href");
        setUrlAttribute(pool, "img", "longdesc");
        setUrlAttribute(pool, "frame", "longdesc");
        setUrlAttribute(pool, "iframe", "longdesc");
        setUrlAttribute(pool, "head", "profile");
        setUrlAttribute(pool, "script", "src");
        setUrlAttribute(pool, "input", "src");
        setUrlAttribute(pool, "frame", "src");
        setUrlAttribute(pool, "iframe", "src");
        setUrlAttribute(pool, "img", "src");
        setUrlAttribute(pool, "img", "usemap");
        setUrlAttribute(pool, "input", "usemap");
        setUrlAttribute(pool, "object", "usemap");
    }

    private void setUrlAttribute(NamePool pool, String element, String attribute) {
        int elcode = pool.allocate("", NamespaceConstant.XHTML, element) & NamePool.FP_MASK;
        int atcode = pool.allocate("", "", attribute) & NamePool.FP_MASK;
        Long key = new Long(((long)elcode)<<32 | (long)atcode);
        urlTable.add(key);
    }

    /**
     * Determine whether a given attribute is a URL attribute
     */

    private boolean isURLAttribute(int elcode, int atcode) {
        elcode = elcode & NamePool.FP_MASK;
        atcode = atcode & NamePool.FP_MASK;
        Long key = new Long(((long)elcode)<<32 | (long)atcode);
        return urlTable.contains(key);
    }

    /**
     * Do the real work of starting the document. This happens when the first
     * content is written.
     *
     * @throws org.orbeon.saxon.trans.XPathException
     *
     */

    public void open() throws XPathException {
        super.open();
        if (escapeURIAttributes) {
            buildURIAttributeTable();
        }
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     *
     * @param nameCode   The name of the attribute, as held in the name pool
     * @param typeCode   The type of the attribute, as held in the name pool
     * @param properties Bit significant value. The following bits are defined:
     *                   <dd>DISABLE_ESCAPING</dd>    <dt>Disable escaping for this attribute</dt>
     *                   <dd>NO_SPECIAL_CHARACTERS</dd>      <dt>Attribute value contains no special characters</dt>
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     *                                start tag
     */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties) throws XPathException {
        if (escapeURIAttributes &&
                isURLAttribute(currentElement, nameCode) &&
                (properties & ReceiverOptions.DISABLE_ESCAPING) == 0) {
            CharSequence normalized = new Normalizer(Normalizer.C).normalize(value);
            getUnderlyingReceiver().attribute(
                    nameCode, typeCode, HTMLURIEscaper.escapeURL(normalized, true), locationId,
                    properties | ReceiverOptions.DISABLE_CHARACTER_MAPS);
        } else {
            getUnderlyingReceiver().attribute(
                    nameCode, typeCode, value, locationId, properties);
        }
    }

}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
