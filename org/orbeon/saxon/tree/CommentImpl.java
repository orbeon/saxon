package net.sf.saxon.tree;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;

/**
  * CommentImpl is an implementation of a Comment node
  * @author Michael H. Kay
  */


final class CommentImpl extends NodeImpl {

    String comment;

    public CommentImpl(String content) {
        this.comment = content;
    }

    public final String getStringValue() {
        return comment;
    }

    public final int getNodeKind() {
        return Type.COMMENT;
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
        out.comment(comment, locationId, 0);
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
