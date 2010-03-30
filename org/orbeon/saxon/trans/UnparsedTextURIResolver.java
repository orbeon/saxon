package org.orbeon.saxon.trans;

import org.orbeon.saxon.Configuration;

import java.io.Reader;
import java.io.Serializable;
import java.net.URI;

/**
 * An UnparsedTextURIResolver accepts an absolute URI and optionally an encoding name as input,
 * and returns a Reader as its result.
 */

public interface UnparsedTextURIResolver extends Serializable {

    /**
     * Resolve the URI passed to the XSLT unparsed-text() function, after resolving
     * against the base URI.
     *
     * <p>Note that a user-written resolver is responsible for enforcing some of the rules in the
     * XSLT specification, such as the rules for inferring an encoding when none is supplied. Saxon
     * will not report any error if the resolver does this in a non-conformant way.</p>
     *
     * @param absoluteURI the absolute URI obtained by resolving the supplied
     * URI against the base URI
     * @param encoding the encoding requested in the call of unparsed-text(), if any. Otherwise null.
     * @param config The Saxon configuration. Provided in case the URI resolver
     * needs it.
     * @return a Reader, which Saxon will use to read the unparsed text. After the text has been read,
     * the close() method of the Reader will be called.
     * @throws XPathException if any failure occurs
     * @since 8.9
     */

    public Reader resolve(URI absoluteURI, String encoding, Configuration config) throws XPathException;
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
