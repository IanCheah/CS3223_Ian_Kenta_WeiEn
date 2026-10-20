package simpledb.materialize;

import static java.sql.Types.INTEGER;

import simpledb.query.*;
import simpledb.record.Schema;

public class SumFn implements AggregationFn {
	
	private String fldname;
	private long total;
	private boolean seen;
	
	public SumFn(String fldname) {
		this.fldname = fldname;
	}
	
	
	
	public void reset() {
		total = 0;
		seen = false;
	}
   
   
   public void processFirst(Scan s) {
	   reset();
	   processNext(s);
	   
   }
   
   
   public void processNext(Scan s) {
	   total = Math.addExact(total, (long) s.getInt(fldname));
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
	   
	   return new Constant(Math.toIntExact(total));
	   
   }
   
   public void addResultField(Schema input, Schema output) {
       if (!input.hasField(fldname)) {
           throw new IllegalArgumentException(
               "Unknown aggregate field: " + fldname
           );
       }
       if (input.type(fldname) != INTEGER) {
           throw new IllegalArgumentException(
               "SUM requires an integer field: " + fldname
           );
       }
       output.addIntField(fieldName());
   }
}
