package net.sf.saxon.sql;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.ExtensionInstruction;
import net.sf.saxon.om.Item;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.xpath.XPathException;

import javax.xml.transform.TransformerConfigurationException;
import java.sql.Connection;
import java.sql.DriverManager;

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

    public void prepareAttributes() throws TransformerConfigurationException {

        // Get mandatory database attribute

        String dbAtt = attributeList.getValue("", "database");
        if (dbAtt==null) {
            reportAbsence("database");
            dbAtt = ""; // for error recovery
        }
        database = makeAttributeValueTemplate(dbAtt);

	    // Get driver attribute

        String dbDriver = attributeList.getValue("", "driver");
        if (dbDriver==null) {
            if (dbAtt.length()>9 && dbAtt.substring(0,9).equals("jdbc:odbc")) {
                dbDriver = "sun.jdbc.odbc.JdbcOdbcDriver";
            } else {
                reportAbsence("driver");
            }
        }
        driver = makeAttributeValueTemplate(dbDriver);


        // Get and expand user attribute, which defaults to empty string

        String userAtt = attributeList.getValue("", "user");
        if (userAtt==null) {
            user = StringValue.EMPTY_STRING;
        } else {
            user = makeAttributeValueTemplate(userAtt);
        }

        // Get and expand password attribute, which defaults to empty string

        String pwdAtt = attributeList.getValue("", "password");
        if (pwdAtt==null) {
            password = StringValue.EMPTY_STRING;
        } else {
            password = makeAttributeValueTemplate(pwdAtt);
        }
    }

    public void validate() throws TransformerConfigurationException {
        super.validate();
        database = typeCheck("database", database);
        driver = typeCheck("driver", driver);
        user = typeCheck("user", user);
        password = typeCheck("password", password);
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
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
        };

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

            String dbString = arguments[DATABASE].evaluateAsString(context);
    	    String dbDriverString = arguments[DRIVER].evaluateAsString(context);
            String userString = arguments[USER].evaluateAsString(context);
            String pwdString = arguments[PASSWORD].evaluateAsString(context);

            try {
                // the following hack is necessary to load JDBC drivers
    	        Class.forName(dbDriverString);
                connection = DriverManager.getConnection(dbString, userString, pwdString);
            } catch (Exception ex) {
                dynamicError("JDBC Connection Failure: " + ex.getMessage(), context);
            }

            return new ObjectValue(connection);

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
// Additional Contributor(s): Rick Bonnett [rbonnett@acadia.net]
//
