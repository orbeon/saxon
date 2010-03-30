package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Version;

import javax.xml.xquery.XQMetaData;
import javax.xml.xquery.XQException;
import java.util.Set;

/**
 * Saxon implementation of the XQMetaData interface
 */
public class SaxonXQMetaData implements XQMetaData {

    private SaxonXQConnection connection;

    /**
     * Create the metadata for a given Saxon configuration
     * @param connection the Saxon connection
     */

    public SaxonXQMetaData(SaxonXQConnection connection) {
        this.connection = connection;
    }

    public int getMaxExpressionLength() throws XQException {
        checkNotClosed();
        checkNotClosed();
        return Integer.MAX_VALUE;
    }

    public int getMaxUserNameLength()  throws XQException {
        checkNotClosed();
        return Integer.MAX_VALUE;
    }

    public int getProductMajorVersion() throws XQException  {
        checkNotClosed();
        return Version.getStructuredVersionNumber()[0];
    }

    public int getProductMinorVersion() throws XQException  {
        checkNotClosed();
        return Version.getStructuredVersionNumber()[1];
    }

    public String getProductName() throws XQException  {
        checkNotClosed();
        return Version.getProductName();
    }

    public String getProductVersion() throws XQException  {
        checkNotClosed();
        return Version.getProductVersion();
    }

    public Set getSupportedXQueryEncodings() throws XQException  {
        checkNotClosed();
        return java.nio.charset.Charset.availableCharsets().keySet();
    }

    public String getUserName() throws XQException  {
        checkNotClosed();
        return null;
    }

    public int getXQJMajorVersion() throws XQException  {
        checkNotClosed();
        return 0;
    }

    public int getXQJMinorVersion() throws XQException  {
        checkNotClosed();
        return 9;
    }

    public String getXQJVersion() throws XQException  {
        checkNotClosed();
        return "0.9";
    }

    public boolean isFullAxisFeatureSupported() throws XQException  {
        checkNotClosed();
        return true;
    }

    public boolean isModuleFeatureSupported()  throws XQException {
        checkNotClosed();
        return true;
    }

    public boolean isReadOnly()  throws XQException {
        checkNotClosed();
        return true;
    }

    public boolean isSchemaImportFeatureSupported() throws XQException  {
        checkNotClosed();
        return connection.getConfiguration().isSchemaAware(Configuration.XQUERY);
    }

    public boolean isSchemaValidationFeatureSupported() throws XQException  {
        checkNotClosed();
        return connection.getConfiguration().isSchemaAware(Configuration.XQUERY);
    }

    public boolean isSerializationFeatureSupported() throws XQException  {
        checkNotClosed();
        return true;
    }

    public boolean isStaticTypingExtensionsSupported()  throws XQException {
        checkNotClosed();
        return false;
    }

    public boolean isStaticTypingFeatureSupported() throws XQException  {
        checkNotClosed();
        return false;
    }

    public boolean isTransactionSupported()  throws XQException {
        checkNotClosed();
        return false;
    }

    public boolean isUserDefinedXMLSchemaTypeSupported()  throws XQException {
        checkNotClosed();
        return connection.getConfiguration().isSchemaAware(Configuration.XQUERY);
    }

    public boolean isXQueryEncodingDeclSupported()  throws XQException {
        checkNotClosed();
        return true;
    }

    public boolean isXQueryEncodingSupported(String encoding) throws XQException  {
        checkNotClosed();
        return getSupportedXQueryEncodings().contains(encoding);
    }

    public boolean isXQueryXSupported()  throws XQException {
        checkNotClosed();
        return false;
    }

    public boolean wasCreatedFromJDBCConnection() throws XQException  {
        checkNotClosed();
        return false;
    }

    private void checkNotClosed() throws XQException {
        connection.checkNotClosed();
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

