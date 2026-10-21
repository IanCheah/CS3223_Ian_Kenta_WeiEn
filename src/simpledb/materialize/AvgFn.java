package simpledb.materialize;

import static java.sql.Types.INTEGER;

import simpledb.query.*;
import simpledb.record.Schema;

public class AvgFn implements AggregationFn {
	
	private String fldname;
	private long total;
	private long count;
	
	public AvgFn(String fldname) {
		this.fldname = fldname;
	}
	
	
	
	public void reset() {
		total = 0;
		count = 0;
	}
   
   
   public void processFirst(Scan s) {
	   reset();
	   processNext(s);
	   
   }
   
   
   public void processNext(Scan s) {
	   total = Math.addExact(total, (long) s.getInt(fldname));
	   count++;
   }
   
   
   public String fieldName() {
	   return "avgof" + fldname;
   }
   
   public String inputField() {
	   return fldname;
   }
   
   
   public Constant value() {
	   if (count == 0) {
		   throw new IllegalStateException("Can't AVG over empty input");
	   }
	   
	   return new Constant(Math.toIntExact(total / count));
	   
   }
   
   public void addResultField(Schema input, Schema output) {
       if (!input.hasField(fldname)) {
           throw new IllegalArgumentException(
               "Unknown aggregate field: " + fldname
           );
       }
       if (input.type(fldname) != INTEGER) {
           throw new IllegalArgumentException(
               "Average requires an integer field: " + fldname
           );
       }
       output.addIntField(fieldName());
   }
   
   public boolean hasValueForEmptyInput() {
	   return false;
   }
}

