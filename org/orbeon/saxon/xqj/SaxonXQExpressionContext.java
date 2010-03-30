package org.orbeon.saxon.xqj;

import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.query.QueryModule;
import org.orbeon.saxon.query.XQueryExpression;

import javax.xml.xquery.XQConstants;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQItemType;
import javax.xml.xquery.XQStaticContext;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This is a read-only implementation of the XQStaticContext interface that provides information about
 * the static context options selected within the query prolog of a compiled expression.
 *
 * <p>Note that it only provides information about the main module of the query, library modules have
 * a different static context and this is not available via the XQJ API.</p>
 */

public class SaxonXQExpressionContext implements XQStaticContext {

    private QueryModule main;

    public SaxonXQExpressionContext(XQueryExpression expression) {
        main = expression.getStaticContext();        
    }


    public void declareNamespace(String prefix, String uri) throws XQException {
        readOnly();
    }

    public String getBaseURI() {
        return main.getBaseURI();
    }

    public int getBindingMode() {
        return XQConstants.BINDING_MODE_IMMEDIATE;
    }

    public int getBoundarySpacePolicy() {
        return main.isPreserveBoundarySpace() ?
               XQConstants.BOUNDARY_SPACE_PRESERVE :
               XQConstants.BOUNDARY_SPACE_STRIP;
    }

    public int getConstructionMode() {
        return main.getConstructionMode() == Validation.PRESERVE ?
               XQConstants.CONSTRUCTION_MODE_PRESERVE :
               XQConstants.CONSTRUCTION_MODE_STRIP;
    }

    public XQItemType getContextItemStaticType() {
        return new SaxonXQItemType(main.getUserQueryContext().getRequiredContextItemType(),
                main.getConfiguration());
    }

    public int getCopyNamespacesModeInherit() {
        return (main.isInheritNamespaces() ?
                XQConstants.COPY_NAMESPACES_MODE_INHERIT :
                XQConstants.COPY_NAMESPACES_MODE_NO_INHERIT);
    }

    public int getCopyNamespacesModePreserve() {
        return (main.isPreserveNamespaces() ?
                XQConstants.COPY_NAMESPACES_MODE_PRESERVE :
                XQConstants.COPY_NAMESPACES_MODE_NO_PRESERVE);
    }

    public String getDefaultCollation() {
        return main.getDefaultCollationName();
    }

    public String getDefaultElementTypeNamespace() {
        return main.getDefaultElementNamespace();
    }

    public String getDefaultFunctionNamespace() {
        return main.getDefaultFunctionNamespace();
    }

    public int getDefaultOrderForEmptySequences() {
        return main.isEmptyLeast() ?
               XQConstants.DEFAULT_ORDER_FOR_EMPTY_SEQUENCES_LEAST :
               XQConstants.DEFAULT_ORDER_FOR_EMPTY_SEQUENCES_GREATEST;
    }

    public int getHoldability() {
        return XQConstants.HOLDTYPE_HOLD_CURSORS_OVER_COMMIT;
    }

    public String[] getNamespacePrefixes() {
        Iterator iter = main.getUserQueryContext().iterateDeclaredPrefixes();
        ArrayList list = new ArrayList(20);
        while (iter.hasNext()) {
            list.add(iter.next());
        }
        String[] result = new String[list.size()];
        for (int i=0; i<result.length; i++) {
            result[i] = (String)list.get(i);
        }
        return result;
    }

    public String getNamespaceURI(String prefix) throws XQException {
        String uri = main.checkURIForPrefix(prefix);
        if (uri == null) {
            throw new XQException("Unknown prefix");
        }
        return uri;
    }

    public int getOrderingMode() {
        return XQConstants.ORDERING_MODE_ORDERED;
    }

    public int getQueryLanguageTypeAndVersion() {
        return XQConstants.LANGTYPE_XQUERY;
    }

    public int getQueryTimeout() {
        return 0;
    }

    public int getScrollability() {
        return XQConstants.SCROLLTYPE_SCROLLABLE;
    }

    public void setBaseURI(String baseUri) throws XQException {
        readOnly();
    }

    public void setBindingMode(int bindingMode) throws XQException {
        readOnly();
    }

    public void setBoundarySpacePolicy(int policy) throws XQException {
        readOnly();
    }

    public void setConstructionMode(int mode) throws XQException {
        readOnly();
    }

    public void setContextItemStaticType(XQItemType contextItemType) {
        //readOnly();
    }

    public void setCopyNamespacesModeInherit(int mode) throws XQException {
        readOnly();
    }

    public void setCopyNamespacesModePreserve(int mode) throws XQException {
        readOnly();
    }

    public void setDefaultCollation(String uri) throws XQException {
        readOnly();
    }

    public void setDefaultElementTypeNamespace(String uri) throws XQException {
        readOnly();
    }

    public void setDefaultFunctionNamespace(String uri) throws XQException {
        readOnly();
    }

    public void setDefaultOrderForEmptySequences(int order) throws XQException {
        readOnly();
    }

    public void setHoldability(int holdability) throws XQException {
        readOnly();
    }

    public void setOrderingMode(int mode) throws XQException {
        readOnly();
    }

    public void setQueryLanguageTypeAndVersion(int langType) throws XQException {
        readOnly();
    }

    public void setQueryTimeout(int seconds) throws XQException {
        readOnly();
    }

    public void setScrollability(int scrollability) throws XQException {
        readOnly();
    }

    private void readOnly() throws XQException {
        throw new XQException("This XQStaticContext object is read-only");
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

