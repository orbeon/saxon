package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.ResultDocument;
import org.orbeon.saxon.om.NamespaceException;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.tree.AttributeCollection;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.Configuration;

import javax.xml.transform.TransformerConfigurationException;
import java.util.*;

/**
* An xsl:result-document (formerly saxon:output) element in the stylesheet. <BR>
* The xsl:result-document element takes an attribute href="filename". The filename will
* often contain parameters, e.g. {position()} to ensure that a different file is produced
* for each element instance. <BR>
* There is a further attribute "name" which determines the format of the
* output file, it identifies the name of an xsl:output element containing the output
* format details.
*/

public class XSLResultDocument extends StyleElement {

    private final static HashSet fans = new HashSet(25);    // formatting attribute names

    static {
        fans.add(StandardNames.METHOD);
        fans.add(StandardNames.OUTPUT_VERSION);
        fans.add(StandardNames.INDENT);
        fans.add(StandardNames.ENCODING);
        fans.add(StandardNames.MEDIA_TYPE);
        fans.add(StandardNames.DOCTYPE_SYSTEM);
        fans.add(StandardNames.DOCTYPE_PUBLIC);
        fans.add(StandardNames.OMIT_XML_DECLARATION);
        fans.add(StandardNames.STANDALONE);
        fans.add(StandardNames.CDATA_SECTION_ELEMENTS);
        fans.add(StandardNames.INCLUDE_CONTENT_TYPE);
        fans.add(StandardNames.ESCAPE_URI_ATTRIBUTES);
        fans.add(StandardNames.UNDECLARE_NAMESPACES);
        //fans.add(StandardNames.USE_CHARACTER_MAPS);
        fans.add(StandardNames.SAXON_NEXT_IN_CHAIN);
        fans.add(StandardNames.SAXON_CHARACTER_REPRESENTATION);
        fans.add(StandardNames.SAXON_INDENT_SPACES);
        fans.add(StandardNames.SAXON_BYTE_ORDER_MARK);
        fans.add(StandardNames.SAXON_REQUIRE_WELL_FORMED);
    }

    private Expression href;
    private int format = -1;     // fingerprint of required xsl:output element
    private int validationAction = Validation.STRIP;
    private SchemaType schemaType = null;
    private HashMap serializationAttributes = new HashMap();

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

   /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction). Default implementation returns Type.ITEM, indicating
     * that we don't know, it might be anything. Returns null in the case of an element
     * such as xsl:sort or xsl:variable that can appear in a sequence constructor but
     * contributes nothing to the result sequence.
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return null;
    }

    public void prepareAttributes() throws TransformerConfigurationException {
		AttributeCollection atts = getAttributeList();

        String formatAttribute = null;
        String hrefAttribute = null;
        String validationAtt = null;
        String typeAtt = null;


		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.FORMAT) {
        		formatAttribute = atts.getValue(a).trim();
        	} else if (f==StandardNames.HREF) {
        		hrefAttribute = atts.getValue(a).trim();
            } else if (f==StandardNames.VALIDATION) {
                validationAtt = atts.getValue(a).trim();
            } else if (f==StandardNames.TYPE) {
                typeAtt = atts.getValue(a).trim();
            } else if (fans.contains(f) || !(f.startsWith("{}"))) {
                // this is a serialization attribute
                String val = atts.getValue(a).trim();
                Expression exp = makeAttributeValueTemplate(val);
                serializationAttributes.put(new Integer(nc&0xfffff), exp);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (hrefAttribute==null) {
            //href = StringValue.EMPTY_STRING;
        } else {
            href = makeAttributeValueTemplate(hrefAttribute);
        }

        if (formatAttribute!=null) {
            try {
                format = makeNameCode(formatAttribute.trim()) & 0xfffff;
            } catch (NamespaceException err) {
                compileError(err.getMessage());
            } catch (XPathException err) {
                compileError(err.getMessage());
            }
        }

        if (validationAtt==null) {
            validationAction = getContainingStylesheet().getDefaultValidation();
        } else {
            validationAction = Validation.getCode(validationAtt);
            if (validationAction != Validation.STRIP && !getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError("To perform validation, a schema-aware XSLT processor is needed");
            }
            if (validationAction == Validation.INVALID) {
                compileError("Invalid value of validation attribute");
            }
        }
        if (typeAtt!=null) {
            if (!getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError("The type attribute is available only with a schema-aware XSLT processor");
            }
            schemaType = getSchemaType(typeAtt);
        }

        if (typeAtt != null && validationAtt != null) {
            compileError("validation and type attributes are mutually exclusive");
        }
    }

    public void validate() throws TransformerConfigurationException {
        checkWithinTemplate();
        if (!getPreparedStylesheet().getConfiguration().isAllowExternalFunctions()) {
            compileError("xsl:result-document is disabled when extension functions are disabled");
        }
        href = typeCheck("href", href);
        Iterator it = serializationAttributes.keySet().iterator();
        while (it.hasNext()) {
            Integer fp = (Integer)it.next();
            final Expression exp1 = (Expression)serializationAttributes.get(fp);
            final Expression exp2 = typeCheck(getNamePool().getDisplayName(fp.intValue()), exp1);
            if (exp1 != exp2) {
                serializationAttributes.put(fp, exp2);
            }
        }
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        Properties props = null;
        try {
            props = getPrincipalStylesheet().gatherOutputProperties(format);
        } catch (TransformerConfigurationException err) {
            compileError("Named output format has not been defined");
            return null;
        }

        ArrayList fixed = new ArrayList();
        boolean needsNamespaceContext = false;
        for (Iterator it=serializationAttributes.keySet().iterator(); it.hasNext();) {
            Integer fp = (Integer)it.next();
            Expression exp = (Expression)serializationAttributes.get(fp);
            if (exp instanceof StringValue) {
                String s = ((StringValue)exp).getStringValue();
                try {
                    ResultDocument.setSerializationProperty(props, fp.intValue(), s,
                            getNamePool(), getStaticContext().getNamespaceResolver());
                    fixed.add(fp);
                } catch (XPathException e) {
                    compileError(e);
                }
            } else {
                String lname = getNamePool().getLocalName(fp.intValue());
                if (lname.equals("method") || lname.equals("cdata-section-elements")) {
                    needsNamespaceContext = true;
                }
            }
        }
        for (Iterator it=fixed.iterator(); it.hasNext();) {
            serializationAttributes.remove(it.next());
        }

        ResultDocument inst = new ResultDocument(props,
                                              href,
                                              getBaseURI(),
                                              validationAction,
                                              schemaType,
                                              serializationAttributes,
                                              (needsNamespaceContext ? getStaticContext().getNamespaceResolver() : null));

        compileChildren(exec, inst, true);
        ExpressionTool.makeParentReferences(inst);
        return inst;
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
// Additional Contributor(s): Brett Knights [brett@knightsofthenet.com]
//
