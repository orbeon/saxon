package org.orbeon.saxon.sql;

import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.ReceiverOptions;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.SimpleExpression;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.style.ExtensionInstruction;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.ObjectValue;
import org.orbeon.saxon.value.StringValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * An sql:query element in the stylesheet.
 * <p/>
 * For example:
 * <pre>
 *   &lt;sql:query column="{$column}" table="{$table}" where="{$where}"
 *                 xsl:extension-element-prefixes="sql"/ &gt;
 * <p/>
 * </pre>
 * (result with HTML-table-output) <BR>
 * <pre>
 *   &lt;sql:query column="{$column}" table="{$table}" where="{$where}"
 *                 row-tag="TR" column-tag="TD"
 *                 separatorType="tag"
 *                 xsl:extension-element-prefixes="sql"/ &gt;
 * </pre>
 *
 * @author claudio.thomas@unix-ag.org (based on Michael Kay's SQLInsert.java)
 */

public class SQLQuery extends ExtensionInstruction {

    Expression connection;
    /**
     * selected column(s) to query
     */
    Expression column;
    /**
     * the table(s) to query in
     */
    Expression table;
    /**
     * conditions of query (can be omitted)
     */
    Expression where;

    String rowTag;
    /**
     * name of element to hold the rows
     */
    String colTag;
    /**
     * name of element to hold the columns
     */

    boolean disable = false;    // true means disable-output-escaping="yes"

    public void prepareAttributes() throws XPathException {
        // Attributes for SQL-statement
        String dbCol = attributeList.getValue("", "column");
        if (dbCol == null) {
            reportAbsence("column");
        }
        column = makeAttributeValueTemplate(dbCol);

        String dbTab = attributeList.getValue("", "table");
        if (dbTab == null) {
            reportAbsence("table");
        }
        table = makeAttributeValueTemplate(dbTab);

        String dbWhere = attributeList.getValue("", "where");
        if (dbWhere == null) {
            where = StringValue.EMPTY_STRING;
        } else {
            where = makeAttributeValueTemplate(dbWhere);
        }

        String connectAtt = attributeList.getValue("", "connection");
        if (connectAtt == null) {
            reportAbsence("connection");
        } else {
            connection = makeExpression(connectAtt);
        }

        // Atributes for row & column element names

        rowTag = attributeList.getValue("", "row-tag");
        if (rowTag == null) {
            rowTag = "row";
        }
        if (rowTag.indexOf(':') >= 0) {
            compileError("rowTag must not contain a colon");
        }

        colTag = attributeList.getValue("", "column-tag");
        if (colTag == null) {
            colTag = "col";
        }
        if (colTag.indexOf(':') >= 0) {
            compileError("colTag must not contain a colon");
        }
        // Attribute output-escaping
        String disableAtt = attributeList.getValue("", "disable-output-escaping");
        if (disableAtt != null) {
            if (disableAtt.equals("yes")) {
                disable = true;
            } else if (disableAtt.equals("no")) {
                disable = false;
            } else {
                compileError("disable-output-escaping attribute must be either yes or no");
            }
        }

    }

    public void validate() throws XPathException {
        super.validate();
        column = typeCheck("column", column);
        table = typeCheck("table", table);
        where = typeCheck("where", where);
        connection = typeCheck("connection", connection);
    }

    public Expression compile(Executable exec) throws XPathException {
        QueryInstruction inst = new QueryInstruction(connection,
                column, table, where,
                rowTag, colTag, disable);
        return inst;
    }

    private static class QueryInstruction extends SimpleExpression {

        public static final int CONNECTION = 0;
        public static final int COLUMN = 1;
        public static final int TABLE = 2;
        public static final int WHERE = 3;
        String rowTag;
        String colTag;
        int options;

        public QueryInstruction(Expression connection,
                                Expression column,
                                Expression table,
                                Expression where,
                                String rowTag,
                                String colTag,
                                boolean disable) {
            Expression[] sub = {connection, column, table, where};
            setArguments(sub);
            this.rowTag = rowTag;
            this.colTag = colTag;
            this.options = (disable ? ReceiverOptions.DISABLE_ESCAPING : 0);
        }

        /**
         * A subclass must provide one of the methods evaluateItem(), iterate(), or process().
         * This method indicates which of the three is provided.
         */

        public int getImplementationMethod() {
            return Expression.PROCESS_METHOD;
        }

        public String getExpressionType() {
            return "sql:query";
        }

        public void process(XPathContext context) throws XPathException {
            // Prepare the SQL statement (only do this once)

            Controller controller = context.getController();
            Item conn = arguments[CONNECTION].evaluateItem(context);
            if (!(conn instanceof ObjectValue && ((ObjectValue)conn).getObject() instanceof Connection)) {
                DynamicError de = new DynamicError("Value of connection expression is not a JDBC Connection");
                de.setXPathContext(context);
                throw de;
            }
            Connection connection = (Connection)((ObjectValue)conn).getObject();

            String dbCol = arguments[COLUMN].evaluateAsString(context);
            String dbTab = arguments[TABLE].evaluateAsString(context);
            String dbWhere = arguments[WHERE].evaluateAsString(context);


            NamePool pool = controller.getNamePool();
            int rowCode = pool.allocate("", "", rowTag);
            int colCode = pool.allocate("", "", colTag);

            PreparedStatement ps = null;
            ResultSet rs = null;
            DynamicError de = null;

            try {
                StringBuffer statement = new StringBuffer();
                statement.append("SELECT " + dbCol + " FROM " + dbTab);
                if (dbWhere != "") {
                    statement.append(" WHERE " + dbWhere);
                }
                //System.err.println("-> SQL: " + statement.toString());

                // -- Prepare the SQL statement
                ps = connection.prepareStatement(statement.toString());
                controller.setUserData(this, "sql:statement", ps);

                // -- Execute Statement
                rs = ps.executeQuery();

                // -- Print out Result
                Receiver out = context.getReceiver();
                String result = "";
                int icol = rs.getMetaData().getColumnCount();
                while (rs.next()) {                            // next row
                    //System.out.print("<- SQL : "+ rowStart);
                    out.startElement(rowCode, StandardNames.XDT_UNTYPED, locationId, 0);
                    for (int col = 1; col <= icol; col++) {     // next column
                        // Read result from RS only once, because
                        // of JDBC-Specifications
                        result = rs.getString(col);
                        out.startElement(colCode, StandardNames.XDT_UNTYPED, locationId, 0);
                        if (result != null) {
                            out.characters(result, locationId, options);
                        }
                        out.endElement();
                    }
                    //System.out.println(rowEnd);
                    out.endElement();
                }
                //rs.close();

                if (!connection.getAutoCommit()) {
                    connection.commit();
                }

            } catch (SQLException ex) {
                de = new DynamicError("(SQL) " + ex.getMessage());
                de.setXPathContext(context);
                throw de;
            } finally {
                boolean wasDEThrown = (de != null);
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException ex) {
                        de = new DynamicError("(SQL) " + ex.getMessage());
                        de.setXPathContext(context);
                    }
                }
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (SQLException ex) {
                        de = new DynamicError("(SQL) " + ex.getMessage());
                        de.setXPathContext(context);
                    }
                }
                if (!wasDEThrown && de != null) {
                    throw de; // test so we don't lose the real exception
                }
            }
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
//
// Contributor(s): claudio.thomas@unix-ag.org (based on SQLInsert.java)
//
