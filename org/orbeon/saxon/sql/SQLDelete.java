package org.orbeon.saxon.sql;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.SimpleExpression;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.style.ExtensionInstruction;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.value.ObjectValue;
import org.orbeon.saxon.value.StringValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
* An sql:delete element in the stylesheet.
*  @author Mathias Payer <mathias.payer@nebelwelt.net>
*  @author Michael Kay
* <p/>
* For example:
* <pre>
*   &lt;sql:delete connection="{$connection}" table="table-name" where="{$where}"
*                 xsl:extension-element-prefixes="sql" /&gt;
* <p/>
* </pre>
*/

public class SQLDelete extends ExtensionInstruction {

    Expression connection;
    String table;
    Expression where;

    public void prepareAttributes() throws XPathException {

		table = getAttributeList().getValue("", "table");
		if (table==null) {
            reportAbsence("table");
        }

        String dbWhere = getAttributeList().getValue("", "where");
        if (dbWhere == null) {
            where = StringValue.EMPTY_STRING;
        } else {
            where = makeAttributeValueTemplate(dbWhere);
        }


        String connectAtt = getAttributeList().getValue("", "connection");
        if (connectAtt==null) {
            reportAbsence("connection");
        } else {
            connection = makeExpression(connectAtt);
        }
    }

    public void validate() throws XPathException {
        super.validate();
        where = typeCheck("where", where);
        connection = typeCheck("connection", connection);
    }

    public Expression compile(Executable exec) throws XPathException {
        return new DeleteInstruction(connection, "DELETE FROM " + table, where);
    }

    private static class DeleteInstruction extends SimpleExpression {

		private static final long serialVersionUID = -4234440812734827279L;
		public static final int CONNECTION = 0;
		public static final int WHERE = 1;
        String statement;

        public DeleteInstruction(Expression connection, String statement, Expression where) {
            Expression[] sub = new Expression[2];
            sub[CONNECTION] = connection;
            sub[WHERE] = where;
            this.statement = statement;
            setArguments(sub);
        }

        /**
         * A subclass must provide one of the methods evaluateItem(), iterate(), or process().
         * This method indicates which of the three is provided.
         */

        public int getImplementationMethod() {
            return Expression.EVALUATE_METHOD;
        }

        public String getExpressionType() {
            return "sql:delete";
        }

        public Item evaluateItem(XPathContext context) throws XPathException {

            // Prepare the SQL statement (only do this once)

            Item conn = arguments[CONNECTION].evaluateItem(context);
            if (!(conn instanceof ObjectValue && ((ObjectValue)conn).getObject() instanceof Connection) ) {
                dynamicError("Value of connection expression is not a JDBC Connection", SaxonErrorCode.SXSQ0001, context);
            }
            Connection connection = (Connection)((ObjectValue)conn).getObject();
            PreparedStatement ps = null;

            String dbWhere = arguments[WHERE].evaluateAsString(context);
            String localstmt = statement;

            if (!dbWhere.equals("")) {
            	localstmt += " WHERE " + dbWhere;
            }

            try {
              	ps=connection.prepareStatement(localstmt);

    			ps.executeUpdate();
    			if (!connection.getAutoCommit()) {
                    connection.commit();
                }

    	    } catch (SQLException ex) {
    			dynamicError("SQL DELETE failed: " + ex.getMessage(), SaxonErrorCode.SXSQ0004, context);
            } finally {
               if (ps != null) {
                   try {
                       ps.close();
                   } catch (SQLException ignore) {}
               }
            }

            return null;
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
// Original code in SQLInsert.java by Michael Kay
// Adaption to SQLDelete.java by Mathias Payer <mathias.payer@nebelwelt.net>