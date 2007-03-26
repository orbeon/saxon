package org.orbeon.saxon.event;
import org.orbeon.saxon.charcode.UnicodeCharacterSet;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.codenorm.Normalizer;

import java.util.HashMap;

/**
  * This class is used as a filter on the serialization pipeline; it performs the function
  * of escaping URI-valued attributes in HTML
  * @author Michael H. Kay
  */

public class HTMLURIEscaper extends ProxyReceiver {

    /**
    * Table of attributes whose value is a URL
    */

    // we use two HashMaps to avoid unnecessary string concatenations

    private static HTMLTagHashSet urlAttributes = new HTMLTagHashSet(47);
    private static HTMLTagHashSet urlCombinations = new HTMLTagHashSet(101);

    static {
        setUrlAttribute("form", "action");
        setUrlAttribute("object", "archive");
        setUrlAttribute("body", "background");
        setUrlAttribute("q", "cite");
        setUrlAttribute("blockquote", "cite");
        setUrlAttribute("del", "cite");
        setUrlAttribute("ins", "cite");
        setUrlAttribute("object", "classid");
        setUrlAttribute("object", "codebase");
        setUrlAttribute("applet", "codebase");
        setUrlAttribute("object", "data");
        setUrlAttribute("button", "datasrc");
        setUrlAttribute("div", "datasrc");
        setUrlAttribute("input", "datasrc");
        setUrlAttribute("object", "datasrc");
        setUrlAttribute("select", "datasrc");
        setUrlAttribute("span", "datasrc");
        setUrlAttribute("table", "datasrc");
        setUrlAttribute("textarea", "datasrc");
        setUrlAttribute("script", "for");
        setUrlAttribute("a", "href");
        setUrlAttribute("a", "name");       // see second note in section B.2.1 of HTML 4 specification
        setUrlAttribute("area", "href");
        setUrlAttribute("link", "href");
        setUrlAttribute("base", "href");
        setUrlAttribute("img", "longdesc");
        setUrlAttribute("frame", "longdesc");
        setUrlAttribute("iframe", "longdesc");
        setUrlAttribute("head", "profile");
        setUrlAttribute("script", "src");
        setUrlAttribute("input", "src");
        setUrlAttribute("frame", "src");
        setUrlAttribute("iframe", "src");
        setUrlAttribute("img", "src");
        setUrlAttribute("img", "usemap");
        setUrlAttribute("input", "usemap");
        setUrlAttribute("object", "usemap");
    }

    private static void setUrlAttribute(String element, String attribute) {
        urlAttributes.add(attribute);
        urlCombinations.add(element + '+' + attribute);
    }

    private HashMap urlAttributeCache = new HashMap(30);

    public boolean isUrlAttribute(int element, int attribute) {
        Long key = new Long(((long)element)<<32 | (long)attribute);
        Boolean result = (Boolean)urlAttributeCache.get(key);
        if (result != null) {
            return result.booleanValue();
        }
        if (pool == null) {
            pool = getNamePool();
        }
        String attributeName = pool.getDisplayName(attribute);
        if (!urlAttributes.contains(attributeName)) {
            urlAttributeCache.put(key, Boolean.FALSE);
            return false;
        }
        String elementName = pool.getDisplayName(element);
        boolean b = urlCombinations.contains(elementName + '+' + attributeName);
        urlAttributeCache.put(key, Boolean.valueOf(b));
        return b;
    }

    protected int currentElement;
    protected boolean escapeURIAttributes = true;
    protected NamePool pool;

     /**
     * Start of a document node.
     */

    public void startDocument(int properties) throws XPathException {
        nextReceiver.startDocument(properties);
        pool = getPipelineConfiguration().getConfiguration().getNamePool();
    }

    /**
     * Notify the start of an element
     *
     * @param nameCode   integer code identifying the name of the element within the name pool.
     * @param typeCode   integer code identifying the element's type within the name pool.
     * @param properties properties of the element node
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        currentElement = nameCode;
        nextReceiver.startElement(nameCode, typeCode, locationId, properties);
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
               isUrlAttribute(currentElement, nameCode) &&  
               (properties & ReceiverOptions.DISABLE_ESCAPING) == 0) {
            nextReceiver.attribute(nameCode, typeCode, escapeURL(value, true), locationId,
                    properties | ReceiverOptions.DISABLE_CHARACTER_MAPS);
        } else {
            nextReceiver.attribute(nameCode, typeCode, value, locationId, properties);
        }
    }

    /**
     * Escape a URI according to the HTML rules: that is, a non-ASCII character (specifically,
     * a character outside the range 32 - 126) is replaced by the %HH encoding of the octets in
     * its UTF-8 representation
     * @param url the URI to be escaped
     * @param normalize
     * @return the URI after escaping non-ASCII characters
     */

    public static CharSequence escapeURL(CharSequence url, boolean normalize) {
        // optimize for the common case where the string is all ASCII characters
        for (int i=url.length()-1; i>=0; i--) {
            char ch = url.charAt(i);
            if (ch<32 || ch>126) {
                if (normalize) {
                    CharSequence normalized = new Normalizer(Normalizer.C).normalize(url);
                    return reallyEscapeURL(normalized);
                } else {
                    return reallyEscapeURL(url);
                }
            }
        }
        return url;
    }

    private static CharSequence reallyEscapeURL(CharSequence url) {
        FastStringBuffer sb = new FastStringBuffer(url.length() + 20);
        final String hex = "0123456789ABCDEF";
        byte[] array = new byte[4];

        for (int i=0; i<url.length(); i++) {
            char ch = url.charAt(i);
            if (ch<32 || ch>126) {
                int used = UnicodeCharacterSet.getUTF8Encoding(ch,
                                                 (i+1 < url.length() ? url.charAt(i+1): ' '), array);
                for (int b=0; b<used; b++) {
                    //int v = (array[b]>=0 ? array[b] : 256 + array[b]);
                    int v = ((int)array[b]) & 0xff;
                    sb.append('%');
                    sb.append(hex.charAt(v/16));
                    sb.append(hex.charAt(v%16));
                }

            } else {
                sb.append(ch);
            }
        }
        return sb;
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
