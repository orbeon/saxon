package org.orbeon.saxon.trans;

import javax.xml.transform.URIResolver;
import javax.xml.transform.ErrorListener;

/**
 * This class exists to hold information associated with a specific XSLT compilation episode.
 * In JAXP, the URIResolver and ErrorListener used during XSLT compilation are those defined in the
 * TransformerFactory. The .NET API, however, allows finer granularity, and this class exists to
 * support that.
 */

public class CompilerInfo {

    private URIResolver uriResolver;

    private ErrorListener errorListener;
    private boolean compileWithTracing;

    /**
     * Set the URI Resolver to be used in this compilation episode.
     * @param resolver The URIResolver to be used. This is used to dereference URIs encountered in constructs
     * such as xsl:include, xsl:import, and xsl:import-schema.
     * @since 8.7
     */

    public void setURIResolver(URIResolver resolver) {
        uriResolver = resolver;
    }

    /**
     * Get the URI Resolver being used in this compilation episode.
     * @return resolver The URIResolver in use. This is used to dereference URIs encountered in constructs
     * such as xsl:include, xsl:import, and xsl:import-schema.
     * @since 8.7
     */

    public URIResolver getURIResolver() {
        return uriResolver;
    }

    /**
     * Set the ErrorListener to be used during this compilation episode
     * @param listener The error listener to be used. This is notified of all errors detected during the
     * compilation.
     * @since 8.7
     */

    public void setErrorListener(ErrorListener listener) {
        errorListener = listener;
    }

    /**
     * Get the ErrorListener being used during this compilation episode
     * @return listener The error listener in use. This is notified of all errors detected during the
     * compilation.
     * @since 8.7
     */

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    /**
     * Set whether trace hooks are to be included in the compiled code. To use tracing, it is necessary
     * both to compile the code with trace hooks included, and to supply a TraceListener at run-time
     * @param trueOrFalse true if trace code is to be compiled in, false otherwise
     * @since 8.9
     */

    public void setCompileWithTracing(boolean trueOrFalse) {
        compileWithTracing = trueOrFalse;
    }

    /**
     * Determine whether trace hooks are included in the compiled code.
     * @return true if trace hooks are included, false if not.
     * @since 8.9
     */

    public boolean isCompileWithTracing() {
        return compileWithTracing;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
