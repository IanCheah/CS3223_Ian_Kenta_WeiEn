package simpledb.plan;

import simpledb.query.ProductScan;
import simpledb.query.Scan;
import simpledb.record.Schema;
import simpledb.query.NestedLoopsJoinScan;
import simpledb.query.Predicate;

public class NestedLoopsJoinPlan implements Plan {
   private Plan p1, p2;
   private Schema schema = new Schema();
   private Predicate joinpred;
   
   public NestedLoopsJoinPlan(Plan p1, Plan p2, Predicate joinpred) {
      this.p1 = p1;
      this.p2 = p2;
      this.joinpred = joinpred;
      schema.addAll(p1.schema());
      schema.addAll(p2.schema());
   }
	   
   public Scan open() {
      Scan s1 = this.p1.open();
      Scan s2 = p2.open();
      return new NestedLoopsJoinScan(s1, s2, this.joinpred);
   }
   
   public int blocksAccessed() {
	  return p1.blocksAccessed() + (p1.recordsOutput() * p2.blocksAccessed());
   }
   
   // This is just an estimate 
   public int recordsOutput() {
      return (p1.recordsOutput() * p2.recordsOutput()) / joinpred.reductionFactor(this);
   }
	   
   public int distinctValues(String fldname) {
      if (p1.schema().hasField(fldname))
         return p1.distinctValues(fldname);
      else
         return p2.distinctValues(fldname);
   }
   
   public Schema schema() {
      return schema;
   }
	      
}