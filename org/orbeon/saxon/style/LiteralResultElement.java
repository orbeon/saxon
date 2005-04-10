package net.sf.saxon.style;
import net.sf.saxon.Configuration;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.instruct.*;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.tree.DocumentImpl;
import net.sf.saxon.tree.TreeBuilder;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.StringValue;

import javax.xml.transform.TransformerException;


/**
* This class represents a literal result element in the style sheet
* (typically an HTML element to be output). <br>
* It is also used to represent unknown top-level elements, which are ignored.
*/

public class LiteralResultElement extends StyleElement {

    private int resultNameCode;
    private int[] attributeNames;
    private Expression[] attributeValues;
    private boolean[] attributeChecked;
    private int numberOfAttributes;
    private boolean toplevel;
    private int[] namespaceCodes;
    private AttributeSet[] attributeSets;
    private SchemaType schemaType = null;
    private int validation = Validation.STRIP;
    private boolean inheritNamespaces = true;

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    /**
     * Specify that this is an instruction
     */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Process the attribute list
    */

    public void prepareAttributes() throws XPathException {

        // Process the values of all attributes. At this stage we deal with attribute
        // values (especially AVTs), but we do not apply namespace aliasing to the
        // attribute names.

        int num = attributeList.getLength();

        if (num == 0) {
            numberOfAttributes = 0;
        } else {
            NamePool namePool = getNamePool();
            attributeNames = new int[num];
            attributeValues = new Expression[num];
            attributeChecked = new boolean[num];
            numberOfAttributes = 0;

            for (int i=0; i<num; i++) {

                int anameCode = attributeList.getNameCode(i);
                short attURIcode = namePool.getURICode(anameCode);

                if (attURIcode==NamespaceConstant.XSLT_CODE) {
                    int fp = anameCode & 0xfffff;

                    if (fp == StandardNames.XSL_USE_ATTRIBUTE_SETS) {
                        // deal with this later
                    } else if (fp == StandardNames.XSL_DEFAULT_COLLATION) {
                    	// already dealt with
                    } else if (fp == StandardNames.XSL_EXTENSION_ELEMENT_PREFIXES) {
                    	// already dealt with
                    } else if (fp == StandardNames.XSL_EXCLUDE_RESULT_PREFIXES) {
                    	// already dealt with
                    } else if (fp == StandardNames.XSL_VERSION) {
                        // already dealt with
                    } else if (fp == StandardNames.XSL_XPATH_DEFAULT_NAMESPACE) {
                        // already dealt with
                    } else if (fp == StandardNames.XSL_TYPE) {
                        // deal with this later
                    } else if (fp == StandardNames.XSL_USE_WHEN) {
                        // already dealt with
                    } else if (fp == StandardNames.XSL_VALIDATION) {
                        // deal with this later
                    } else if (fp == StandardNames.XSL_INHERIT_NAMESPACES) {
                        String inheritAtt = attributeList.getValue(i);
                        if (inheritAtt.equals("yes")) {
                            inheritNamespaces = true;
                        } else if (inheritAtt.equals("no")) {
                            inheritNamespaces = false;
                        } else {
                            compileError("The xsl:inherit-namespaces attribute has permitted values (yes, no)", "XTSE0020");
                        }
                    } else {
                        compileError("Unknown XSL attribute " + namePool.getDisplayName(anameCode), "XTSE0010");
                    }
                } else {
                    attributeNames[numberOfAttributes] = anameCode;
                    Expression exp = makeAttributeValueTemplate(attributeList.getValue(i));
                    attributeValues[numberOfAttributes] = exp;

                    // if we can be sure the attribute value contains no special XML/HTML characters,
                    // we can save the trouble of checking it each time it is output.
                    // Note that the check includes characters that need to be escaped in a URL if the
                    // output method turns out to be HTML (we don't know the method at compile time).

                    attributeChecked[numberOfAttributes] = false;
                    boolean special = false;
                    if (exp instanceof StringValue) {
                        CharSequence val = ((StringValue)exp).getStringValueCS();
                        for (int k=0; k<val.length(); k++) {
                            char c = val.charAt(k);
                            if ((int)c<33 || (int)c>126 ||
                                     c=='<' || c=='>' || c=='&' || c=='\"') {
                                special = true;
                                break;
                             }
                        }
                        attributeChecked[numberOfAttributes] = !special;
                    }
                    numberOfAttributes++;
                }
            }

            // now shorten the arrays if necessary. This is necessary if there are [xsl:]-prefixed
            // attributes that weren't copied into the arrays.

            if (numberOfAttributes < attributeNames.length) {

                int[] attributeNames2 = new int[numberOfAttributes];
                System.arraycopy(attributeNames, 0, attributeNames2, 0, numberOfAttributes);
                attributeNames = attributeNames2;

                Expression[] attributeValues2 = new Expression[numberOfAttributes];
                System.arraycopy(attributeValues, 0, attributeValues2, 0, numberOfAttributes);
                attributeValues = attributeValues2;

                boolean[] attributeChecked2 = new boolean[numberOfAttributes];
                System.arraycopy(attributeChecked, 0, attributeChecked2, 0, numberOfAttributes);
                attributeChecked = attributeChecked2;
            }
        }
    }

    /**
    * Validate that this node is OK
    */

    public void validate() throws XPathException {

        toplevel = (getParent() instanceof XSLStylesheet);

        resultNameCode = getNameCode();

        NamePool namePool = getNamePool();
        short elementURICode = namePool.getURICode(resultNameCode);

        if (toplevel) {
            // A top-level element can never be a "real" literal result element,
            // but this class gets used for unknown elements found at the top level

            if (elementURICode == 0) {
                compileError("Top level elements must have a non-null namespace URI", "XTSE0010");
            }
        } else {

            // Build the list of output namespace nodes

            // Up to 5.3.1 we listed the namespace nodes associated with this element that were not also
            // associated with an ancestor literal result element (because those will already
            // have been output). Unfortunately this isn't true if the namespace was present on an outer
            // LRE, and was excluded at that level using exclude-result-prefixes, and is now used in an
            // inner element: bug 5.3.1/006

            // We now use a different optimisation: if
            // (a) this LRE has a parent that is also an LRE, and
            // (b) this LRE has no namespace declarations of its own, and
            // (c) this element name is in the same namespace as its parent, and
            // (d) the parent doesn't specify xsl:inherit-namespaces="no"
            // (e) there are no attributes in a non-null namespace,
            // then we don't need to output any namespace declarations to the result.

            boolean optimizeNS = false;
            NodeInfo parent = getParent();
            if ((parent instanceof LiteralResultElement) &&
                    ((LiteralResultElement)parent).inheritNamespaces &&
                    (namespaceList==null || namespaceList.length==0) &&
                    ( elementURICode == namePool.getURICode(getParent().getFingerprint()))) {
                optimizeNS = true;
            }
            if (optimizeNS) {
                for (int a=0; a<attributeList.getLength(); a++ ) {
                    if (((attributeList.getNameCode(a)>>20)&0xff) != 0) {	// prefix != ""
                        optimizeNS = false;
                        break;
                    }
                }
            }

            if (optimizeNS) {
            	namespaceCodes = NodeInfo.EMPTY_NAMESPACE_LIST;
            } else {
                namespaceCodes = getInScopeNamespaceCodes();
	        }

            // apply any aliases required to create the list of output namespaces

            XSLStylesheet sheet = getPrincipalStylesheet();

            if (sheet.hasNamespaceAliases()) {
                for (int i=0; i<namespaceCodes.length; i++) {
                	// System.err.println("Examining namespace " + namespaceCodes[i]);
                	short scode = (short)(namespaceCodes[i]&0xffff);
                    int ncode = sheet.getNamespaceAlias(scode);
                    if (ncode != -1 && (ncode & 0xffff) != scode) {
                        // apply the namespace alias. Change in 7.3: use the prefix associated
                        // with the new namespace, not the old prefix.
                        namespaceCodes[i] = ncode;
                    }
                }

                // determine if there is an alias for the namespace of the element name

                int ercode = sheet.getNamespaceAlias(elementURICode);
                if ((ercode & 0xffff) != elementURICode) {
                    resultNameCode = namePool.allocate(namePool.getPrefixFromNamespaceCode(ercode),
                                                       namePool.getURIFromNamespaceCode(ercode),
                                                       getLocalPart());
                }
            }

            // deal with special attributes

            String useAttSets = getAttributeValue(StandardNames.XSL_USE_ATTRIBUTE_SETS);
            if (useAttSets != null) {
                attributeSets = getAttributeSets(useAttSets, null);
            }

            String type = getAttributeValue(StandardNames.XSL_TYPE);
            if (type != null) {
                if (!getConfiguration().isSchemaAware(Configuration.XSLT)) {
                    compileError("The xsl:type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
                }
                schemaType = getSchemaType(type);
            }

            String validate = getAttributeValue(StandardNames.XSL_VALIDATION);
            if (validate != null) {
                validation = Validation.getCode(validate);
                if (validation != Validation.STRIP && !getConfiguration().isSchemaAware(Configuration.XSLT)) {
                    compileError("To perform validation, a schema-aware XSLT processor is needed", "XTSE1660");
                }
                if (validation == Validation.INVALID) {
                    compileError("Invalid value for xsl:validation. " +
                                 "Permitted values are (strict, lax, preserve, strip)", "XTSE0020");
                }
            } else {
                validation = getContainingStylesheet().getDefaultValidation();
            }

            // establish the names to be used for all the output attributes;
            // also type-check the AVT expressions

            short attributeURIs[] = new short[numberOfAttributes];
            if (numberOfAttributes > 0) {

                for (int i=0; i<numberOfAttributes; i++) {

                    int anameCode = attributeNames[i];
                    int alias = anameCode;
                    short attURIcode = namePool.getURICode(anameCode);

                    if (attURIcode!=0) {	// attribute has a namespace prefix
                        int newNSCode = sheet.getNamespaceAlias(attURIcode);
                        if ((newNSCode & 0xffff) != attURIcode) {
                	        attURIcode = (short)(newNSCode & 0xffff);
                            alias = namePool.allocate( namePool.getPrefixFromNamespaceCode(newNSCode),
                                                       namePool.getURIFromNamespaceCode(newNSCode),
                                                       attributeList.getLocalName(i));
                        }
                    }

    	            //attributeNames[i] = translate(alias);
                    attributeNames[i] = alias;
    	            attributeURIs[i] = attURIcode;
  	                attributeValues[i] = typeCheck(namePool.getDisplayName(alias), attributeValues[i]);
                }
            }

            // remove any namespaces that are on the exclude-result-prefixes list, unless it is
            // the namespace of the element or an attribute

            int numberExcluded = 0;
            for (int n=0; n<namespaceCodes.length; n++) {
            	short uricode = (short)(namespaceCodes[n] & 0xffff);
                if (isExcludedNamespace(uricode) && !sheet.isAliasResultNamespace(uricode)) {
                    // exclude it from the output namespace list
                    namespaceCodes[n] = -1;
                    numberExcluded++;
                }
            }

            // Now translate the list of namespace codes to use the target namepool,
            // getting rid of the -1 entries while we go.

//            int count = namespaceCodes.length - numberExcluded;
//            NamePool oldPool = getNamePool();
//            NamePool newPool = getTargetNamePool();
//            // these must now be the same name pool?
//            int[] newNamespaceCodes = new int[count];
//            count = 0;
//            for (int i=0; i<namespaceCodes.length; i++) {
//                if (namespaceCodes[i] != -1) {
//                    String prefix = oldPool.getPrefixFromNamespaceCode(namespaceCodes[i]);
//                    String uri = oldPool.getURIFromNamespaceCode(namespaceCodes[i]);
//                    newNamespaceCodes[count++] = newPool.allocateNamespaceCode(prefix, uri);
//                }
//            }
//            namespaceCodes = newNamespaceCodes;

            int count = namespaceCodes.length - numberExcluded;
            int[] newNamespaceCodes = new int[count];
            count = 0;
            for (int i=0; i<namespaceCodes.length; i++) {
                if (namespaceCodes[i] != -1) {
                    newNamespaceCodes[count++] = namespaceCodes[i];
                }
            }
            namespaceCodes = newNamespaceCodes;
        }
    }

    /**
    * Validate the children of this node, recursively. Overridden for top-level
    * data elements.
    */

    protected void validateChildren() throws XPathException {
        if (!toplevel) {
            super.validateChildren();
        }
    }

    /**
    * Translate a namecode in the stylesheet namepool to a namecode in the target namepool
    */

//    private int translate(int oldNameCode) {
//        NamePool oldPool = getNamePool();
//        NamePool newPool = getTargetNamePool();
//        String prefix = oldPool.getPrefix(oldNameCode);
//        String uri = oldPool.getURI(oldNameCode);
//        String localName = oldPool.getLocalName(oldNameCode);
//        return newPool.allocate(prefix, uri, localName);
//    }

	/**
	* Process the literal result element by copying it to the result tree
	*/

    public Expression compile(Executable exec) throws XPathException {
        // top level elements in the stylesheet are ignored
        if (toplevel) return null;

        FixedElement inst = new FixedElement(
                        resultNameCode,
                        namespaceCodes,
                inheritNamespaces,
                        schemaType,
                        validation);

        Expression content = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);

        if (numberOfAttributes > 0) {
            for (int i=attributeNames.length - 1; i>=0; i--) {
                FixedAttribute att = new FixedAttribute(
                        attributeNames[i],
                        Validation.STRIP,
                        null,
                        -1);
                try {
                    att.setSelect(attributeValues[i]);
                } catch (XPathException err) {
                    compileError(err);
                }
                att.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
                att.setParentExpression(inst);
                ExpressionTool.makeParentReferences(att);
                if (attributeChecked[i]) {
                    att.setNoSpecialChars();
                }
                if (content == null) {
                    content = att;
                } else {
                    content = Block.makeBlock(att, content);
                }
            }
        }

        if (attributeSets != null) {
            UseAttributeSets use = new UseAttributeSets(attributeSets);
            if (content == null) {
                content = use;
            } else {
                content = Block.makeBlock(use, content);
            }
        }

        if (content == null) {
            content = EmptySequence.getInstance();
        }
        inst.setContentExpression(content);

        ExpressionTool.makeParentReferences(inst);
        return inst;
    }

    /**
    * Make a top-level literal result element into a stylesheet. This implements
    * the "Literal Result Element As Stylesheet" facility.
    */

    public DocumentImpl makeStylesheet(PreparedStylesheet pss,
                                       StyleNodeFactory nodeFactory)
            throws XPathException {

        // the implementation grafts the LRE node onto a containing xsl:template and
        // xsl:stylesheet

		NamePool pool = getNamePool();
        String xslPrefix = getPrefixForURI(NamespaceConstant.XSLT);
        if (xslPrefix==null) {
            String message;
            if (getLocalPart().equals("stylesheet") || getLocalPart().equals("transform")) {
                if (getPrefixForURI(NamespaceConstant.MICROSOFT_XSL)!=null) {
                    message = "Saxon is not able to process Microsoft's WD-xsl dialect";
                } else {
                    message = "Namespace for stylesheet element should be " + NamespaceConstant.XSLT;
                }
            } else {
                message = "The supplied file does not appear to be a stylesheet";
            }
            StaticError err = new StaticError (message);
            err.setLocator(this);
            err.setErrorCode(SaxonErrorCode.SXIN0004);
            try {
                pss.reportError(err);
            } catch (TransformerException err2) {}
            throw err;

        }

        // check there is an xsl:version attribute (it's mandatory), and copy
        // it to the new xsl:stylesheet element

        String version = getAttributeValue(StandardNames.XSL_VERSION);
        if (version==null) {
            StaticError err = new StaticError (
                "Simplified stylesheet: xsl:version attribute is missing");
            err.setErrorCode("XTSE0150");
            err.setLocator(this);
            try {
                pss.reportError(err);
            } catch(TransformerException err2) {}
            throw err;
        }

        try {
            TreeBuilder builder = new TreeBuilder();
            builder.setPipelineConfiguration(pss.getConfiguration().makePipelineConfiguration());
            builder.setNodeFactory(nodeFactory);
            builder.setSystemId(this.getSystemId());

            builder.open();
            builder.startDocument(0);

            int st = StandardNames.XSL_STYLESHEET;
            builder.startElement(st, -1, 0, 0);
            builder.namespace(NamespaceConstant.XSLT_CODE, 0);
            builder.attribute(pool.allocate("", "", "version"), -1, version, 0, 0);
            builder.startContent();

            int te = StandardNames.XSL_TEMPLATE;
            builder.startElement(te, -1, 0, 0);
            builder.attribute(pool.allocate("", "", "match"), -1, "/", 0, 0);
            builder.startContent();

            builder.graftElement(this);

            builder.endElement();
            builder.endElement();
            builder.endDocument();
            builder.close();

            return (DocumentImpl)builder.getCurrentRoot();
        } catch (XPathException err) {
            //TransformerConfigurationException e = new TransformerConfigurationException(err);
            err.setLocator(this);
            throw err;
        }

    }

    /**
     * Get the type of construct. This will be a constant in
     * class {@link net.sf.saxon.trace.Location}. This method is part of the
     * {@link net.sf.saxon.trace.InstructionInfo} interface
     */

    public int getConstructType() {
        return Location.LITERAL_RESULT_ELEMENT;
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     * If there is no name, the value will be -1.
     */

    public int getObjectNameCode() {
        return resultNameCode;
    }

    /**
     * Get the value of a particular property of the instruction. This is part of the
     * {@link net.sf.saxon.trace.InstructionInfo} interface for run-time tracing and debugging. The properties
     * available include all the attributes of the source instruction (named by the attribute name):
     * these are all provided as string values.
     * @param name The name of the required property
     * @return  The value of the requested property, or null if the property is not available
     */

    public Object getProperty(String name) {
        if (name.equals("name")) {
            return getDisplayName();
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
