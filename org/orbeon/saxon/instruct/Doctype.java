package net.sf.saxon.instruct;

import net.sf.saxon.Controller;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.PromotionOffer;
import net.sf.saxon.om.*;
import net.sf.saxon.tinytree.TinyBuilder;

import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.xpath.DynamicError;

import java.io.PrintStream;

/**
 * A saxon:doctype element in the stylesheet.
 */

public class Doctype extends InstructionWithChildren {

    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     */

    public int getInstructionNameCode() {
        return StandardNames.SAXON_DOCTYPE;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        XPathContext c2 = context.newMinorContext();
        c2.setOrigin(this);
        SequenceReceiver out = c2.getReceiver();
        TinyBuilder builder = new TinyBuilder();
        Receiver receiver = builder;
        receiver.setConfiguration(controller.getConfiguration());
        receiver.open();

        c2.changeOutputDestination(null, receiver, false, Validation.PRESERVE, null);
        processChildren(c2);
        receiver.close();
        DocumentInfo dtdRoot = builder.getCurrentDocument();

        SequenceIterator children = dtdRoot.iterateAxis(Axis.CHILD);
        NodeInfo docType = (NodeInfo) children.next();
        if (docType == null || !("doctype".equals(docType.getLocalPart()))) {
            DynamicError e = new DynamicError("saxon:doctype instruction must contain dtd:doctype");
            e.setXPathContext(context);
            throw e;
        }
        String name = Navigator.getAttributeValue(docType, "", "name");
        String system = Navigator.getAttributeValue(docType, "", "system");
        String publicid = Navigator.getAttributeValue(docType, "", "public");

        if (name == null) {
            DynamicError e = new DynamicError("dtd:doctype must have a name attribute");
            e.setXPathContext(context);
            throw e;
        }

        write(out, "<!DOCTYPE " + name + " ");
        if (system != null) {
            if (publicid != null) {
                write(out, "PUBLIC \"" + publicid + "\" \"" + system + "\"");
            } else {
                write(out, "SYSTEM \"" + system + "\"");
            }
        }

        boolean openSquare = false;
        children = docType.iterateAxis(Axis.CHILD);

        NodeInfo child = (NodeInfo) children.next();
        if (child != null) {
            write(out, " [");
            openSquare = true;
        }

        while (child != null) {
            String localname = child.getLocalPart();

            if ("element".equals(localname)) {
                String elname = Navigator.getAttributeValue(child, "", "name");
                String content = Navigator.getAttributeValue(child, "", "content");
                if (elname == null) {
                    DynamicError e = new DynamicError("dtd:element must have a name attribute");
                    e.setXPathContext(context);
                    throw e;
                }
                if (content == null) {
                    DynamicError e = new DynamicError("dtd:element must have a content attribute");
                    e.setXPathContext(context);
                    throw e;
                }
                write(out, "\n  <!ELEMENT " + elname + " " + content + ">");

            } else if (localname.equals("attlist")) {
                String elname = Navigator.getAttributeValue(child, "", "element");
                if (elname == null) {
                    DynamicError e = new DynamicError("dtd:attlist must have an attribute named 'element'");
                    e.setXPathContext(context);
                    throw e;
                }
                write(out, "\n  <!ATTLIST " + elname + " ");

                SequenceIterator attributes = child.iterateAxis(Axis.CHILD);
                while (true) {
                    NodeInfo attDef = (NodeInfo) attributes.next();
                    if (attDef == null) {
                        break;
                    }

                    if ("attribute".equals(attDef.getLocalPart())) {

                        String atname = Navigator.getAttributeValue(attDef, "", "name");
                        String type = Navigator.getAttributeValue(attDef, "", "type");
                        String value = Navigator.getAttributeValue(attDef, "", "value");
                        if (atname == null) {
                            DynamicError e = new DynamicError("dtd:attribute must have a name attribute");
                            e.setXPathContext(context);
                            throw e;
                        }
                        if (type == null) {
                            DynamicError e = new DynamicError("dtd:attribute must have a type attribute");
                            e.setXPathContext(context);
                            throw e;
                        }
                        if (value == null) {
                            DynamicError e = new DynamicError("dtd:attribute must have a value attribute");
                            e.setXPathContext(context);
                            throw e;
                        }
                        write(out, "\n    " + atname + " " + type + " " + value);
                    } else {
                        DynamicError e = new DynamicError("Unrecognized element within dtd:attlist");
                        e.setXPathContext(context);
                        throw e;
                    }
                }
                write(out, ">");

            } else if (localname.equals("entity")) {

                String entname = Navigator.getAttributeValue(child, "", "name");
                String parameter = Navigator.getAttributeValue(child, "", "parameter");
                String esystem = Navigator.getAttributeValue(child, "", "system");
                String epublicid = Navigator.getAttributeValue(child, "", "public");
                String notation = Navigator.getAttributeValue(child, "", "notation");

                if (entname == null) {
                    DynamicError e = new DynamicError("dtd:entity must have a name attribute");
                    e.setXPathContext(context);
                    throw e;
                }

                // we could do a lot more checking now...

                write(out, "\n  <!ENTITY ");
                if ("yes".equals(parameter)) {
                    write(out, "% ");
                }
                write(out, entname + " ");
                if (esystem != null) {
                    if (epublicid != null) {
                        write(out, "PUBLIC \"" + epublicid + "\" \"" + esystem + "\" ");
                    } else {
                        write(out, "SYSTEM \"" + esystem + "\" ");
                    }
                }
                if (notation != null) {
                    write(out, "NDATA " + notation + " ");
                }

                SequenceIterator contents = child.iterateAxis(Axis.CHILD);
                while (true) {
                    NodeInfo content = (NodeInfo) contents.next();
                    if (content == null) {
                        break;
                    }
                    content.copy(out, NodeInfo.NO_NAMESPACES, false, locationId);
                }
                write(out, ">");

            } else if (localname.equals("notation")) {
                String notname = Navigator.getAttributeValue(child, "", "name");
                String nsystem = Navigator.getAttributeValue(child, "", "system");
                String npublicid = Navigator.getAttributeValue(child, "", "public");
                if (notname == null) {
                    DynamicError e = new DynamicError("dtd:notation must have a name attribute");
                    e.setXPathContext(context);
                    throw e;
                }
                if ((nsystem == null) && (npublicid == null)) {
                    DynamicError e = new DynamicError("dtd:notation must have a system attribute or a public attribute");
                    e.setXPathContext(context);
                    throw e;
                }
                write(out, "\n  <!NOTATION " + notname);
                if (npublicid != null) {
                    write(out, " PUBLIC \"" + npublicid + "\" ");
                    if (nsystem != null) {
                        write(out, "\"" + nsystem + "\" ");
                    }
                } else {
                    write(out, " SYSTEM \"" + nsystem + "\" ");
                }
                write(out, ">");
            } else {
                DynamicError e = new DynamicError("Unrecognized element " + localname + " in DTD output");
                e.setXPathContext(context);
                throw e;
            }
            child = (NodeInfo) children.next();
        }

        if (openSquare) {
            write(out, "\n]");
            openSquare = false;
        }
        write(out, ">\n");

        return null;

    }

    private void write(Receiver out, String s) throws XPathException {
        out.characters(s, locationId, ReceiverOptions.DISABLE_ESCAPING);
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param out
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "saxon:doctype");
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
