package simpledb.materialize;

import simpledb.query.*;
import simpledb.record.Schema;

/**
 * The <i>count</i> aggregation function.
 * @author Edward Sciore
 */
public class CountFn implements AggregationFn {
   private String fldname;
   private int count;
   
   /**
    * Create a count aggregation function for the specified field.
    * @param fldname the name of the aggregated field
    */
   public CountFn(String fldname) {
      this.fldname = fldname;
   }
   
   public void reset() {
	   count = 0;
   }
   
   /**
    * Start a new count by calling reset
    * Since SimpleDB does not support null values,
    * every record will be counted,
    * regardless of the field.
    * @see simpledb.materialize.AggregationFn#processFirst(simpledb.query.Scan)
    */
   public void processFirst(Scan s) {
      reset();
      processNext(s);
   }
   
   /**
    * Since SimpleDB does not support null values,
    * this method always increments the count,
    * regardless of the field.
    * @see simpledb.materialize.AggregationFn#processNext(simpledb.query.Scan)
    */
   public void processNext(Scan s) {
      count++;
   }
   
   /**
    * Return the field's name, prepended by "countof".
    * @see simpledb.materialize.AggregationFn#fieldName()
    */
   public String fieldName() {
      return "countof" + fldname;
   }
   
   public String inputField() {
	   return fldname;
   }
   
   /**
    * Return the current count.
    * @see simpledb.materialize.AggregationFn#value()
    */
   public Constant value() {
      return new Constant(count);
   }
   
   /**
    * Validates that the input field exists and adds an INTEGER result
    * field to the output schema.
    *
    * @param input the schema containing the counted field
    * @param output the schema to receive the count field
    * @throws IllegalArgumentException if the input field does not exist
    */
   public void addResultField(Schema input, Schema output) {
       if (!input.hasField(fldname)) {
           throw new IllegalArgumentException(
               "Unknown aggregate field: " + fldname
           );
       }
       output.addIntField(fieldName());
   }

   public boolean hasValueForEmptyInput() {
	   return true;
   }
}
