package net.sf.saxon;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import java.util.Stack;
import java.util.HashMap;
import java.util.Iterator;


/**
* IDFilter is a SAX filter that extracts the subtree of a document rooted at the
* element with a given ID value. Namespace declarations outside this subtree are
* treated as if they were present on the identified element.
*/

public class IDFilter extends XMLFilterImpl {

    private String id;
    private int activeDepth = 0;
    private Stack namespacePrefixes = new Stack();
    private Stack namespaceURIs = new Stack();
    
    public IDFilter (String id) {
        // System.err.println("IDFilter, looking for " + id);
        this.id = id;
    }

    public void startPrefixMapping(String prefix, String uri)
    throws SAXException {
        // System.err.println("Start prefix " + prefix + " at " + activeDepth);
        if (activeDepth > 0) {
            super.startPrefixMapping(prefix, uri);
        } else {
            namespacePrefixes.push(prefix);
            namespaceURIs.push(uri);
        }
    }
    
    public void endPrefixMapping(String prefix)
    throws SAXException {
        // System.err.println("End prefix " + prefix + " at " + activeDepth);
        if (activeDepth > 0) {
            super.endPrefixMapping(prefix);
        } else {
            namespacePrefixes.pop();
            namespaceURIs.pop();
        }
    }        
            

    public void startElement(String localName, String uri, String qname, Attributes atts)
    throws SAXException {
        // System.err.println("Start element " + qname + " at " + activeDepth);
        if (activeDepth==0) {
            for (int a=0; a<atts.getLength(); a++) {
                if (atts.getType(a).equals("ID") && atts.getValue(a).equals(id)) {
                    activeDepth = 1;
                    break;
                }
            }
            if (activeDepth==1) {
                // Collect together all the in-scope namespaces that haven't been redeclared
                // or undeclared
                HashMap nsmap = new HashMap(namespacePrefixes.size());
                for (int n=0; n<namespacePrefixes.size(); n++) {
                     if (namespaceURIs.elementAt(n).equals("")) {
                         nsmap.remove(namespacePrefixes.elementAt(n));
                     } else {
                         nsmap.put(namespacePrefixes.elementAt(n),
                                 namespaceURIs.elementAt(n));
                     }
                }
                for (Iterator it = nsmap.keySet().iterator(); it.hasNext();) {
                    String prefix = (String)it.next();
                    super.startPrefixMapping(prefix, (String)nsmap.get(prefix));
                }
            }
        } else {
            activeDepth++;
        }
        if (activeDepth>0) {
            super.startElement(localName, uri, qname, atts);
        }
    }
    
    public void endElement(String localName, String uri, String qname)
    throws SAXException {
        // System.err.println("End element " + qname + " at " + activeDepth);
        if (activeDepth > 0) {
            super.endElement(localName, uri, qname);
            activeDepth--;
            if (activeDepth==0) {
                for (int n=namespacePrefixes.size()-1; n>=0; n--) {
                    super.endPrefixMapping(
                            (String)namespacePrefixes.elementAt(n) );
                }
            }
        }
    }    

    public void characters(char[] chars, int start, int len)
    throws SAXException {
        if (activeDepth > 0) {
            super.characters(chars, start, len);
        }
    }
        
    public void ignorableWhitespace(char[] chars, int start, int len)
    throws SAXException {
        if (activeDepth > 0) {
            super.ignorableWhitespace(chars, start, len);
        }
    }
    
    public void processingInstruction(String name, String data)
    throws SAXException {
        if (activeDepth > 0) {
            super.processingInstruction(name, data);
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
