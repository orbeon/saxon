package org.orbeon.saxon.style;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Whitespace;

/**
* Handler for xsl:copy elements in stylesheet. <br>
*/

public class XSLCopy extends StyleElement {

    private String use;                     // value of use-attribute-sets attribute
    private AttributeSet[] attributeSets = null;
    private boolean copyNamespaces = true;
    private boolean inheritNamespaces = true;
    private int validationAction = Validation.PRESERVE;
    private SchemaType schemaType = null;

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

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();
		String copyNamespacesAtt = null;
		String validationAtt = null;
		String typeAtt = null;
        String inheritAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.USE_ATTRIBUTE_SETS)) {
        		use = atts.getValue(a);
            } else if (f.equals(StandardNames.COPY_NAMESPACES)) {
                copyNamespacesAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.TYPE)) {
                typeAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.VALIDATION)) {
                validationAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.INHERIT_NAMESPACES)) {
                inheritAtt = Whitespace.trim(atts.getValue(a));
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (copyNamespacesAtt == null) {
            copyNamespaces = true;
        } else {
            if (copyNamespacesAtt.equals("yes")) {
                copyNamespaces = true;
            } else if (copyNamespacesAtt.equals("no")) {
                copyNamespaces = false;
            } else {
                compileError("Value of copy-namespaces must be 'yes' or 'no'", "XTSE0020");
            }
        }

        if (typeAtt != null && validationAtt != null) {
            compileError("The type and validation attributes must not both be specified", "XTSE1505");
        }

        if (validationAtt != null) {
            validationAction = Validation.getCode(validationAtt);
            if (validationAction != Validation.STRIP && !getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError("To perform validation, a schema-aware XSLT processor is needed", "XTSE1660");
            }
        } else {
            validationAction = getContainingStylesheet().getDefaultValidation();
        }

        if (typeAtt != null) {
            schemaType = getSchemaType(typeAtt);
            if (!getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError("The @type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
            }
            validationAction = Validation.BY_TYPE;
        }
        if (inheritAtt != null) {
            if (inheritAtt.equals("yes")) {
                inheritNamespaces = true;
            } else if (inheritAtt.equals("no")) {
                inheritNamespaces = false;
            } else {
                compileError("The @inherit-namespaces attribute has permitted values (yes, no)", "XTSE0020");
            }
        }

//        if (validationAction == Validation.PRESERVE && !copyNamespaces) {
//            // this is an error only when copying namespace-sensitive content
//            compileError("copy-namespaces must be set to 'yes' when validation is set to 'preserve'", "XTTE0950");
//        }
    }

    public void validate() throws XPathException {
        if (use!=null) {
            attributeSets = getAttributeSets(use, null);         // find any referenced attribute sets
        }
    }

    public Expression compile(Executable exec) throws XPathException {
        Copy inst = new Copy(copyNamespaces,
                             inheritNamespaces,
                             schemaType,
                             validationAction);
        Expression content = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);

        if (attributeSets != null) {
            UseAttributeSets use = new UseAttributeSets(attributeSets);
            // The use-attribute-sets is ignored unless the context item is an element node. So we
            // wrap the UseAttributeSets instruction in a conditional to perform a run-time test
            Expression condition = new InstanceOfExpression(
                    new ContextItemExpression(),
                    SequenceType.makeSequenceType(NodeKindTest.ELEMENT, StaticProperty.EXACTLY_ONE));
            Expression choice = Choose.makeConditional(condition, use);
            if (content == null) {
                content = choice;
            } else {
                content = Block.makeBlock(choice, content);
                content.setLocationId(
                            allocateLocationId(getSystemId(), getLineNumber()));
            }
        }

        if (content == null) {
            content = Literal.makeEmptySequence();
        }
        inst.setContentExpression(content);
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
// Contributor(s): none.
//
