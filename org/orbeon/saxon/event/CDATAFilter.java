package net.sf.saxon.event;
import net.sf.saxon.charcode.CharacterSet;
import net.sf.saxon.charcode.CharacterSetFactory;
import net.sf.saxon.om.XMLChar;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.OutputKeys;
import java.nio.CharBuffer;
import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;

/**
* CDATAFilter: This ProxyEmitter converts character data to CDATA sections,
* if the character data belongs to one of a set of element types to be handled this way.
*
* @author Michael Kay
*/


public class CDATAFilter extends ProxyReceiver {

    private FastStringBuffer buffer = new FastStringBuffer(256);
    private Stack stack = new Stack();
    private int[] nameList;             // fingerprints of cdata elements
    private CharacterSet characterSet;

    /**
    * Set the properties for this CDATA filter
    */

    public void setOutputProperties (Properties details)
    throws XPathException {
        nameList = getCdataElements(details);
        characterSet = CharacterSetFactory.getCharacterSet(details);
    }

    /**
    * Output element start tag
    */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        flush(buffer);
        stack.push(new Integer(nameCode & 0xfffff));
        super.startElement(nameCode, typeCode, locationId, properties);
    }

    /**
    * Output element end tag
    */

    public void endElement() throws XPathException {
        flush(buffer);
        stack.pop();
        super.endElement();
    }

    /**
    * Output a processing instruction
    */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        flush(buffer);
        super.processingInstruction(target, data, locationId, properties);
    }

    /**
    * Output character data
    */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {

        if ((properties & ReceiverOptions.DISABLE_ESCAPING) == 0) {
            buffer.append(chars.toString());
        } else {
            // if the user requests disable-output-escaping, this overrides the CDATA request. We end
            // the CDATA section and output the characters as supplied.
            flush(buffer);
            super.characters(chars, locationId, properties);
        }
    }

    /**
    * Output a comment
    */

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        flush(buffer);
        super.comment(chars, locationId, properties);
    }


    /**
    * Flush the buffer containing accumulated character data,
    * generating it as CDATA where appropriate
    */

    public void flush(FastStringBuffer buffer) throws XPathException {
        boolean cdata;
        int end = buffer.length();
        if (end==0) return;

        if (stack.isEmpty()) {
            cdata = false;      // text is not part of any element
        } else {
            int fprint = ((Integer)stack.peek()).intValue();
            cdata = isCDATA(fprint);
        }

        if (cdata) {

            // Check that the buffer doesn't include a character not available in the current
            // encoding

            int start = 0;
            int k = 0;
            while ( k < end ) {
                int next = buffer.charAt(k);
                int skip = 1;
                if (XMLChar.isHighSurrogate((char)next)) {
                    next = XMLChar.supplemental((char)next, buffer.charAt(k+1));
                    skip = 2;
                }
                if (characterSet.inCharset(next)) {
                    k++;
                } else {

                    // flush out the preceding characters as CDATA

                    char[] array = new char[k-start];
                    buffer.getChars(start, k, array, 0);
                    flushCDATA(array, k-start);

                    while (k < end) {
                        // output consecutive non-encodable characters
                        // before restarting the CDATA section
                        super.characters(CharBuffer.wrap(buffer, k, k+skip), 0, 0);
                                // was: (..., ReceiverOptions.DISABLE_ESCAPING);
                        k += skip;
                        next = buffer.charAt(k);
                        skip = 1;
                        if (XMLChar.isHighSurrogate((char)next)) {
                            next = XMLChar.supplemental((char)next, buffer.charAt(k+1));
                            skip = 2;
                        }
                        if (characterSet.inCharset(next)) {
                            break;
                        }
                    }
                    start=k;
                }
            }
            char[] rest = new char[end-start];
            buffer.getChars(start, end, rest, 0);
            flushCDATA(rest, end-start);

        } else {
            char[] array = new char[end];
            buffer.getChars(0, end, array, 0);
            super.characters(CharBuffer.wrap(array, 0, end), 0, 0);
        }

        buffer.setLength(0);

    }

    /**
    * Output an array as a CDATA section. At this stage we have checked that all the characters
    * are OK, but we haven't checked that there is no "]]>" sequence in the data
    */

    private void flushCDATA(char[] array, int len) throws XPathException {
        super.characters("<![CDATA[", 0, ReceiverOptions.DISABLE_ESCAPING);

        // Check that the character data doesn't include the substring "]]>"

        int i=0;
        int doneto=0;
        while (i<len-2) {
            if (array[i]==']' && array[i+1]==']' && array[i+2]=='>') {
                super.characters(CharBuffer.wrap(array, doneto, i+2-doneto), 0, ReceiverOptions.DISABLE_ESCAPING);
                super.characters("]]><![CDATA[", 0, ReceiverOptions.DISABLE_ESCAPING);
                doneto=i+2;
            }
            i++;
        }
        super.characters(CharBuffer.wrap(array, doneto, len-doneto), 0, ReceiverOptions.DISABLE_ESCAPING);
        super.characters("]]>", 0, ReceiverOptions.DISABLE_ESCAPING);
    }


    /**
    * See if a particular element is a CDATA element
    */

    private boolean isCDATA(int fingerprint) {
        for (int i=0; i<nameList.length; i++) {
            if (nameList[i]==fingerprint) return true;
        }
		return false;
	}

    /**
    * Extract the list of CDATA elements from the output properties
    */

    private int[] getCdataElements(Properties details) {
        String cdata = details.getProperty(OutputKeys.CDATA_SECTION_ELEMENTS);
        if (cdata==null) {
            return new int[0];
        }
        // first count the number of names in the list
        int count=0;
        StringTokenizer st1 = new StringTokenizer(cdata);
        while (st1.hasMoreTokens()) {
            st1.nextToken();
            count++;
        }
        int[] array = new int[count];
        count = 0;
        StringTokenizer st2 = new StringTokenizer(cdata);
        while (st2.hasMoreTokens()) {
            String expandedName = st2.nextToken();
            array[count++] = getNamePool().getFingerprintForExpandedName(expandedName);
        }
        return array;
    }

};

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

