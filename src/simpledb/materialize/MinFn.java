package simpledb.materialize;

import simpledb.query.*;
import simpledb.record.Schema;

/**
 * The <i>min</i> aggregation function.
 * @author Edward Sciore
 */
public class MinFn implements AggregationFn {
	private String fldname;
	private Constant val;
	
	/**
	 * Create a min aggregation function for the specified field.
	 * @param fldname the name of the aggregated field
	 */
	public MinFn(String fldname) {
	      this.fldname = fldname;
	}
	  
	public void reset() {
		val = null;
	}
	   
	/**
	 * Start a new minimum to be the 
	 * field value in the current record.
	 * @see simpledb.materialize.AggregationFn#processFirst(simpledb.query.Scan)
	 */
	public void processFirst(Scan s) {
		reset();
		processNext(s);
	}
	   
	   
	/**
	 * Replace the current minimum by the field value
	 * in the current record, if it is lower.
	 * @see simpledb.materialize.AggregationFn#processNext(simpledb.query.Scan)
	 */
	public void processNext(Scan s) {
		Constant newval = s.getVal(fldname);
		if (val == null || newval.compareTo(val) < 0) {
			val = newval;
		}
	         
	}
	   
	/**
	 * Return the field's name, prepended by "minof".
	 * @see simpledb.materialize.AggregationFn#fieldName()
	 */
	public String fieldName() {
	   return "minof" + fldname;
	}
	   
	public String inputField() {
		return fldname;
	}
	   
	/**
	 * Return the current minimum.
	 * @see simpledb.materialize.AggregationFn#value()
	 */
	public Constant value() {
	   if (val == null) {
	    throw new IllegalStateException();
	   }
	      
	   return val;
	}
	   
	/**
	 * Adds a minimum-result field with the input field's SQL type and
	 * declared length, preserving string capacity where applicable.
	 *
	 * @param input the schema containing the aggregated field
	 * @param output the schema to receive the minimum field
	 * @throws IllegalArgumentException if the input field does not exist
	 */
	public void addResultField(Schema input, Schema output) {
	    if (!input.hasField(fldname)) {
	        throw new IllegalArgumentException(
	            "Unknown aggregate field: " + fldname
	        );
	    }
	    output.addField(fieldName(), input.type(fldname), input.length(fldname));
	}

   public boolean hasValueForEmptyInput() {
	   return false;
   }
}
