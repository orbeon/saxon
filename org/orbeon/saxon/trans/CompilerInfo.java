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

    public void setURIResolver(URIResolver resolver) {
        this.uriResolver = resolver;
    }

    public URIResolver getURIResolver() {
        return uriResolver;
    }

    public void setErrorListener(ErrorListener listener) {
        this.errorListener = listener;
    }

    public ErrorListener getErrorListener() {
        return errorListener;
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
