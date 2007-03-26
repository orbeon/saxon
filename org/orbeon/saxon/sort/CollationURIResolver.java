package org.orbeon.saxon.sort;

import org.orbeon.saxon.Configuration;

import java.util.Comparator;
import java.io.Serializable;

/**
 * A CollationURIResolver accepts a collation name as input, and returns
 * a collation (represented by a Comparator) as output. A CollationURIResolver
 * can be registered with the Configuration (or with the TransformerFactory)
 * to resolve all collation URIs used in a stylesheet or query.
 */
public interface CollationURIResolver extends Serializable {

    /**
     * Resolve a collation URI (expressed as a string) and return
     * the corresponding collation.
     * @param relativeURI the collation URI as written in the query or stylesheet
     * @param baseURI The base URI of the static context where the collation URI
     * appears. The base URI is available only in cases where the collation URI is resolved
     * at compile time; in cases where the collation URI is not resolved until execution
     * time (typically because it is supplied as an expression rather than as a string literal)
     * this parameter is currently set to null.
     * @param config The configuration. Provided in case the collation URI resolver
     * needs it.
     * @return a Comparator, representing the collation to be used. Note that although
     * any Comparator may be returned, functions such as contains() that need to break
     * a string into its collation units will work only if the returned Comparator
     * is a {@link java.text.Collator}. If the Collation URI cannot be resolved, return null.
     * Note that unlike the JAXP URIResolver, returning null does not cause the default
     * CollationURIResolver to be invoked; if this is required, the user-written CollationURIResolver
     * should explicitly instantiate and invoke the {@link StandardCollationURIResolver} before
     * returning null.
     * @since 8.5 (this interface is new in Saxon 8.5 and may be revised in the light of
     * experience)
     */

    public Comparator resolve(String relativeURI, String baseURI, Configuration config);
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
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
