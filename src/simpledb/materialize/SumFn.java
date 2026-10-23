package simpledb.materialize;

import static java.sql.Types.*;

import simpledb.query.*;
import simpledb.record.Schema;

public class SumFn implements AggregationFn {
	
	private String fldname;
	private long intTotal;
	private double floatTotal;
	private boolean isFloat;
	private boolean seen;
	
	/**
	 * Create a sum aggregation function for the specified field.
	 * @param fldname the name of the aggregated field
	 */
	public SumFn(String fldname) {
		this.fldname = fldname;
	}
	
	
	
	public void reset() {
		intTotal = 0;
		floatTotal = 0.0;
		seen = false;
	}
   
   
   public void processFirst(Scan s) {
	   reset();
	   processNext(s);
	   
   }
   
   
   /**
    * Adds the current record's field value to the running total.
    *
    * The configured input type determines whether integer or
    * floating-point accumulation is used.
    *
    * @param s the scan positioned on the record to process
    */
   public void processNext(Scan s) {
	    Constant value = s.getVal(fldname);

	    if (isFloat) {
	        floatTotal += value.asDouble();
	    } else {
	        intTotal = Math.addExact(
	            intTotal, (long) value.asInt()
	        );
	    }

	    seen = true;
	}
   
   
   public String fieldName() {
	   return "sumof" + fldname;
   }
   
   public String inputField() {
	   return fldname;
   }
   
   /**
    * Returns the current sum using the configured input type.
    *
    * @return an integer constant for INTEGER input, or a float constant
    *         for FLOAT input
    * @throws IllegalStateException if no records have been processed
    */
   public Constant value() {
	   if (!seen) {
		   throw new IllegalStateException("SUM over empty input");
	   }
	   
	   if (isFloat) {
		   return new Constant((float) floatTotal);
	   }
	   
	   return new Constant(Math.toIntExact(intTotal));
	   
   }
   
   /**
    * Validates the numeric input field, configures the accumulation type,
    * and adds a result field with the same SQL type as the input.
    *
    * <p>This method must be called before processing records.
    *
    * @param input the schema containing the field to sum
    * @param output the schema to receive the sum field
    * @throws IllegalArgumentException if the field does not exist or
    *         its type is neither INTEGER nor REAL
    */
   public void addResultField(Schema input, Schema output) {
	    if (!input.hasField(fldname)) {
	        throw new IllegalArgumentException("Unknown aggregate field: " + fldname);
	    }

	    int type = input.type(fldname);

	    if (type != INTEGER && type != REAL) {
	        throw new IllegalArgumentException("SUM requires a numeric field: " + fldname);
	    }

	    isFloat = type == REAL;

	    if (isFloat) {
	        output.addFloatField(fieldName());
	    } else {
	        output.addIntField(fieldName());
	    }
	}
   
   public boolean hasValueForEmptyInput() {
	   return false;
   }
}
