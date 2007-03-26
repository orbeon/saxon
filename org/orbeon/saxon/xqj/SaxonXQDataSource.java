package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.javax.xml.xquery.XQCommonHandler;
import org.orbeon.saxon.javax.xml.xquery.XQConnection;
import org.orbeon.saxon.javax.xml.xquery.XQDataSource;
import org.orbeon.saxon.javax.xml.xquery.XQException;

import java.io.PrintWriter;
import java.sql.Connection;
import java.util.Properties;

/**
 * Saxon implementation of the XQJ XQDataSource interface. The first action of a client application
 * is to instantiate a SaxonXQDataSource. This is done directly: there is no factory class as with JAXP.
 * An application that does not want compile-time references to the Saxon XQJ implementation can instantiate
 * this class dynamically using the reflection API (class.newInstance()).
 * <p>
 * For full Javadoc descriptions of the public methods, see the XQJ specification.
 */
public class SaxonXQDataSource extends SaxonXQDataFactory implements XQDataSource {

    private Configuration config;
    private XQCommonHandler commonHandler;

    /**
     * Create a SaxonXQDataSource using a default configuration.
     * A schema-aware configuration will be created if Saxon-SA can be loaded from the
     * classpath; otherwise a basic (non-schema-aware) configuration is created
     */

    public SaxonXQDataSource() {
        try {
            config = Configuration.makeSchemaAwareConfiguration(null);
        } catch (RuntimeException err) {
            config = new Configuration();
        }
        commonHandler = new StandardDOMHandler(this);
    }

    /**
     * Create a Saxon XQDataSource with a specific configuration
     * @param config The Saxon configuration to be used
     */

    public SaxonXQDataSource(Configuration config) {
        this.config = config;
        commonHandler = new StandardDOMHandler(this);
    }

    /**
     * Get the configuration in use. Changes made to this configuration will affect this
     * data source and XQJ connections created from it (either before or afterwards).
     * @return the configuration in use.
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Get the XQCommonHandler in use. This controls the mapping between XPath data types
     * and Java types. If no XQCommonHandler has been supplied explicitly, a default handler
     * is used, which implements the default mappings as defined in the XQJ specification.
     * @return the XQCommonHandler in use.
     */

    public XQCommonHandler getCommonHandler() {
        return commonHandler;
    }

    public XQConnection getConnection() throws XQException {
        return new SaxonXQConnection(this);
    }

    /**
     * Get a connection based on an underlying JDBC connection
     * @param con the JDBC connection
     * @return a connection based on an underlying JDBC connection
     * @throws XQException The Saxon implementation of this method always throws
     * an XQException, indicating that Saxon does not support connection to a JDBC data source.
     */

    public XQConnection getConnection(Connection con) throws XQException {
        throw new XQException("Saxon cannot connect to a SQL data source");
    }

    /**
     * Get a connection, by supplying a username and password. The Saxon implementation of this is equivalent
     * to the default constructor: the username and password are ignored.
     * @param username
     * @param password
     * @return a connection
     * @throws XQException
     */

    public XQConnection getConnection(String username, String password) throws XQException {
        return getConnection();
    }

    public int getLoginTimeout() {
        return 0;
    }

    public PrintWriter getLogWriter() {
        return null;
    }

    public String getProperty(String name) throws XQException {
        return null;
    }

    public String[] getSupportedPropertyNames() {
        String[] names =
                {   "javax.xml.xquery.property.UserName",
                    "javax.xml.xquery.property.Password",
                    "javax.xml.xquery.property.MaxConnections" };
        return names;
    }

    public void setCommonHandler(XQCommonHandler handler) throws XQException {
        commonHandler = handler;
    }

    public void setLoginTimeout(int seconds) throws XQException {
        // no-op
    }

    public void setLogWriter(PrintWriter out) throws XQException {
        // no-op
    }

    public void setProperties(Properties props) throws XQException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setProperty(String name, String value) throws XQException {
        //To change body of implemented methods use File | Settings | File Templates.
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
// Contributor(s):
//