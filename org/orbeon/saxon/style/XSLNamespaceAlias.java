package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NamespaceException;

import javax.xml.transform.TransformerConfigurationException;


/**
* An xsl:namespace-alias element in the stylesheet. <br>
*/

public class XSLNamespaceAlias extends StyleElement {

    private short stylesheetURICode;
    private int resultNamespaceCode;

    public void prepareAttributes() throws TransformerConfigurationException {

	    String stylesheetPrefix=null;
	    String resultPrefix=null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.STYLESHEET_PREFIX) {
        		stylesheetPrefix = atts.getValue(a).trim();
        	} else if (f==StandardNames.RESULT_PREFIX) {
        		resultPrefix = atts.getValue(a).trim();
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }
        if (stylesheetPrefix==null) {
            reportAbsence("stylesheet-prefix");
            return;
        }
        if (stylesheetPrefix.equals("#default")) {
            stylesheetPrefix="";
        }
        if (resultPrefix==null) {
            reportAbsence("result-prefix");
            return;
        }
        if (resultPrefix.equals("#default")) {
            resultPrefix="";
        }
        try {
            stylesheetURICode = getURICodeForPrefix(stylesheetPrefix);
            NamePool pool = getNamePool();
            resultNamespaceCode = pool.getNamespaceCode(
                                            resultPrefix,
                                            getURIForPrefix(resultPrefix, true));
        } catch (NamespaceException err) {
            compileError(err.getMessage());
        }
    }

    public void validate() throws TransformerConfigurationException {
        checkTopLevel(null);
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        return null;
    }

    public short getStylesheetURICode() {
        return stylesheetURICode;
    }

    public int getResultNamespaceCode() {
        return resultNamespaceCode;
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
