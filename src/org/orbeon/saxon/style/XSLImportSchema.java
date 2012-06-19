package org.orbeon.saxon.style;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.PreparedStylesheet;
import org.orbeon.saxon.value.Whitespace;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.type.SchemaException;

import javax.xml.transform.TransformerConfigurationException;


/**
* Compile-time representation of an xsl:import-schema declaration
 * in a stylesheet
*/

public class XSLImportSchema extends StyleElement {

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();
        String namespace = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
            if (f.equals(StandardNames.SCHEMA_LOCATION)) {
        		//
            } else if (f.equals(StandardNames.NAMESPACE)) {
                namespace = Whitespace.trim(atts.getValue(a));
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if ("".equals(namespace)) {
            compileError("The zero-length string is not a valid namespace URI. "+
                    "For a schema with no namespace, omit the namespace attribute");
        }
    }

    public void validate() throws XPathException {
        checkTopLevel(null);
    }

    public void readSchema() throws SchemaException, XPathException {
        try {
            String schemaLoc = Whitespace.trim(getAttributeValue(StandardNames.SCHEMA_LOCATION));
            String namespace = Whitespace.trim(getAttributeValue(StandardNames.NAMESPACE));
            if (namespace==null) {
                namespace = "";
            } else {
                namespace = namespace.trim();
            }
            PreparedStylesheet preparedStylesheet = getPreparedStylesheet();
            Configuration config = preparedStylesheet.getConfiguration();
            if (!config.isSchemaAware(Configuration.XSLT)) {
                compileError("To use xsl:import-schema, you need the schema-aware " +
                        "version of Saxon from http://www.saxonica.com/", "XTSE1650");
                return;
            }
            AxisIterator kids = iterateAxis(Axis.CHILD);
            NodeInfo inlineSchema = null;
            while (true) {
                Item child = kids.next();
                if (child==null) {
                    break;
                }
                if (inlineSchema != null) {
                    compileError(getDisplayName() + " must not have more than one child element");
                }
                inlineSchema = (NodeInfo)child;
                if (inlineSchema.getFingerprint() != StandardNames.XS_SCHEMA) {
                    compileError("The only child element permitted for " + getDisplayName() + " is xs:schema");
                }
                if (schemaLoc != null) {
                    compileError("The schema-location attribute must be absent if an inline schema is present");
                }

                namespace = config.readInlineSchema(inlineSchema, namespace, preparedStylesheet.getErrorListener());
                getPrincipalStylesheet().addImportedSchema(namespace);
            }
            if (inlineSchema != null) {
                return;
            }
            if (!config.isSchemaAvailable(namespace)) {
                if (schemaLoc == null) {
                    compileWarning("No schema for this namespace is known, " +
                            "and no schema-location was supplied, so no schema has been imported",
                            SaxonErrorCode.SXWN9006);
                    return;
                }
                PipelineConfiguration pipe = config.makePipelineConfiguration();
                namespace = config.readSchema(pipe, getBaseURI(), schemaLoc, namespace);
            }
            getPrincipalStylesheet().addImportedSchema(namespace);
        } catch (SchemaException err) {
            compileError(err.getMessage(), "XTSE0220");
        } catch (TransformerConfigurationException err) {
            compileError(err.getMessage(), "XTSE0220");
        }

    }


    public Expression compile(Executable exec) throws XPathException {
        exec.setReasonUnableToCompile("Cannot compile a stylesheet that imports a schema");
        return null;
        // No action. The effect of import-schema is compile-time only
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
