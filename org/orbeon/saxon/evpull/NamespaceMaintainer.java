package org.orbeon.saxon.evpull;

import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * NamespaceMaintainer is an EventIterator responsible for maintaining namespace context in an
 * event stream. It allows the current namespace context to be determined at any time while
 * processing the stream of events.
 *
 * <p>Note that this class merely provides the service of keeping track of which namespaces are
 * currently in scope. It does not attempt to remove duplicate namespace declarations, and it does
 * not perform namespace fixup.</p>
 */

public class NamespaceMaintainer implements EventIterator, NamespaceResolver {

    private EventIterator base;
    private NamePool namePool;

    // We keep track of namespaces to avoid outputting duplicate declarations. The namespaces
    // vector holds a list of all namespaces currently declared (organised as integer namespace codes).
    // The countStack contains an entry for each element currently open; the
    // value on the countStack is an Integer giving the number of namespaces added to the main
    // namespace stack by that element.

    private int[] allNamespaces = new int[50];          // all namespace codes currently declared
    private int allNamespacesSize = 0;                  // all namespaces currently declared
    private int[] namespaceCountStack = new int[50];    // one entry per started element, holding the number
    private int depth = 0;                              // current depth of element nesting

    /**
     * Create a namespace context for a pull-events pipeline
     * @param base the previous stage in the pipeline, from which events are read
     * @param namePool the NamePool
     */

    public NamespaceMaintainer(EventIterator base, NamePool namePool) {
        this.base = EventStackIterator.flatten(base);
        this.namePool = namePool;
    }


    /**
     * Determine whether the EventIterator returns a flat sequence of events, or whether it can return
     * nested event iterators
     *
     * @return true if the next() method is guaranteed never to return an EventIterator
     */

    public boolean isFlatSequence() {
        return true;
    }

    /**
     * Get the next event in the sequence
     *
     * @return the next event, or null when the sequence is exhausted. Note that since an EventIterator is
     *         itself a PullEvent, this method may return a nested iterator.
     * @throws org.orbeon.saxon.trans.XPathException
     *          if a dynamic evaluation error occurs
     */

    public PullEvent next() throws XPathException {
        PullEvent event = base.next();
        if (event instanceof StartElementEvent) {
            startElement((StartElementEvent)event);
        } else if (event instanceof EndElementEvent) {
            endElement();
        }
        return event;
    }

    private void startElement(StartElementEvent event) throws XPathException {

        // Record the current height of the namespace list so it can be reset at endElement time

        int[] declaredNamespaces = event.getLocalNamespaces();
        int numberOfDeclaredNamespaces = declaredNamespaces.length;
        for (int i=0; i<declaredNamespaces.length; i++) {
            if (declaredNamespaces[i] == -1) {
                numberOfDeclaredNamespaces = i;
                break;
            }
        }

        if (depth >= namespaceCountStack.length) {
            int[] newstack = new int[depth * 2];
            System.arraycopy(namespaceCountStack, 0, newstack, 0, depth);
            namespaceCountStack = newstack;
        }
        namespaceCountStack[depth++] = numberOfDeclaredNamespaces;

        // expand the stack if necessary
        while (allNamespacesSize + numberOfDeclaredNamespaces >= allNamespaces.length) {
            int[] newlist = new int[allNamespacesSize * 2];
            System.arraycopy(allNamespaces, 0, newlist, 0, allNamespacesSize);
            allNamespaces = newlist;
        }

        for (int i=0; i<declaredNamespaces.length; i++) {
            if (declaredNamespaces[i] == -1) {
                break;
            }
            allNamespaces[allNamespacesSize++] = declaredNamespaces[i];
        }

        // test code
//        System.err.println("==============");
//        System.err.println("ELEMENT " + namePool.getDisplayName(event.getNameCode()));
//        for (Iterator iter = iteratePrefixes(); iter.hasNext();) {
//            String prefix = (String)iter.next();
//            String uri = (getURIForPrefix(prefix, true));
//            System.err.println("  '" + prefix + "'='" + uri + "'");
//        }
//        System.err.println("==============");

    }

    private void endElement() {
        allNamespacesSize -= namespaceCountStack[--depth];
    }


    /**
     * Get the URI code corresponding to a given prefix code, by searching the
     * in-scope namespaces. This is a service provided to subclasses.
     *
     * @param prefixCode the 16-bit prefix code required
     * @return the 16-bit URI code, or -1 if the prefix is not found
     */

    protected short getURICode(short prefixCode) {
        for (int i = allNamespacesSize - 1; i >= 0; i--) {
            if ((allNamespaces[i] >> 16) == (prefixCode)) {
                return (short) (allNamespaces[i] & 0xffff);
            }
        }
        if (prefixCode == 0) {
            return 0;   // by default, no prefix means no namespace URI
        } else {
            return -1;
        }
    }

    /**
     * Get the namespace URI corresponding to a given prefix. Return null
     * if the prefix is not in scope.
     *
     * @param prefix     the namespace prefix
     * @param useDefault true if the default namespace is to be used when the
     *                   prefix is ""
     * @return the uri for the namespace, or null if the prefix is not in scope
     */

    public String getURIForPrefix(String prefix, boolean useDefault) {
        if ((prefix == null || prefix.length() == 0) && !useDefault) {
            return "";
        } else if ("xml".equals(prefix)) {
            return NamespaceConstant.XML;
        } else {
            short prefixCode = namePool.getCodeForPrefix(prefix);
            short uriCode = getURICode(prefixCode);
            if (uriCode == -1) {
                return null;
            }
            return namePool.getURIFromURICode(uriCode);
        }
    }

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

    public Iterator iteratePrefixes() {
        List prefixes = new ArrayList(allNamespacesSize);
        for (int i = allNamespacesSize - 1; i >= 0; i--) {
            String prefix = namePool.getPrefixFromNamespaceCode(allNamespaces[i]);
            if (!prefixes.contains(prefix)) {
                prefixes.add(prefix);
            }
        }
        prefixes.add("xml");
        return prefixes.iterator();
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

