package net.sf.saxon.event;
import net.sf.saxon.om.XMLChar;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.trans.XPathException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
* CharacterMapExpander: This ProxyEmitter expands characters occurring in a character map,
 * as specified by the XSLT 2.0 xsl:character-map declaration
*
* @author Michael Kay
*/


public class CharacterMapExpander extends ProxyReceiver {

    private HashMap charMap;
    private int min = Integer.MAX_VALUE;    // the lowest mapped character
    private int max = 0;                    // the highest mapped character
    private boolean useNullMarkers = true;

    /**
     * Set the character maps to be used by this CharacterMapExpander.
     * They are merged into a single character map if there is more than one.
     */

    public void setCharacterMaps(List maps) {
            // merge the character maps, allowing definitions in a later map
            // to overwrite definitions in an earlier map. (Note, we don't really
            // need to do this if there is only one map, but we want to scan the keys
            // anyway to extract the mimimum and maximum mapped characters.)

        charMap = new HashMap(64);
        for (int i = 0; i < maps.size(); i++) {
            HashMap hashMap = (HashMap)maps.get(i);
            Iterator keys = hashMap.keySet().iterator();
            while (keys.hasNext()) {
                Integer next = (Integer)keys.next();
                int n = next.intValue();
                if (n < min) {
                    min = n;
                }
                if (n > max) {
                    max = n;
                }
                charMap.put(next, hashMap.get(next));
            }
        }
        if (min > 0xD800) {
            // if all the mapped characters are above the BMP, we need to check
            // surrogates
            min = 0xD800;
        }
    }

    /**
     * Indicate whether the result of character mapping should be marked using NUL
     * characters to prevent subsequent XML or HTML character escaping
     */

    public void setUseNullMarkers(boolean use) {
        useNullMarkers = use;
    }

    /**
     * Output an attribute
     */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
            throws XPathException {
        CharSequence mapped = map(value, useNullMarkers);
        if (mapped == value) {
            // no mapping was done
            super.attribute(nameCode, typeCode, value, locationId, properties);
        } else {
            super.attribute(nameCode, typeCode, mapped,
                    locationId, properties | ReceiverOptions.USE_NULL_MARKERS);
        }
    }

    /**
    * Output character data
    */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {

        if ((properties & ReceiverOptions.DISABLE_ESCAPING) == 0) {
            super.characters(map(chars, useNullMarkers), locationId, properties | ReceiverOptions.USE_NULL_MARKERS);
        } else {
            // if the user requests disable-output-escaping, this overrides the character
            // mapping
            super.characters(chars, locationId, properties);
        }

    }

    /**
     * Perform the character mappping
     * @param in the input string to be mapped
     * @param insertNulls true if null (0) characters are to be inserted before
     * and after replacement characters. This is done in attribute values to signal
     * that output escaping of these characters is disabled.
     */

    private CharSequence map(CharSequence in, boolean insertNulls) {

        // First scan the string to see if there are any possible mapped
        // characters; if not, don't bother creating the new buffer

        boolean move = false;
        for (int i=0; i<in.length();) {
            char c = in.charAt(i++);
            if (c >= min && c <= max) {
                move = true;
                break;
            }
        }
        if (!move) {
            return in;
        }

        FastStringBuffer buffer = new FastStringBuffer(in.length());
        int i = 0;
        while(i < in.length()) {
            char c = in.charAt(i++);
            if (c >= min && c <= max) {
                if (XMLChar.isHighSurrogate(c)) {
                    // assume the string is properly formed
                    char d = in.charAt(i++);
                    int s = XMLChar.supplemental(c, d);
                    String rep = (String)charMap.get(new Integer(s));
                    if (rep == null) {
                        buffer.append(c);
                        buffer.append(d);
                    } else {
                        if (insertNulls) {
                            buffer.append((char)0);
                            buffer.append(rep);
                            buffer.append((char)0);
                        } else {
                            buffer.append(rep);
                        }
                    }
                } else {
                    String rep = (String)charMap.get(new Integer(c));
                    if (rep == null) {
                        buffer.append(c);
                    } else {
                        if (insertNulls) {
                            buffer.append((char)0);
                            buffer.append(rep);
                            buffer.append((char)0);
                        } else {
                            buffer.append(rep);
                        }
                    }
                }
            } else {
                buffer.append(c);
            }
        }
        return buffer;
    }


};

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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

