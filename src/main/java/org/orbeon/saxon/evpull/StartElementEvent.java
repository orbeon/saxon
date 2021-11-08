package org.orbeon.saxon.evpull;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.expr.ExpressionLocation;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.Orphan;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.sort.IntArraySet;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This is a PullEvent representing the start of an element node. It contains (or potentially contains) all the
 * namespace declarations and attributes associated with the element.
 */
public class StartElementEvent implements PullEvent {

    PipelineConfiguration pipe;
    private int nameCode;
    private int typeCode;
    private int[] localNamespaces;
    private List attributes;
    private int locationId = -1;

    /**
     * Create a Start Element Event
     * @param pipe the pipeline configuration
     */

    public StartElementEvent(PipelineConfiguration pipe) {
        this.pipe = pipe;
    }

    /**
     * Set the nameCode of this element
     * @param nameCode the namecode of the element (its name as identified in the NamePool)
     */

    public void setNameCode(int nameCode) {
        this.nameCode = nameCode;
    }

    /**
     * Get the nameCode of this element
     * @return the nameCode representing the element's name
     */

    public int getNameCode() {
        return nameCode;
    }

    /**
     * Set the typeCode of this element
     * @param typeCode the name pool fingerprint of the element's type annotation
     */

    public void setTypeCode(int typeCode) {
        this.typeCode = typeCode;
    }

    /**
     * Get the typeCode of this element
     * @return the name pool fingerprint of the element's type annotation
     */

    public int getTypeCode() {
        return typeCode;
    }

    /**
     * Set the namespaces that are locally declared (or undeclared) on this element
     * @param nscodes integer array of namespace codes
     */

    public void setLocalNamespaces(int[] nscodes) {
        localNamespaces = nscodes;
    }

    /**
     * Add a namespace code representing a locally declared namespace
     * @param nscode a namespace code
     * @throws XPathException
     */

    public void addNamespace(int nscode) throws XPathException {
        if (localNamespaces == null) {
            localNamespaces = new int[]{nscode, -1, -1, -1};
        }
        for (int n=0; n<localNamespaces.length; n++) {
            int nn = localNamespaces[n];
            if (nn == nscode) {
                return;
            }
            if (nn == -1) {
                localNamespaces[n] = nscode;
                if (n < localNamespaces.length - 1) {
                    localNamespaces[n+1] = -1;
                }
                return;
            }
            if ((nn & 0xffff0000) == (nscode & 0xffff0000)) {
                NamePool pool = pipe.getConfiguration().getNamePool();
                String prefix = pool.getPrefixFromNamespaceCode(nscode);
                String uri1 = pool.getURIFromNamespaceCode(nn);
                String uri2 = pool.getURIFromNamespaceCode(nscode);
                XPathException err = new XPathException(
                        "Cannot create two namespace nodes with the same prefix mapped to different URIs (prefix=" +
                        (prefix.length() == 0 ? "\"\"" : prefix) + ", URI=" +
                        (uri1.length() == 0 ? "\"\"" : uri1) + ", URI=" +
                        (uri2.length() == 0 ? "\"\"" : uri2) + ")");
                err.setErrorCode("XTDE0430");
                throw err;
            }
        }
        int[] n2 = new int[localNamespaces.length * 2 + 2];
        System.arraycopy(localNamespaces, 0, n2, 0, localNamespaces.length);
        n2[localNamespaces.length] = nscode;
        n2[localNamespaces.length+1] = -1;
        localNamespaces = n2;
    }

    /**
     * Get the namespaces locally declared on this element
     * @return an array of namespace codes
     */

    public int[] getLocalNamespaces() {
        if (localNamespaces == null) {
            return IntArraySet.EMPTY_INT_ARRAY;
        }
        return localNamespaces;
    }

    /**
     * Add an attribute to the element node
     * @param att the attribute to be added
     * @throws XPathException in the event of a dynamic error, for example a duplicate attribute in XQuery
     */

    public void addAttribute(NodeInfo att) throws XPathException {
        if (attributes == null) {
            attributes = new ArrayList(4);
        }
        int fp = att.getFingerprint();
        for (int a=0; a<attributes.size(); a++) {
            int fp2 = ((NodeInfo)attributes.get(a)).getFingerprint();
            if (fp == fp2) {
                if (pipe.getHostLanguage() == Configuration.XQUERY) {
                    // In XQuery, duplicate attributes are an error
                    XPathException err = new XPathException(
                            "Cannot create an element having two attributes with the same name: " +
                            Err.wrap(att.getDisplayName(), Err.ATTRIBUTE));
                    err.setErrorCode("XQDY0025");
                    if (locationId != -1) {
                        err.setLocator(new ExpressionLocation(pipe.getLocationProvider(), locationId));
                    }
                    throw err;
                } else {
                    // In XSLT, the last attribute to be added wins
                    attributes.set(a, att);
                    return;
                }
            }
        }
        attributes.add(att);
    }

    /**
     * Ask whether the element has any attributes
     * @return true if the element has one or more attributes
     */

    public boolean hasAttributes() {
        return attributes != null && !attributes.isEmpty();
    }

    /**
     * Ask how may attributes the element has
     * @return the number of attributes that the element has
     */

    public int getAttributeCount() {
        return attributes == null ? 0 : attributes.size();
    }

    /**
     * Get an iterator over the attributes of this element
     * @return an iterator which delivers NodeInfo objects representing the attributes of this element
     */

    public Iterator iterateAttributes() {
        if (attributes == null) {
            return Collections.EMPTY_LIST.iterator();
        } else {
            return attributes.iterator();
        }
    }

    /**
     * Get the n'th attribute if there is one
     * @param index the index of the attributes, starting from zero
     * @return a NodeInfo representing the attribute, or null if there is no such attribute
     */

    public NodeInfo getAttribute(int index) {
        if (attributes == null) {
            return null;
        } else {
            return (NodeInfo)attributes.get(index);
        }
    }

    /**
     * Perform namespace fixup. This is done after all the attributes and explicit namespaces have been added.
     * Namespace fixup ensures that a namespace declaration is present for the element name and for every
     * attribute name, and that the prefixes of the element and each attribute are consistent with the declared
     * namespaces, changing any prefixes in the event of a conflict.
     */

    public void namespaceFixup() {
        NamePool pool = pipe.getConfiguration().getNamePool();
        nameCode = fixup(pool, nameCode, 0);
        if (attributes != null) {
            for (int a=0; a<attributes.size(); a++) {
                NodeInfo oldAtt = (NodeInfo)attributes.get(a);
                int oldCode = oldAtt.getNameCode();
                int newCode = fixup(pool, oldCode, a);
                if (oldCode != newCode) {
                    Orphan att = new Orphan(oldAtt.getConfiguration());
                    att.setNodeKind(Type.ATTRIBUTE);
                    att.setNameCode(newCode);
                    att.setStringValue(oldAtt.getStringValue());
                    att.setTypeAnnotation(oldAtt.getTypeAnnotation());
                    att.setSystemId(oldAtt.getSystemId());
                    attributes.set(a, att);
                }
            }
        }
    }

    private int fixup(NamePool pool, int nameCode, int seq) {
        int nscode = pool.getNamespaceCode(nameCode);
        if (nscode == 0) {
            return nameCode;
        }
        if (nscode == -1) {
            nscode = pool.allocateNamespaceCode(nameCode);
        }
        if (localNamespaces != null) {
            for (int n=0; n<localNamespaces.length; n++) {
                int nn = localNamespaces[n];
                if (nn == nscode) {
                    return nameCode;
                }
                if (nn == -1) {
                    break;
                }
                if ((nscode & 0xffff0000) == (nn & 0xffff0000)) {
                    // Same as an existing prefix, but mapped to a different URI: we need to choose a new prefix
                    String local = pool.getLocalName(nameCode);
                    String uri = pool.getURI(nameCode);
                    String prefix = pool.getPrefix(nameCode) + "_" + seq;
                    int newCode = pool.allocate(prefix, uri, local);
                    return fixup(pool, newCode, seq);
                }
            }
        }
        // Namespace declaration not found: we need to add it
        try {
            addNamespace(nscode);
        } catch (XPathException err) {
            throw new AssertionError(err);
        }
        return nameCode;
    }

    /**
     * Strip type annotations from the element and its attributes
     */

    public void stripTypeAnnotations() {
        setTypeCode(StandardNames.XS_UNTYPED);
        if (attributes != null) {
            for (int i=0; i<attributes.size(); i++) {
                NodeInfo att = (NodeInfo)attributes.get(i);
                if (att.getTypeAnnotation() != StandardNames.XS_UNTYPED_ATOMIC) {
                    Orphan o = new Orphan(att.getConfiguration());
                    o.setNodeKind(Type.ATTRIBUTE);
                    o.setNameCode(att.getNameCode());
                    o.setStringValue(att.getStringValue());
                    o.setSystemId(att.getSystemId());
                    o.setTypeAnnotation(StandardNames.XS_UNTYPED_ATOMIC);
                    attributes.set(i, o);
                }
            }
        }
    }

    /**
     * Get the PipelineConfiguration
     * @return the PipelineConfiguration
     */

    public PipelineConfiguration getPipelineConfiguration() {
        return pipe;
    }

    /**
     * Set the location associated with the event
     * @param locationId a location identifier, to be interpreted by the locationProvider
     * associated with the PipelineConfiguration
     */

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    /**
     * Get the location associated with the event
     * @return a location identifier, to be interpreted by the locationProvider
     * associated with the PipelineConfiguration, or -1 if none is available
     */

    public int getLocationId() {
        return locationId;
    }

//    public static void main(String[] args) throws Exception {
//        try {
//            Configuration config = new Configuration();
//            config.setLineNumbering(true);
//            DocumentInfo doc = config.buildDocument(new StreamSource(new File("C:/MyJava/testdata/books.xml")));
//            PipelineConfiguration pipe = config.makePipelineConfiguration();
//            EventIterator ei = new Decomposer(doc, pipe);
//            XMLStreamReader reader = new EventToStaxBridge(ei, config.getNamePool());
//            while (true) {
//                int event = reader.next();
//                if (event == XMLStreamReader.END_DOCUMENT) {
//                    break;
//                }
//                System.err.println("EVENT: " + event + " @ " +
//                        reader.getLocation().getSystemId() + "#" + reader.getLocation().getLineNumber());
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
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

