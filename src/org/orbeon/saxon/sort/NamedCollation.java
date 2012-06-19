package org.orbeon.saxon.sort;

import org.orbeon.saxon.Platform;
import org.orbeon.saxon.Configuration;

import java.util.Comparator;

/**
 * A StringCollator is used for comparing strings (Java String objects).
 * The URI is retained along with the collation so that the collation can
 * be reconstructed on demand, typically at run-time by compiled code which has access to the
 * URI but not the collation object itself.
 */

public class NamedCollation implements StringCollator {

    private String uri;
    private transient Comparator collation;  // the collation is not serialized, but is reconstituted on demand
    private static Platform platform = Configuration.getPlatform();

    /**
     * Create a NamedCollation
     * @param uri the name of the collation
     * @param collation the Comparator that does the actual string comparison
     */

    public NamedCollation(String uri, Comparator collation) {
        this.uri = uri;
        this.collation = collation;
    }

     /**
     * Compares its two arguments for order.  Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.<p>
     * <p/>

     * @param o1 the first object to be compared.
     * @param o2 the second object to be compared.
     * @return a negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than the
     *         second.
     * @throws ClassCastException if the arguments' types prevent them from
     *                            being compared by this Comparator.
     */
    public int compareStrings(String o1, String o2) {
        return collation.compare(o1, o2);
    }

    /**
     * Get the URI identifying the collation
     */

    public String getUri() {
        return uri;
    }

    /**
     * Set the URI identifying the collation
     * @param uri the collation URI
     */

    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Get the underlying comparator
     * @return the underlying comparator
     */

    public Comparator getCollation() {
        return collation;
    }

    /**
     * Set the underlying comparator
     * @param collation the underlying comparator
     */

    public void setCollation(Comparator collation) {
        this.collation = collation;
    }

    /**
     * Get a collation key for two Strings. The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * compare correctly under the equals() method.
     */

    public Object getCollationKey(String s) {
        return platform.getCollationKey(this, s);
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

