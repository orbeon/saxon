package org.orbeon.saxon.tinytree;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.LocationProvider;
import org.orbeon.saxon.event.ReceiverOptions;
import org.orbeon.saxon.event.SourceLocationProvider;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;


/**
  * The TinyBuilder class is responsible for taking a stream of SAX events and constructing
  * a Document tree, using the "TinyTree" implementation.
  *
  * @author Michael H. Kay
  */

public class TinyBuilder extends Builder {

    public static final int PARENT_POINTER_INTERVAL = 10;
            // a lower value allocates more parent pointers which takes more space but reduces
            // the length of parent searches

    private TinyTree tree;

    private int currentDepth = 0;
    private int nodeNr = 0;             // this is the local sequence within this document
    private boolean ended = false;
    private int[] sizeParameters;       // estimate of number of nodes, attributes, namespaces, characters

    /**
     * Create a TinyTree builder
     */

    public TinyBuilder() {}

    /**
     * Set the size parameters for the tree
     * @param params an array of four integers giving the expected number of non-attribute nodes, the expected
     * number of attributes, the expected number of namespace declarations, and the expected total length of
     * character data
     */

    public void setSizeParameters(int[] params) {
        sizeParameters = params;
    }

    /**
     * Get the size parameters for the tree
     * @return an array of four integers giving the actual number of non-attribute nodes, the actual
     * number of attributes, the actual number of namespace declarations, and the actual total length of
     * character data
     */

    public int[] getSizeParameters() {
        return new int[] {tree.numberOfNodes, tree.numberOfAttributes, tree.numberOfNamespaces,
                        tree.getCharacterBuffer().length()};
    }

    private int[] prevAtDepth = new int[100];
            // this array is scaffolding used while constructing the tree, it is
            // not present in the final tree. For each level of the tree, it records the
            // node number of the most recent node at that level.

    private int[] siblingsAtDepth = new int[100];
            // more scaffolding. For each level of the tree, this array records the
            // number of siblings processed at that level. When this exceeds a threshold value,
            // a dummy node is inserted into the arrays to contain a parent pointer: this it to
            // prevent excessively long searches for a parent node, which is normally found by
            // scanning the siblings. The value is then reset to zero.

    private boolean isIDElement = false;

    /**
     * Get the tree being built by this builder
     * @return the TinyTree
     */

    public TinyTree getTree() {
        return tree;
    }

    /**
     * Open the event stream
     */

    public void open() throws XPathException {
        if (started) {
            // this happens when using an IdentityTransformer
            return;
        }
        if (tree == null) {
            if (sizeParameters==null) {
                tree = new TinyTree();
            } else {
                tree = new TinyTree(sizeParameters[0],
                                        sizeParameters[1], sizeParameters[2], sizeParameters[3]);
            }
            tree.setConfiguration(config);
            currentDepth = 0;
            if (lineNumbering) {
                tree.setLineNumbering();
            }
        }
        super.open();
    }

    /**
    * Write a document node to the tree
    */

    public void startDocument (int properties) throws XPathException {
//        if (currentDepth == 0 && tree.numberOfNodes != 0) {
//            System.err.println("**** FOREST DOCUMENT ****");
//        }
        if ((started && !ended) || currentDepth > 0) {
            // this happens when using an IdentityTransformer, or when copying a document node to form
            // the content of an element
            return;
        }
        started = true;
        ended = false;

        currentRoot = new TinyDocumentImpl(tree);
        TinyDocumentImpl doc = (TinyDocumentImpl)currentRoot;
        doc.setSystemId(getSystemId());
        doc.setBaseURI(getBaseURI());
        doc.setConfiguration(config);

        currentDepth = 0;

        int nodeNr = tree.addDocumentNode((TinyDocumentImpl)currentRoot);
        prevAtDepth[0] = nodeNr;
        prevAtDepth[1] = -1;
        siblingsAtDepth[0] = 0;
        siblingsAtDepth[1] = 0;
        tree.next[nodeNr] = -1;

        currentDepth++;

        super.startDocument(0);

    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void endDocument () throws XPathException {
//             System.err.println("TinyBuilder: " + this + " End document");

        if (currentDepth > 1) return;
            // happens when copying a document node as the child of an element

        if (ended) return;  // happens when using an IdentityTransformer
        ended = true;

        prevAtDepth[currentDepth] = -1;
        currentDepth--;

    }

    public void reset() {
        super.reset();
        tree = null;
        currentDepth = 0;
        nodeNr = 0;
        ended = false;
        sizeParameters = null;
    }

    public void close() throws XPathException {
        //System.err.println("Tree.close " + tree + " size=" + tree.numberOfNodes);
        tree.addNode(Type.STOPPER, 0, 0, 0, -1);
        tree.condense();
        super.close();
    }

    /**
    * Notify the start tag of an element
    */

    public void startElement (int nameCode, int typeCode, int locationId, int properties) throws XPathException
    {
//        if (currentDepth == 0 && tree.numberOfNodes != 0) {
//            System.err.println("**** FOREST ELEMENT **** trees=" + tree.rootIndexUsed );
//        }

        // if the number of siblings exceeds a certain threshold, add a parent pointer, in the form
        // of a pseudo-node
        if (siblingsAtDepth[currentDepth] > PARENT_POINTER_INTERVAL) {
            nodeNr = tree.addNode(Type.PARENT_POINTER, currentDepth, prevAtDepth[currentDepth-1], 0, 0);
            int prev = prevAtDepth[currentDepth];
            if (prev > 0) {
                tree.next[prev] = nodeNr;
            }
            tree.next[nodeNr] = prevAtDepth[currentDepth-1];
            prevAtDepth[currentDepth] = nodeNr;
            siblingsAtDepth[currentDepth] = 0;
        }

        // now add the element node itself
		nodeNr = tree.addNode(Type.ELEMENT, currentDepth, -1, -1, nameCode);

		isIDElement = ((properties & ReceiverOptions.IS_ID) != 0);
        if (typeCode != StandardNames.XS_UNTYPED && typeCode != -1) {
            if ((properties & ReceiverOptions.NILLED_ELEMENT) != 0) {
                typeCode |= NodeInfo.IS_NILLED;
            }
            tree.setElementAnnotation(nodeNr, typeCode);
            if (!isIDElement && config.getTypeHierarchy().isIdCode(typeCode)) {
                isIDElement = true;
            }
		}

        if (currentDepth == 0) {
            prevAtDepth[0] = nodeNr;
            prevAtDepth[1] = -1;
            //tree.next[0] = -1;
            currentRoot = tree.getNode(nodeNr);
        } else {
            int prev = prevAtDepth[currentDepth];
            if (prev > 0) {
                tree.next[prev] = nodeNr;
            }
            tree.next[nodeNr] = prevAtDepth[currentDepth - 1];   // *O* owner pointer in last sibling
            prevAtDepth[currentDepth] = nodeNr;
            siblingsAtDepth[currentDepth]++;
        }
        currentDepth++;

        if (currentDepth == prevAtDepth.length) {
            int[] p2 = new int[currentDepth*2];
            System.arraycopy(prevAtDepth, 0, p2, 0, currentDepth);
            prevAtDepth = p2;
            p2 = new int[currentDepth*2];
            System.arraycopy(siblingsAtDepth, 0, p2, 0, currentDepth);
            siblingsAtDepth = p2;
        }
        prevAtDepth[currentDepth] = -1;
        siblingsAtDepth[currentDepth] = 0;

        LocationProvider locator = pipe.getLocationProvider();
        if (locator instanceof SourceLocationProvider) {
            tree.setSystemId(nodeNr, locator.getSystemId(locationId));
//            if (lineNumbering) {
//                tree.setLineAndColumn(nodeNr, locator.getLineNumber(locationId), locator.getColumnNumber(locationId));
//            }
        } else if (currentDepth == 1) {
            tree.setSystemId(nodeNr, systemId);
        }
        if (lineNumbering) {
            tree.setLineNumber(nodeNr, locator.getLineNumber(locationId), locator.getColumnNumber(locationId));
        }
    }

    public void namespace(int namespaceCode, int properties) throws XPathException {
        tree.addNamespace( nodeNr, namespaceCode );
    }

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        // System.err.println("attribute " + nameCode + "=" + value);

        tree.addAttribute(currentRoot, nodeNr, nameCode, typeCode, value, properties);
    }

    public void startContent() {
        nodeNr++;
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void endElement() throws XPathException {
//        System.err.println("End element");
        prevAtDepth[currentDepth] = -1;
        siblingsAtDepth[currentDepth] = 0;
        currentDepth--;
        if (isIDElement) {
            // we're relying on the fact that an ID element has no element children!
            tree.indexIDElement(currentRoot, prevAtDepth[currentDepth], config.getNameChecker());
            isIDElement = false;
        }
    }

    /**
     * Get the last completed element node. This is used during checking of schema assertions,
     * which happens while the tree is still under construction. This method is called immediately after
     * a call on endElement(), and it returns the element that has just ended.
     * @return the last completed element node, that is, the element whose endElement event is the most recent
     * endElement event to be reported
     */

    public NodeInfo getLastCompletedElement() {
        return tree.getNode(prevAtDepth[currentDepth]);
        // Note: reading an incomplete tree needs care if it constructs a prior index, etc.
    }


    /**
    * Callback interface for SAX: not for application use
    */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException
    {
        // TODO:PERF when appropriate, point to an existing text node containing the same characters

        if (chars instanceof CompressedWhitespace &&
                (properties & ReceiverOptions.WHOLE_TEXT_NODE) != 0) {
            long lvalue = ((CompressedWhitespace)chars).getCompressedValue();
            nodeNr = tree.addNode(Type.WHITESPACE_TEXT, currentDepth, (int)(lvalue>>32), (int)(lvalue), -1);

            int prev = prevAtDepth[currentDepth];
            if (prev > 0) {
                tree.next[prev] = nodeNr;
            }
            tree.next[nodeNr] = prevAtDepth[currentDepth - 1];   // *O* owner pointer in last sibling
            prevAtDepth[currentDepth] = nodeNr;
            siblingsAtDepth[currentDepth]++;
            return;
        }

        final int len = chars.length();
        if (len>0) {
            int bufferStart = tree.getCharacterBuffer().length();
            tree.appendChars(chars);
            int n=tree.numberOfNodes-1;
            if (tree.nodeKind[n] == Type.TEXT && tree.depth[n] == currentDepth) {
                // merge this text node with the previous text node
                tree.beta[n] += len;
            } else {
                nodeNr = tree.addNode(Type.TEXT, currentDepth, bufferStart, len, -1);

                int prev = prevAtDepth[currentDepth];
                if (prev > 0) {
                    tree.next[prev] = nodeNr;
                }
                tree.next[nodeNr] = prevAtDepth[currentDepth - 1];   // *O* owner pointer in last sibling
                prevAtDepth[currentDepth] = nodeNr;
                siblingsAtDepth[currentDepth]++;
            }
        }
    }

    /**
    * Callback interface for SAX: not for application use<BR>
    */

    public void processingInstruction (String piname, CharSequence remainder, int locationId, int properties) throws XPathException
    {
        if (tree.commentBuffer==null) {
            tree.commentBuffer = new FastStringBuffer(200);
        }
        int s = tree.commentBuffer.length();
        tree.commentBuffer.append(remainder.toString());
        int nameCode = namePool.allocate("", "", piname);

        nodeNr = tree.addNode(Type.PROCESSING_INSTRUCTION, currentDepth, s, remainder.length(),
        			 nameCode);

        int prev = prevAtDepth[currentDepth];
        if (prev > 0) {
            tree.next[prev] = nodeNr;
        }
        tree.next[nodeNr] = prevAtDepth[currentDepth - 1];   // *O* owner pointer in last sibling
        prevAtDepth[currentDepth] = nodeNr;
        siblingsAtDepth[currentDepth]++;

        LocationProvider locator = pipe.getLocationProvider();
        if (locator instanceof SourceLocationProvider) {
            tree.setSystemId(nodeNr, locator.getSystemId(locationId));
            if (lineNumbering) {
                tree.setLineNumber(nodeNr, locator.getLineNumber(locationId), locator.getColumnNumber(locationId));
            }
        }
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void comment (CharSequence chars, int locationId, int properties) throws XPathException {
        if (tree.commentBuffer==null) {
            tree.commentBuffer = new FastStringBuffer(200);
        }
        int s = tree.commentBuffer.length();
        tree.commentBuffer.append(chars.toString());
        nodeNr = tree.addNode(Type.COMMENT, currentDepth, s, chars.length(), -1);

        int prev = prevAtDepth[currentDepth];
        if (prev > 0) {
            tree.next[prev] = nodeNr;
        }
        tree.next[nodeNr] = prevAtDepth[currentDepth - 1];   // *O* owner pointer in last sibling
        prevAtDepth[currentDepth] = nodeNr;
        siblingsAtDepth[currentDepth]++;

    }

    /**
    * Set an unparsed entity in the document
    */

    public void setUnparsedEntity(String name, String uri, String publicId) {
        ((TinyDocumentImpl)currentRoot).setUnparsedEntity(name, uri, publicId);
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
