package net.sf.saxon.style;
import net.sf.saxon.Configuration;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.StandardURIResolver;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tree.DocumentImpl;
import net.sf.saxon.tree.ElementImpl;
import net.sf.saxon.xpath.DynamicError;
import org.w3c.dom.Node;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;


/**
* Abstract class to represent xsl:include or xsl:import element in the stylesheet. <br>
* The xsl:include and xsl:import elements have mandatory attribute href
*/

public abstract class XSLGeneralIncorporate extends StyleElement {

    String href;
    DocumentImpl includedDoc;

    /**
    * isImport() returns true if this is an xsl:import statement rather than an xsl:include
    */

    public abstract boolean isImport();

    public void prepareAttributes() throws TransformerConfigurationException {

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.HREF) {
        		href = atts.getValue(a).trim();
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (href==null) {
            reportAbsence("href");
        }
    }

    public void validate() throws TransformerConfigurationException {
        // The node will never be validated, because it replaces itself
        // by the contents of the included file.
        checkEmpty();
        checkTopLevel(null);
    }

    public XSLStylesheet getIncludedStylesheet(XSLStylesheet importer, int precedence)
                 throws TransformerConfigurationException {

        if (href==null) {
            // error already reported
            return null;
        }

        checkEmpty();
        checkTopLevel((this instanceof XSLInclude ? "XT0170" : "XT0190"));

        try {
            XSLStylesheet thisSheet = (XSLStylesheet)getParentNode();
            PreparedStylesheet pss = getPreparedStylesheet();
            Configuration config = pss.getConfiguration();

            // System.err.println("GeneralIncorporate: href=" + href + " base=" + getBaseURI());
            Source source = config.getURIResolver().resolve(href, getBaseURI());

            // if a user URI resolver returns null, try the standard one
            // (Note, the standard URI resolver never returns null)
            if (source==null) {
                source = (new StandardURIResolver(config)).resolve(href, getBaseURI());
            }

            if (source instanceof NodeInfo) {
                if (source instanceof Node) {
                    source = new DOMSource((Node)source);
                } else {
                    throw new DynamicError("URIResolver must not return a " + source.getClass());
                }
            }

            // check for recursion

            XSLStylesheet anc = thisSheet;

            if (source.getSystemId() != null) {
                while(anc!=null) {
                    if (source.getSystemId().equals(anc.getSystemId())) {
                        compileError("A stylesheet cannot " + getLocalPart() + " itself",
                                (this instanceof XSLInclude ? "XT0180" : "XT0210"));
                        return null;
                    }
                    anc = anc.getImporter();
                }
            }

            StyleNodeFactory snFactory = new StyleNodeFactory(getNamePool(),
                                        config.isAllowExternalFunctions());
            includedDoc = pss.loadStylesheetModule(source, config, getNamePool(), snFactory);

            // allow the included document to use "Literal Result Element as Stylesheet" syntax

            ElementImpl outermost = (ElementImpl)includedDoc.getDocumentElement();

            if (outermost instanceof LiteralResultElement) {
                includedDoc = ((LiteralResultElement)outermost)
                        .makeStylesheet(getPreparedStylesheet(), snFactory);
                outermost = (ElementImpl)includedDoc.getDocumentElement();
            }

            if (!(outermost instanceof XSLStylesheet)) {
                compileError("Included document " + href + " is not a stylesheet", "XT0165");
                return null;
            }
            XSLStylesheet incSheet = (XSLStylesheet)outermost;

            if (incSheet.validationError!=null) {
                if (reportingCircumstances == REPORT_ALWAYS) {
                    incSheet.compileError(incSheet.validationError);
                } else if (incSheet.reportingCircumstances == REPORT_UNLESS_FORWARDS_COMPATIBLE
                              && !incSheet.forwardsCompatibleModeIsEnabled()) {
                    incSheet.compileError(incSheet.validationError);
                }
            }

            incSheet.setPrecedence(precedence);
            incSheet.setImporter(importer);
            incSheet.spliceIncludes();          // resolve any nested includes;

            return incSheet;

        } catch (TransformerException err) {
            compileError(err);
            return null;
        }
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        return null;
        // no action. The node will never be compiled, because it replaces itself
        // by the contents of the included file.
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
