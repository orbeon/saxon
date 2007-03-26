package org.orbeon.saxon.style;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.PreparedStylesheet;
import org.orbeon.saxon.AugmentedSource;
import org.orbeon.saxon.event.IDFilter;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.tree.DocumentImpl;
import org.orbeon.saxon.tree.ElementImpl;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;


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

    public void prepareAttributes() throws XPathException {

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

    public void validate() throws XPathException {
        checkEmpty();
        checkTopLevel(isImport() ? "XTSE0190" : "XTSE0170");
    }

    public XSLStylesheet getIncludedStylesheet(XSLStylesheet importer, int precedence)
                 throws XPathException {

        if (href==null) {
            // error already reported
            return null;
        }

        checkEmpty();
        checkTopLevel((this instanceof XSLInclude ? "XTSE0170" : "XTSE0190"));

        try {
            XSLStylesheet thisSheet = (XSLStylesheet)getParent();
            PreparedStylesheet pss = getPreparedStylesheet();
            URIResolver resolver = pss.getURIResolver();
            Configuration config = pss.getConfiguration();

            //System.err.println("GeneralIncorporate: href=" + href + " base=" + getBaseURI());
            String relative = href;
            String fragment = null;
            int hash = relative.indexOf('#');
            if (hash == 0 || relative.length() == 0) {
                compileError("A stylesheet cannot " + getLocalPart() + " itself",
                                (this instanceof XSLInclude ? "XTSE0180" : "XTSE0210"));
                return null;
            } else if (hash == relative.length() - 1) {
                relative = relative.substring(0, hash);
            } else if (hash > 0) {
                if (hash+1 < relative.length()) {
                    fragment = relative.substring(hash+1);
                }
                relative = relative.substring(0, hash);
            }
            Source source;
            try {
                source = resolver.resolve(relative, getBaseURI());
            } catch (TransformerException e) {
                throw StaticError.makeStaticError(e);
            }

            // if a user URI resolver returns null, try the standard one
            // (Note, the standard URI resolver never returns null)
            if (source==null) {
                source = config.getSystemURIResolver().resolve(relative, getBaseURI());
            }

            if (fragment != null) {
                IDFilter filter = new IDFilter(fragment);
                source = AugmentedSource.makeAugmentedSource(source);
                ((AugmentedSource)source).addFilter(filter);
            }

            // check for recursion

            XSLStylesheet anc = thisSheet;

            if (source.getSystemId() != null) {
                while(anc!=null) {
                    if (source.getSystemId().equals(anc.getSystemId())) {
                        compileError("A stylesheet cannot " + getLocalPart() + " itself",
                                (this instanceof XSLInclude ? "XTSE0180" : "XTSE0210"));
                        return null;
                    }
                    anc = anc.getImporter();
                }
            }

            StyleNodeFactory snFactory = new StyleNodeFactory(config, getPreparedStylesheet().getErrorListener());
            includedDoc = PreparedStylesheet.loadStylesheetModule(source, config, getNamePool(), snFactory);

            // allow the included document to use "Literal Result Element as Stylesheet" syntax

            ElementImpl outermost = includedDoc.getDocumentElement();

            if (outermost instanceof LiteralResultElement) {
                includedDoc = ((LiteralResultElement)outermost)
                        .makeStylesheet(getPreparedStylesheet(), snFactory);
                outermost = includedDoc.getDocumentElement();
            }

            if (!(outermost instanceof XSLStylesheet)) {
                compileError("Included document " + href + " is not a stylesheet", "XTSE0165");
                return null;
            }
            XSLStylesheet incSheet = (XSLStylesheet)outermost;

            if (incSheet.validationError!=null) {
                if (reportingCircumstances == REPORT_ALWAYS) {
                    incSheet.compileError(incSheet.validationError);
                } else if (incSheet.reportingCircumstances == REPORT_UNLESS_FORWARDS_COMPATIBLE
                    // not sure if this can still happen
                              /*&& !incSheet.forwardsCompatibleModeIsEnabled()*/) {
                    incSheet.compileError(incSheet.validationError);
                }
            }

            incSheet.setPrecedence(precedence);
            incSheet.setImporter(importer);
            incSheet.spliceIncludes();          // resolve any nested includes;

            // Check the consistency of input-type-annotations
            thisSheet.setInputTypeAnnotations(incSheet.getInputTypeAnnotationsAttribute() |
                    incSheet.getInputTypeAnnotations());

            return incSheet;

        } catch (XPathException err) {
            err.setErrorCode("XTSE0165");
            compileError(err);
            return null;
        }
    }

    public Expression compile(Executable exec) throws XPathException {
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
