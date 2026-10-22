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
   
   
   public Constant value() {
	   if (!seen) {
		   throw new IllegalStateException("SUM over empty input");
	   }
	   
	   if (isFloat) {
		   return new Constant((float) floatTotal);
	   }
	   
	   return new Constant(Math.toIntExact(intTotal));
	   
   }
   
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
