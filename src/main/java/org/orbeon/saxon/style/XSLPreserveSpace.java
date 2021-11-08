package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.QNameException;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.pattern.*;
import org.orbeon.saxon.trans.Mode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;

import java.util.StringTokenizer;

/**
* An xsl:preserve-space or xsl:strip-space elements in stylesheet. <br>
*/

public class XSLPreserveSpace extends StyleElement {

    private String elements;

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.ELEMENTS) {
        		elements = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }
        if (elements==null) {
            reportAbsence("elements");
            elements="*";   // for error recovery
        }
    }

    public void validate() throws XPathException {
        checkEmpty();
        checkTopLevel(null);
    }

    public Expression compile(Executable exec) throws XPathException
    {
        Boolean preserve = Boolean.valueOf(getFingerprint() == StandardNames.XSL_PRESERVE_SPACE);
        Mode stripperRules = getPrincipalStylesheet().getStripperRules();

        // elements is a space-separated list of element names

        StringTokenizer st = new StringTokenizer(elements, " \t\n\r", false);
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            NodeTestPattern pat = new NodeTestPattern();
            // following information is used in conflict warnings
            pat.setOriginalText(s);
            pat.setSystemId(getSystemId());
            pat.setLineNumber(getLineNumber());
            NodeTest nt;
            if (s.equals("*")) {
                nt = AnyNodeTest.getInstance();
                pat.setNodeTest(nt);
                stripperRules.addRule(
                            pat,
                            preserve,
                            getPrecedence(),
                            -0.5, true);

            } else if (s.endsWith(":*")) {
                if (s.length()==2) {
                    compileError("No prefix before ':*'");
                }
                String prefix = s.substring(0, s.length()-2);
                String uri = getURIForPrefix(prefix, false);
                nt = new NamespaceTest(
                                    getNamePool(),
                                    Type.ELEMENT,
                                    uri);
                pat.setNodeTest(nt);
                stripperRules.addRule(
                            pat,
                            preserve,
                            getPrecedence(),
                            -0.25, true);
            } else if (s.startsWith("*:")) {
                if (s.length()==2) {
                    compileError("No local name after '*:'");
                }
                String localname = s.substring(2);
                nt = new LocalNameTest(
                                    getNamePool(),
                                    Type.ELEMENT,
                                    localname);
                pat.setNodeTest(nt);
                stripperRules.addRule(
                            pat,
                            preserve,
                            getPrecedence(),
                            -0.25, true);
            } else {
                String prefix;
                String localName = null;
                String uri = null;
                try {
                    String[] parts = getConfiguration().getNameChecker().getQNameParts(s);
                    prefix = parts[0];
                    if (parts[0].equals("")) {
                        uri = getDefaultXPathNamespace();
                    } else {
                        uri = getURIForPrefix(prefix, false);
                        if (uri == null) {
                            undeclaredNamespaceError(prefix, "XTSE0280");
                            return null;
                        }
                    }
                    localName = parts[1];
                } catch (QNameException err) {
                    compileError("Element name " + s + " is not a valid QName", "XTSE0280");
                    return null;
                }
                NamePool target = getNamePool();
                int nameCode = target.allocate("", uri, localName);
                nt = new NameTest(Type.ELEMENT, nameCode, getNamePool());
                pat.setNodeTest(nt);
                stripperRules.addRule(
                            pat,
                            preserve,
                            getPrecedence(),
                            0, true);
            }

        }
        return null;
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
