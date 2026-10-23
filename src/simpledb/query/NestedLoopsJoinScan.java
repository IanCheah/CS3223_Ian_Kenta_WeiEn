package simpledb.query;

/**
 * Returns the pairs of an outer and inner scan that satisfy a join predicate.
 * The inner scan restarts when exhausted for the current outer row.
 */
public class NestedLoopsJoinScan implements Scan {
   private Scan s1, s2;
   private Predicate joinpred;
   private boolean hasOuter;
   
   /**
    * Creates a nested-loops scan and initializes the input positions.
    *
    * @param s1 the outer scan
    * @param s2 the inner scan
    * @param joinpred the predicate that a returned pair must satisfy
    */
   public NestedLoopsJoinScan(Scan s1, Scan s2, Predicate joinpred) {
      this.s1 = s1;
      this.s2 = s2;
      this.joinpred = joinpred;
      beforeFirst();
   }

   public void beforeFirst() {
      s1.beforeFirst();
      hasOuter = s1.next();
      s2.beforeFirst();
   }
   
   public boolean next() {
	  while (advance()) {
		  if (joinpred.isSatisfied(this)) {
			  return true;
		  }
	  }
	  return false;
   }
   
   /**
    * Advances to the next candidate pair and restarts the inner scan or advances the outer scan as needed.
    *
    * @return true if a candidate pair was reached
    */
   private boolean advance() {
      if (!hasOuter)
         return false;
      if (s2.next())
         return true;
      s2.beforeFirst();
      hasOuter = s1.next();
      return hasOuter && s2.next();
   }
   
   public int getInt(String fldname) {
      if (s1.hasField(fldname))
         return s1.getInt(fldname);
      else
         return s2.getInt(fldname);
   }

   public String getString(String fldname) {
      if (s1.hasField(fldname))
         return s1.getString(fldname);
      else
         return s2.getString(fldname);
   }

   public Constant getVal(String fldname) {
      if (s1.hasField(fldname))
         return s1.getVal(fldname);
      else
         return s2.getVal(fldname);
   }

   public boolean hasField(String fldname) {
      return s1.hasField(fldname) || s2.hasField(fldname);
   }

   public void close() {
      s1.close();
      s2.close();
   }

}
