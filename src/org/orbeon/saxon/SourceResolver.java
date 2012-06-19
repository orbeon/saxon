package org.orbeon.saxon;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.Source;


/**
 * This interface defines a SourceResolver. A SourceResolver can be registered as
 * part of the Configuration, and enables new kinds of Source to be recognized
 * beyond those that are natively recognized by Saxon.
 * <p>
 * The task of the SourceResolver is to take any Source as input, and to return
 * a Source that has native support in Saxon: that is, one of the classes
 * StreamSource, SAXSource, DOMSource, {@link org.orbeon.saxon.om.NodeInfo},
 * or {@link org.orbeon.saxon.pull.PullSource}
 * @author Michael H. Kay
*/

public interface SourceResolver {

    /**
     * Resolve a Source.
     * @param source A source object, typically the source supplied as the first
     * argument to {@link javax.xml.transform.Transformer#transform(javax.xml.transform.Source, javax.xml.transform.Result)}
     * or similar methods.
     * @param config The Configuration. This provides the SourceResolver with access to
     * configuration information; it also allows the SourceResolver to invoke the
     * resolveSource() method on the Configuration object as a fallback implementation.
     * @return a source object that Saxon knows how to process. This must be an instance of one
     * of the classes  StreamSource, SAXSource, DOMSource, {@link org.orbeon.saxon.AugmentedSource},
     *  {@link org.orbeon.saxon.om.NodeInfo},
     * or {@link org.orbeon.saxon.pull.PullSource}. Return null if the Source object is not
     * recognized
     * @throws XPathException if the Source object is recognized but cannot be processed
    */

    public Source resolveSource(Source source, Configuration config) throws XPathException;

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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
