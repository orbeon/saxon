package net.sf.saxon.type;

import java.util.Iterator;

/**
 * This interface represents a collection of facets. It contains methods that
 * allow the facets for a simple type to be retrieved individually or collectively,
 * with or without the facets inherited from the base type. The concrete implementation
 * is in the schema-aware product.
 */
public interface FacetCollection {

    /**
     * Returns the first facet associated with the given name, defined either on this type
     * or on a type from which it is derived
     * @return the first facet associated with the given name
    **/

    Facet getFacet(String name);

    /**
     * Return all the facets having the given name
     * @param name the required facet name, for example "totalDigits" or "maxInclusive"
     * @return an iterator over all the facets with the given name, including those defined
     * on a type from which this is derived by restriction or by union. The iterator contains
     * objects of class {@link Facet}.
     */

    Iterator getFacets(String name);

    /**
     * Returns an Iterator over all the Facets (including inherited facets)
     *  for this type.
     * @return an Iterator over all the Facets for this type. The iterator contains
     * objects of class {@link Facet}.
    */

    Iterator getFacets();

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
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//