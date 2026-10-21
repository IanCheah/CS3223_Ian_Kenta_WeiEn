package simpledb.plan;

import simpledb.tx.Transaction;
import simpledb.parse.*;
import simpledb.record.Schema;
import simpledb.metadata.MetadataMgr;

/**
 * The object that executes SQL statements.
 * @author Edward Sciore
 */
public class Planner {
   private QueryPlanner qplanner;
   private UpdatePlanner uplanner;
   private MetadataMgr metadataMgr;
   
   public Planner(QueryPlanner qplanner, UpdatePlanner uplanner, MetadataMgr metadataMgr) {
      this.qplanner = qplanner;
      this.uplanner = uplanner;
      this.metadataMgr = metadataMgr;
   }
   
   /**
    * Creates a plan for an SQL select statement, using the supplied planner.
    * @param qry the SQL query string
    * @param tx the transaction
    * @return the scan corresponding to the query plan
    */
   public Plan createQueryPlan(String qry, Transaction tx) {
      Parser parser = new Parser(qry);
      QueryData data = parser.query();
      verifyQuery(data, tx);
      return qplanner.createPlan(data, tx);
   }
   
   /**
    * Executes an SQL insert, delete, modify, or
    * create statement.
    * The method dispatches to the appropriate method of the
    * supplied update planner,
    * depending on what the parser returns.
    * @param cmd the SQL update string
    * @param tx the transaction
    * @return an integer denoting the number of affected records
    */
   public int executeUpdate(String cmd, Transaction tx) {
      Parser parser = new Parser(cmd);
      Object data = parser.updateCmd();
      verifyUpdate(data);
      if (data instanceof InsertData)
         return uplanner.executeInsert((InsertData)data, tx);
      else if (data instanceof DeleteData)
         return uplanner.executeDelete((DeleteData)data, tx);
      else if (data instanceof ModifyData)
         return uplanner.executeModify((ModifyData)data, tx);
      else if (data instanceof CreateTableData)
         return uplanner.executeCreateTable((CreateTableData)data, tx);
      else if (data instanceof CreateViewData)
         return uplanner.executeCreateView((CreateViewData)data, tx);
      else if (data instanceof CreateIndexData)
         return uplanner.executeCreateIndex((CreateIndexData)data, tx);
      else
         return 0;
   }
 
   // Verifies that every fields in predicates is a valid one
   private void verifyQuery(QueryData data, Transaction tx) {
	   // Verify that every field in the where clause must belong to one of the tables
	   Schema allFields = new Schema();
	   for (String tableName : data.tables()) {
		   Schema tmpSch = this.metadataMgr.getLayout(tableName, tx).schema();
		   allFields.addAll(tmpSch);
	   }

	   for (String fieldName : data.pred().fieldNames()) {
		   // Check that each fieldName mentioned exists
		   if (!allFields.hasField(fieldName)) {
			   throw new IllegalArgumentException("Unknown field '" + fieldName + "' in WHERE clause"
					      + " (if it is a string value, put it in quotes: '" + fieldName + "')");
		   }
	   }
	   
	   return;
   }

   // SimpleDB does not verify updates, although it should.
   private void verifyUpdate(Object data) {
   }
}
