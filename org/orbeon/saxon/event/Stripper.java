package org.orbeon.saxon.event;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.Orphan;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.Mode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.Rule;
import org.orbeon.saxon.type.ComplexType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.Whitespace;

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

	// Mode expects to test an Element, so we create a dummy element for it to test
	private Orphan element;

	// Stripper needs a context (a) for evaluating patterns
	// and (b) to provide reporting of rule conflicts.
    private XPathContext context;

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
        clone.setPipelineConfiguration(getPipelineConfiguration());
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

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        super.setPipelineConfiguration(pipe);
        if (context == null) {
            Controller controller = pipe.getController();
            if (controller != null) {
                context = controller.newXPathContext();
            }
        }
        if (element == null) {
            element = new Orphan(pipe.getConfiguration());
            element.setNodeKind(Type.ELEMENT);
        }
    }

    /**
    * Decide whether an element is in the set of white-space preserving element types
    * @param nameCode Identifies the name of the element whose whitespace is to
     * be preserved
     * @return ALWAYS_PRESERVE if the element is in the set of white-space preserving
     *  element types, ALWAYS_STRIP if the element is to be stripped regardless of the
     * xml:space setting, and STRIP_DEFAULT otherwise
    */



    public byte isSpacePreserving(int nameCode) throws XPathException {
    	//try {
	    	if (preserveAll) return ALWAYS_PRESERVE;
	    	if (stripAll) return STRIP_DEFAULT;
	    	element.setNameCode(nameCode);
	    	Rule rule = stripperMode.getRule(element, context);
	    	if (rule==null) return ALWAYS_PRESERVE;
	    	return (((Boolean)rule.getAction()).booleanValue() ? ALWAYS_PRESERVE : STRIP_DEFAULT);
//	    } catch (XPathException err) {
//	    	return ALWAYS_PRESERVE;
//	    }
    }

    public static final byte ALWAYS_PRESERVE = 0x01;    // whitespace always preserved (e.g. xsl:text)
    public static final byte ALWAYS_STRIP = 0x02;       // whitespace always stripped (e.g. xsl:choose)
    public static final byte STRIP_DEFAULT = 0x00;      // no special action
    public static final byte PRESERVE_PARENT = 0x04;    // parent element specifies xml:space="preserve"
    public static final byte CANNOT_STRIP = 0x08;       // type annotation indicates simple typed content

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

    public byte isSpacePreserving(NodeInfo element) throws XPathException {
//    	try {
	    	if (preserveAll) return ALWAYS_PRESERVE;
	    	if (stripAll) return STRIP_DEFAULT;
	    	Rule rule = stripperMode.getRule(element, context);
	    	if (rule==null) return ALWAYS_PRESERVE;
	    	return (((Boolean)rule.getAction()).booleanValue() ? ALWAYS_PRESERVE : STRIP_DEFAULT);
//	    } catch (XPathException err) {
//	    	return ALWAYS_PRESERVE;
//	    }
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
        nextReceiver.startElement(nameCode, typeCode, locationId, properties);

        byte preserveParent = stripStack[top];
        byte preserve = (byte)(preserveParent & PRESERVE_PARENT);

        byte elementStrip = isSpacePreserving(nameCode);
        if (elementStrip == ALWAYS_PRESERVE) {
            preserve |= ALWAYS_PRESERVE;
        } else if (elementStrip == ALWAYS_STRIP) {
            preserve |= ALWAYS_STRIP;
        }
        if (preserve == 0 && typeCode != -1 && typeCode != StandardNames.XDT_UNTYPED) {
            // if the element has simple content, whitespace stripping is disabled
            SchemaType type = getConfiguration().getSchemaType(typeCode);
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

        if ((nameCode & 0xfffff) == StandardNames.XML_SPACE) {
            if (value.toString().equals("preserve")) {
                stripStack[top] |= PRESERVE_PARENT;
            } else {
                stripStack[top] &= ~PRESERVE_PARENT;
            }
        }
        nextReceiver.attribute(nameCode, typeCode, value, locationId, properties);
    }

    /**
    * Handle an end-of-element event
    */

    public void endElement () throws XPathException
    {
        nextReceiver.endElement();
        top--;
    }

    /**
    * Handle a text node
    */

    public void characters (CharSequence chars, int locationId, int properties) throws XPathException
    {
        // assume adjacent chunks of text are already concatenated

        if (chars.length() > 0) {
            if ((((stripStack[top] & (ALWAYS_PRESERVE | PRESERVE_PARENT | CANNOT_STRIP)) != 0) &&
                  (stripStack[top] & ALWAYS_STRIP) == 0)
                    || !Whitespace.isWhite(chars)) {
                nextReceiver.characters(chars, locationId, properties);
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
