package simpledb.materialize;

import static java.sql.Types.*;

import simpledb.query.*;
import simpledb.record.Schema;

/**
 * The <i>average</i> aggregation function.
 * 
 */
public class AvgFn implements AggregationFn {
	
	private String fldname;
	private double total;
	private long count;
	
	/**
	 * Create a avg aggregation function for the specified field.
	 * @param fldname the name of the aggregated field
	 */
	public AvgFn(String fldname) {
		this.fldname = fldname;
	}
	
	
	
	public void reset() {
		total = 0.0;
		count = 0;
	}
   
   
   public void processFirst(Scan s) {
	   reset();
	   processNext(s);
	   
   }
   
   /**
    * Adds the current record's numeric value to the running total and
    * increments the record count.
    *
    * @param s the scan positioned on the record to process
    */
   public void processNext(Scan s) {
	    total += s.getVal(fldname).asDouble();
	    count++;
	}
   
   
   public String fieldName() {
	   return "avgof" + fldname;
   }
   
   public String inputField() {
	   return fldname;
   }
   
   
   /**
    * Returns the running total divided by the number of processed
    * records, converted to a float.
    *
    * @return the arithmetic mean as a float constant
    * @throws IllegalStateException if no records have been processed
    */
   public Constant value() {
	   if (count == 0) {
		   throw new IllegalStateException("Can't AVG over empty input");
	   }
	   
	   return new Constant((float) (total / count));
	   
   }
   
   /**
    * Validates that the input field is numeric and adds a REAL result
    * field to the output schema.
    *
    *
    * @param input the schema containing the field to average
    * @param output the schema to receive the average field
    * @throws IllegalArgumentException if the field does not exist or
    *         its type is neither INTEGER nor REAL
    */
   public void addResultField(Schema input, Schema output) {
       if (!input.hasField(fldname)) {
           throw new IllegalArgumentException(
               "Unknown aggregate field: " + fldname
           );
       }
       if (input.type(fldname) != INTEGER && input.type(fldname) != REAL) {
           throw new IllegalArgumentException(
               "Average requires a numeric field: " + fldname
           );
       }
       output.addFloatField(fieldName());
   }
   
   public boolean hasValueForEmptyInput() {
	   return false;
   }
}

