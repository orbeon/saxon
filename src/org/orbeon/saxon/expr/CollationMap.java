package org.orbeon.saxon.expr;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.sort.CodepointCollator;
import org.orbeon.saxon.sort.StringCollator;

import java.io.Serializable;
import java.util.HashMap;

/**
 * This object maps collation URIs to collations. Logically this function is part of the static
 * context, but it is often needed dynamically, so it is defined as a separate component that can
 * safely be retained at run-time.
 */
public class CollationMap implements Serializable {

    private Configuration config;
    private String defaultCollationName;
    private HashMap map;

    /**
     * Create a collation map
     * @param config the Saxon configuration
     */

    public CollationMap(Configuration config) {
        this.config = config;
        this.defaultCollationName = NamespaceConstant.CODEPOINT_COLLATION_URI;
    }

    /**
     * Create a copy of a collation map
     * @param in the collation map to be copied
     */

    public CollationMap(CollationMap in) {
        if (in.map != null) {
            map = new HashMap(in.map);
        }
        config = in.config;
        defaultCollationName = in.defaultCollationName;
    }

    /**
     * Set the name of the default collation
     * @param name the default collation name (should be a URI, but this is not enforced)
     */

    public void setDefaultCollationName(String name) {
        defaultCollationName = name;
    }

    /**
     * Get the name of the default collation
     * @return the default collation name (should be a URI, but this is not enforced)
     */

    public String getDefaultCollationName() {
        return defaultCollationName;
    }

    /**
     * Get the default collation
     * @return the default collation, as a StringCollator
     */

    public StringCollator getDefaultCollation() {
        return getNamedCollation(defaultCollationName);
    }

    /**
     * Register a named collation
     * @param absoluteURI the name of the collation. This should be an absolute URI, but
     * this is not enforced
     * @param collator the StringCollator that implements the collating rules
     */

    public void setNamedCollation(String absoluteURI, StringCollator collator) {
        if (map == null) {
            map = new HashMap();
        }
        map.put(absoluteURI, collator);
    }

    /**
     * Get the collation with a given collation name. If the collation name has
     * not been registered in this CollationMap, the CollationURIResolver registered
     * with the Configuration is called. If this cannot resolve the collation name,
     * it should return null.
     * @param name the collation name (should be an absolute URI)
     * @return the StringCollator with this name if known, or null if not known
     */

    public StringCollator getNamedCollation(String name) {
        if (name.equals(NamespaceConstant.CODEPOINT_COLLATION_URI)) {
            return CodepointCollator.getInstance();
        }
        if (map != null) {
            StringCollator c = (StringCollator)map.get(name);
            if (c != null) {
                return c;
            }
        }
        return config.getCollationURIResolver().resolve(name, null, config);
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

