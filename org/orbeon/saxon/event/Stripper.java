package net.sf.saxon.event;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.ExpressionLocation;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.Mode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.ComplexType;

/**
  * The Stripper class maintains details of which elements need to be stripped.
  * The code is written to act as a SAX-like filter to do the stripping.
  * @author Michael H. Kay
  */


public class Stripper extends ProxyReceiver {

    private boolean preserveAll;              // true if all elements have whitespace preserved
    private boolean stripAll;                 // true if all whitespace nodes are stripped

    // stripStack is used to hold information used while stripping nodes. We avoid allocating
    // space on the tree itself to keep the size of nodes down. Each entry on the stack is two
    // booleans, one indicates the current value of xml-space is "preserve", the other indicates
    // that we are in a space-preserving element.

    // We implement our own stack to avoid the overhead of allocating objects. The two booleans
    // are held as the ls bits of a byte.

    private byte[] stripStack = new byte[100];
    private int top = 0;

	// We use a collection of rules to determine whether to strip spaces; a collection
	// of rules is known as a Mode. (We are reusing the code for template rule matching)

	private Mode stripperMode;

    //private PipelineConfiguration pipe;

	// Mode expects to test an Element, so we create a dummy element for it to test
	private Orphan element;

	// Stripper needs a context (a) for evaluating patterns
	// and (b) to provide reporting of rule conflicts.
	private Controller controller;
    private XPathContext context;

	// Need the namePool to get URI codes from name codes
	private NamePool namePool;

	// Namecode for xml:space attribute
	private int xmlSpaceCode;

    /**
    * Default constructor for use in subclasses
    */

    protected Stripper() {}

    /**
    * create a Stripper and initialise variables
    * @param stripperRules defines which elements have whitespace stripped. If
    * null, all whitespace is preserved.
    */

    public Stripper(Mode stripperRules) {
        stripperMode = stripperRules;
        preserveAll = (stripperRules==null);
        stripAll = false;
    }

    /**
     * Get a clean copy of this stripper
     */

    public Stripper getAnother() {
        Stripper clone = new Stripper(stripperMode);
        clone.setController(controller);
        clone.stripAll = stripAll;
        clone.preserveAll = preserveAll;
        return clone;
    }


    /**
    * Specify that all whitespace nodes are to be stripped
    */

    public void setStripAll() {
        preserveAll = false;
        stripAll = true;
    }

    /**
    * Determine if all whitespace is to be stripped (in this case, no further testing
    * is needed)
    */

    public boolean getStripAll() {
    	return stripAll;
    }


	/**
	* Set the Controller to be used
	*/

	public void setController(Controller controller) {
		this.controller = controller;
        context = controller.newXPathContext();
		namePool = controller.getNamePool();
		xmlSpaceCode = namePool.allocate("xml", NamespaceConstant.XML, "space");
        element = new Orphan(controller.getConfiguration());
        element.setNodeKind(Type.ELEMENT);
	}

    /**
    * Decide whether an element is in the set of white-space preserving element types
    * @param nameCode Identifies the name of the element whose whitespace is to
     * be preserved
     * @return ALWAYS_PRESERVE if the element is in the set of white-space preserving
     *  element types, ALWAYS_STRIP if the element is to be stripped regardless of the
     * xml:space setting, and STRIP_DEFAULT otherwise
    */



    public byte isSpacePreserving(int nameCode) {
    	try {
	    	if (preserveAll) return ALWAYS_PRESERVE;
	    	if (stripAll) return STRIP_DEFAULT;
	    	element.setNameCode(nameCode);
	    	Object rule = stripperMode.getRule(element, context);
	    	if (rule==null) return ALWAYS_PRESERVE;
	    	return (((Boolean)rule).booleanValue() ? ALWAYS_PRESERVE : STRIP_DEFAULT);
	    } catch (XPathException err) {
	    	return ALWAYS_PRESERVE;
	    }
    }

    public static final byte ALWAYS_PRESERVE = 0x01;
    public static final byte ALWAYS_STRIP = 0x02;
    public static final byte STRIP_DEFAULT = 0x00;
    public static final byte PRESERVE_PARENT = 0x04;
    public static final byte CANNOT_STRIP = 0x08;

    /**
    * Decide whether an element is in the set of white-space preserving element types.
     * This version of the method is useful in cases where getting the namecode of the
     * element is potentially expensive, e.g. with DOM nodes.
     * @param element Identifies the element whose whitespace is possibly to
     * be preserved
     * @return ALWAYS_PRESERVE if the element is in the set of white-space preserving
     *  element types, ALWAYS_STRIP if the element is to be stripped regardless of the
     * xml:space setting, and STRIP_DEFAULT otherwise
    */

    public byte isSpacePreserving(NodeInfo element) {
    	try {
	    	if (preserveAll) return ALWAYS_PRESERVE;
	    	if (stripAll) return STRIP_DEFAULT;
	    	Object rule = stripperMode.getRule(element, context);
	    	if (rule==null) return ALWAYS_PRESERVE;
	    	return (((Boolean)rule).booleanValue() ? ALWAYS_PRESERVE : STRIP_DEFAULT);
	    } catch (XPathException err) {
	    	return ALWAYS_PRESERVE;
	    }
    }


    /**
    * Callback interface for SAX: not for application use
    */

    public void open () throws XPathException {
        // System.err.println("Stripper#startDocument()");
        top = 0;
        stripStack[top] = ALWAYS_PRESERVE;             // {xml:preserve = false, preserve this element = true}
        super.open();
    }

    public void startElement (int nameCode, int typeCode, int locationId, int properties) throws XPathException
    {
    	// System.err.println("startElement " + nameCode);
        super.startElement(nameCode, typeCode, locationId, properties);

        byte preserveParent = stripStack[top];
        byte preserve = (byte)(preserveParent & PRESERVE_PARENT);

        if (isSpacePreserving(nameCode) == ALWAYS_PRESERVE) {
            preserve |= ALWAYS_PRESERVE;
        }
        if (preserve == 0 && typeCode != -1) {
            // if the element has simple content, whitespace stripping is not allowed (error XT0275)
            SchemaType type = controller.getConfiguration().getSchemaType(typeCode);
            if (type.isSimpleType() || ((ComplexType)type).isSimpleContent()) {
                preserve |= CANNOT_STRIP;
            }
        }

        // put "preserve" value on top of stack

        top++;
        if (top >= stripStack.length) {
            byte[] newStack = new byte[top*2];
            System.arraycopy(stripStack, 0, newStack, 0, top);
            stripStack = newStack;
        }
        stripStack[top] = preserve;
    }

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {

        // test for xml:space="preserve" | "default"

        if (nameCode == xmlSpaceCode) {
            if (value.toString().equals("preserve")) {
                stripStack[top] |= PRESERVE_PARENT;
            } else {
                stripStack[top] &= ~PRESERVE_PARENT;
            }
        }
        super.attribute(nameCode, typeCode, value, locationId, properties);
    }

    /**
    * Handle an end-of-element event
    */

    public void endElement () throws XPathException
    {
        super.endElement();
        top--;
    }

    /**
    * Handle a text node
    */

    public void characters (CharSequence chars, int locationId, int properties) throws XPathException
    {
        // assume adjacent chunks of text are already concatenated

        if (chars.length() > 0) {
            if (stripStack[top]!=0 || !Navigator.isWhite(chars)) {
                if ((stripStack[top] & (ALWAYS_STRIP | CANNOT_STRIP)) == 0) {
                    super.characters(chars, locationId, properties);

                } else if ((stripStack[top] & CANNOT_STRIP) != 0) {
                    DynamicError err = new DynamicError(
                                    "Cannot apply strip-space to a schema-defined element with simple content");
                    err.setErrorCode("XT0275");
                    err.setLocator(new ExpressionLocation(
                            getPipelineConfiguration().getLocationProvider(), locationId));
                    controller.recoverableError(err);
                }
            }
        }
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
