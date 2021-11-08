package org.orbeon.saxon.event;

import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.sort.IntHashSet;
import org.orbeon.saxon.trans.XPathException;


/**
  * XHTMLEmitter is an Emitter that generates XHTML output.
  * It is the same as XMLEmitter except that it follows the legacy HTML browser
  * compatibility rules: for example, generating empty elements such as [BR /], and
  * using [p][/p] for empty paragraphs rather than [p/]
  */

public class XHTMLEmitter extends XMLEmitter {

    /**
    * Table of XHTML tags that have no closing tag
    */

    IntHashSet emptyTags = new IntHashSet(31);

    private static String[] emptyTagNames = {
        "area", "base", "basefont", "br", "col", "frame", "hr", "img", "input", "isindex", "link", "meta", "param"
    };

    /**
     * Do the real work of starting the document. This happens when the first
     * content is written.
     *
     * @throws org.orbeon.saxon.trans.XPathException
     *
     */

    protected void openDocument() throws XPathException {
        NamePool pool = getPipelineConfiguration().getConfiguration().getNamePool();
        for (int i=0; i<emptyTagNames.length; i++) {
            emptyTags.add(pool.allocate("", NamespaceConstant.XHTML, emptyTagNames[i]) & NamePool.FP_MASK);
        }
        super.openDocument();
    }

    /**
    * Close an empty element tag.
    */

    protected String emptyElementTagCloser(String displayName, int nameCode) {
        if (emptyTags.contains(nameCode & NamePool.FP_MASK)) {
            return " />";
        } else {
            return "></" + displayName + '>';
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
