package simpledb.plan;

import java.util.List;
import simpledb.tx.Transaction;
import simpledb.metadata.*;
import simpledb.query.Constant;
import simpledb.query.Scan;
import simpledb.record.*;

/** The Plan class corresponding to a table.
  * @author Edward Sciore
  */
public class TablePlan implements Plan {
   private String tblname;
   private Transaction tx;
   private Layout layout;
   private StatInfo si;
   
   /**
    * Creates a leaf node in the query tree corresponding
    * to the specified table.
    * @param tblname the name of the table
    * @param tx the calling transaction
    */
   public TablePlan(Transaction tx, String tblname, MetadataMgr md) {
      this.tblname = tblname;
      this.tx = tx;
      layout = md.getLayout(tblname, tx);
      si = md.getStatInfo(tblname, layout, tx);
   }
   
   /**
    * Creates a table scan for this query.
    * @see simpledb.plan.Plan#open()
    */
   public Scan open() {
      return new TableScan(tx, tblname, layout);
   }
   
   /**
    * Estimates the number of block accesses for the table,
    * which is obtainable from the statistics manager.
    * @see simpledb.plan.Plan#blocksAccessed()
    */ 
   public int blocksAccessed() {
      return si.blocksAccessed();
   }
   
   /**
    * Estimates the number of records in the table,
    * which is obtainable from the statistics manager.
    * @see simpledb.plan.Plan#recordsOutput()
    */
   public int recordsOutput() {
      return si.recordsOutput();
   }
   
   /**
    * Estimates the number of distinct field values in the table,
    * which is obtainable from the statistics manager.
    * @see simpledb.plan.Plan#distinctValues(java.lang.String)
    */
   public int distinctValues(String fldname) {
      return si.distinctValues(fldname);
   }

   /**
    * Returns the lowest value of the field, as recorded by the statistics manager.
    * @see simpledb.plan.Plan#minValue(java.lang.String)
    */
   public Constant minValue(String fldname) {
      return si.minValue(fldname);
   }

   /**
    * Returns the highest value of the field, as recorded by the statistics manager.
    * @see simpledb.plan.Plan#maxValue(java.lang.String)
    */
   public Constant maxValue(String fldname) {
      return si.maxValue(fldname);
   }
   
   /**
    * Determines the schema of the table,
    * which is obtainable from the catalog manager.
    * @see simpledb.plan.Plan#schema()
    */
   public Schema schema() {
      return layout.schema();
   }

   /**
    * Describes this table by its name, since base tables are
    * named as inputs rather than printed as steps.
    * @see simpledb.plan.Plan#describe(java.util.List)
    */
   public String describe(List<String> inputs) {
      return tblname;
   }
}
