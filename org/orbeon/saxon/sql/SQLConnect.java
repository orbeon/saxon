package org.orbeon.saxon.sql;

import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.style.ExtensionInstruction;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.ObjectValue;
import org.orbeon.saxon.value.StringValue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.regex.Pattern;

/**
* An sql:connect element in the stylesheet.
*/

public class SQLConnect extends ExtensionInstruction {

    Expression database;
    Expression driver;
    Expression user;
    Expression password;

    public boolean mayContainSequenceConstructor() {
        return false;
    }

    public void prepareAttributes() throws XPathException {

        // Get mandatory database attribute

        String dbAtt = getAttributeValue("", "database");
        if (dbAtt==null) {
            reportAbsence("database");
            dbAtt = ""; // for error recovery
        }
        database = makeAttributeValueTemplate(dbAtt);

	    // Get driver attribute

        String dbDriver = getAttributeValue("", "driver");
        if (dbDriver==null) {
            if (dbAtt.length()>9 && dbAtt.substring(0,9).equals("jdbc:odbc")) {
                dbDriver = "sun.jdbc.odbc.JdbcOdbcDriver";
            } else {
                reportAbsence("driver");
            }
        }
        driver = makeAttributeValueTemplate(dbDriver);


        // Get and expand user attribute, which defaults to empty string

        String userAtt = getAttributeValue("", "user");
        if (userAtt==null) {
            user = new StringLiteral(StringValue.EMPTY_STRING);
        } else {
            user = makeAttributeValueTemplate(userAtt);
        }

        // Get and expand password attribute, which defaults to empty string

        String pwdAtt = getAttributeValue("", "password");
        if (pwdAtt==null) {
            password = new StringLiteral(StringValue.EMPTY_STRING);
        } else {
            password = makeAttributeValueTemplate(pwdAtt);
        }
    }

    public void validate() throws XPathException {
        super.validate();
        database = typeCheck("database", database);
        driver = typeCheck("driver", driver);
        user = typeCheck("user", user);
        password = typeCheck("password", password);
    }

    public Expression compile(Executable exec) throws XPathException {
        return new ConnectInstruction(database, driver, user, password);
    }

    private static class ConnectInstruction extends SimpleExpression {

        public static final int DATABASE = 0;
        public static final int DRIVER = 1;
        public static final int USER = 2;
        public static final int PASSWORD = 3;

        public ConnectInstruction(Expression database,
            Expression driver, Expression user, Expression password) {

            Expression[] subs = {database, driver, user, password};
            setArguments(subs);
        }

        /**
         * A subclass must provide one of the methods evaluateItem(), iterate(), or process().
         * This method indicates which of the three is provided.
         */

        public int getImplementationMethod() {
            return Expression.EVALUATE_METHOD;
        }

        public int computeCardinality() {
            return StaticProperty.EXACTLY_ONE;
        }

        public String getExpressionType() {
            return "sql:connect";
        }

        public Item evaluateItem(XPathContext context) throws XPathException {

            // Establish the JDBC connection

            Connection connection = null;      // JDBC Database Connection

            String dbString = arguments[DATABASE].evaluateAsString(context).toString();
    	    String dbDriverString = arguments[DRIVER].evaluateAsString(context).toString();
            String userString = arguments[USER].evaluateAsString(context).toString();
            String pwdString = arguments[PASSWORD].evaluateAsString(context).toString();

            try {
                // the following hack is necessary to load JDBC drivers
    	        Class.forName(dbDriverString);
                connection = DriverManager.getConnection(dbString, userString, pwdString);
            } catch (Exception ex) {
                dynamicError("JDBC Connection Failure: " + ex.getMessage(), SaxonErrorCode.SXSQ0003, context);
            }

            return new ObjectValue(connection);

        }
    }

    /**
     * Utility method to quote a SQL table or column name if it needs quoting.
     * @param name the supplied name
     * @return the supplied name, enclosed in double quotes if it does not satisfy the pattern [A-Za-z_][A-Za-z0-9_]*,
     * with any double quotes replaced by two double quotes
     */

    public static String quoteSqlName(String name) throws IllegalArgumentException {
        // TODO: allow an embedded double-quote to be escaped as two double-quotes
        if (namePattern.matcher(name).matches()) {
            return name;
        }
        return "\"" + name + "\"";
    }

    private static Pattern namePattern = Pattern.compile("\"[^\"]+\"|[A-Za-z_][A-Za-z0-9_]*");
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
// Additional Contributor(s): Rick Bonnett [rbonnett@acadia.net]
//
