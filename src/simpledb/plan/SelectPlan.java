package simpledb.plan;

import java.util.Arrays;
import java.util.List;
import simpledb.query.Constant;
import simpledb.query.Predicate;
import simpledb.query.Scan;
import simpledb.query.SelectScan;
import simpledb.record.Schema;

/** The Plan class corresponding to the <i>select</i>
  * relational algebra operator.
  * @author Edward Sciore
  */
public class SelectPlan implements Plan {
   private Plan p;
   private Predicate pred;
   
   /**
    * Creates a new select node in the query tree,
    * having the specified subquery and predicate.
    * @param p the subquery
    * @param pred the predicate
    */
   public SelectPlan(Plan p, Predicate pred) {
      this.p = p;
      this.pred = pred;
   }
   
   /**
    * Creates a select scan for this query.
    * @see simpledb.plan.Plan#open()
    */
   public Scan open() {
      Scan s = p.open();
      return new SelectScan(s, pred);
   }
   
   /**
    * Estimates the number of block accesses in the selection,
    * which is the same as in the underlying query.
    * @see simpledb.plan.Plan#blocksAccessed()
    */
   public int blocksAccessed() {
      return p.blocksAccessed();
   }
   
   /**
    * Estimates the number of output records in the selection,
    * which is determined by the 
    * selectivity of the predicate.
    * @see simpledb.plan.Plan#recordsOutput()
    */
   public int recordsOutput() {
      return (int) Math.round(p.recordsOutput() * pred.selectivity(p));
   }
   
   /**
    * Estimates the number of distinct field values
    * in the projection.
    * If the predicate contains a term equating the specified 
    * field to a constant, then this value will be 1.
    * Otherwise, it will be the number of the distinct values
    * in the underlying query 
    * (but not more than the size of the output table).
    * @see simpledb.plan.Plan#distinctValues(java.lang.String)
    */
   public int distinctValues(String fldname) {
      int v;
      if (pred.equatesWithConstant(fldname) != null)
         return 1;
      else {
         String fldname2 = pred.equatesWithField(fldname);
         if (fldname2 != null)
            v = Math.min(p.distinctValues(fldname),
                         p.distinctValues(fldname2));
         else
            v = p.distinctValues(fldname);
      }
      // a field cannot have more distinct values than there are records
      return Math.max(1, Math.min(v, recordsOutput()));
   }

   public Constant minValue(String fldname) {
      return p.minValue(fldname);
   }

   public Constant maxValue(String fldname) {
      return p.maxValue(fldname);
   }
   
   /**
    * Returns the schema of the selection,
    * which is the same as in the underlying query.
    * @see simpledb.plan.Plan#schema()
    */
   public Schema schema() {
      return p.schema();
   }

   public String describe(List<String> inputs) {
      return "Select: keep rows of " + inputs.get(0) + " where " + pred;
   }

   public List<Plan> children() {
      return Arrays.asList(p);
   }
}
