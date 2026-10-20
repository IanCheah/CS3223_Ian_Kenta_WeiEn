package simpledb.plan;

import java.util.Collections;
import java.util.List;

import simpledb.query.Constant;
import simpledb.query.Scan;
import simpledb.record.*;

/**
 * The interface implemented by each query plan.
 * There is a Plan class for each relational algebra operator.
 * @author Edward Sciore
 *
 */
public interface Plan {
   
   /**
    * Opens a scan corresponding to this plan.
    * The scan will be positioned before its first record.
    * @return a scan
    */
   public Scan   open();
   
   /**
    * Returns an estimate of the number of block accesses
    * that will occur when the scan is read to completion.
    * @return the estimated number of block accesses
    */
   public int    blocksAccessed();
   
   /**
    * Returns an estimate of the number of records
    * in the query's output table.
    * @return the estimated number of output records
    */
   public int    recordsOutput();
   
   /**
    * Returns an estimate of the number of distinct values
    * for the specified field in the query's output table.
    * @param fldname the name of a field
    * @return the estimated number of distinct field values in the output
    */
   public int    distinctValues(String fldname);
   
   /**
    * Returns the schema of the query.
    * @return the query's schema
    */
   public Schema schema();

   /**
    * Returns the lowest value of the specified field,
    * or null if the plan does not know it.
    * Used to estimate the selectivity of range predicates.
    * @param fldname the name of a field
    * @return the lowest value of the field, or null
    */
   default Constant minValue(String fldname) {
      return null;
   }

   /**
    * Returns the highest value of the specified field,
    * or null if the plan does not know it.
    * @param fldname the name of a field
    * @return the highest value of the field, or null
    */
   default Constant maxValue(String fldname) {
      return null;
   }

   /**
    * Returns a one-sentence description of this operator as a step
    * of the query plan, e.g. "Select: keep rows of student where gradyear = 2020".
    * See PlanPrinter.
    * @param inputs how to refer to each child plan, in the order of
    *        children(), e.g. "student" or "result of step 2"
    *        (null for a base table, which describes itself by name)
    * @return the description of this step
    */
   default String describe(List<String> inputs) {
      return getClass().getSimpleName() + ": combine " + PlanPrinter.list(inputs);
   }

   /**
    * Returns the input plans of this operator,
    * for printing the query plan.
    * @return the child plans (empty for a leaf)
    */
   default List<Plan> children() {
      return Collections.emptyList();
   }
}
