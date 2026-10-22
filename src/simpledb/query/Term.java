package simpledb.query;

import java.util.List;
import java.util.ArrayList;

import simpledb.plan.Plan;
import simpledb.record.Schema;

/**
 * A term is a comparison between two expressions.
 * @author Edward Sciore
 *
 */
public class Term {
   private Expression lhs, rhs;
   private String opr;
   
   /**
    * Created this for back compatibility
    */
   public Term(Expression lhs, Expression rhs) {
	   this(lhs, rhs, "=");
   }
   
   /**
    * Create a new term that compares two expressions
    * for equality.
    * @param lhs  the LHS expression
    * @param rhs  the RHS expression
    */
   public Term(Expression lhs, Expression rhs, String opr) {
      this.lhs = lhs;
      this.rhs = rhs;
      this.opr = opr;
   }
   
   /**
    * Return true if both of the term's expressions
    * evaluate to the same constant,
    * with respect to the specified scan.
    * @param s the scan
    * @return true if both expressions have the same value in the scan
    */
   public boolean isSatisfied(Scan s) {
      Constant lhsval = lhs.evaluate(s);
      Constant rhsval = rhs.evaluate(s);
      switch (opr) {
         case "=":
            return lhsval.equals(rhsval);
         case "!=":
         case "<>":
            return !lhsval.equals(rhsval);
         case "<":
            return lhsval.compareTo(rhsval) < 0;
         case "<=":
            return lhsval.compareTo(rhsval) <= 0;
         case ">":
            return lhsval.compareTo(rhsval) > 0;
         case ">=":
            return lhsval.compareTo(rhsval) >= 0;
         default:
            throw new RuntimeException("unrecognised /  unimplemented operator: " + opr);
      }
   }
   
   /**
    * Calculate the fraction of records that satisfy the term
    * selectivity, e.g. 0.5 if the term keeps half the records.
    * F = c: 1/V(F); F1 = F2: 1/max(V(F1), V(F2)); F != c: 1 - 1/V(F);
    * range terms: see rangeSelectivity.
    * c1 op c2: 1 or 0, since it is either true or false for every record.
    * @param p the query's plan
    * @return the selectivity, between 0 and 1
    */
   public double selectivity(Plan p) {
      if (!lhs.isFieldName() && !rhs.isFieldName())
         return constantSelectivity();
      if (this.opr.equals("!=") || this.opr.equals("<>")) {
         if (lhs.isFieldName() && !rhs.isFieldName())
            return 1.0 - 1.0 / p.distinctValues(lhs.asFieldName());
         if (rhs.isFieldName() && !lhs.isFieldName())
            return 1.0 - 1.0 / p.distinctValues(rhs.asFieldName());
         return 1.0;
      }
      if (!this.opr.equals("="))
         return rangeSelectivity(p);

      String lhsName, rhsName;
      if (lhs.isFieldName() && rhs.isFieldName()) {
         lhsName = lhs.asFieldName();
         rhsName = rhs.asFieldName();
         return 1.0 / Math.max(p.distinctValues(lhsName),
                               p.distinctValues(rhsName));
      }
      if (lhs.isFieldName()) {
         lhsName = lhs.asFieldName();
         return 1.0 / p.distinctValues(lhsName);
      }
      rhsName = rhs.asFieldName();
      return 1.0 / p.distinctValues(rhsName);
   }

   /**
    * Selectivity of a term comparing two constants, e.g. "5 > 3".
    * The term does not depend on the record, so it keeps
    * all records (1) or none (0).
    * Comparing an int with a string cannot be decided here
    * (and fails when the query runs), so the default 1/3 is used.
    */
   private double constantSelectivity() {
      Constant l = lhs.asConstant(), r = rhs.asConstant();
      switch (opr) {
         case "=":
            return l.equals(r) ? 1.0 : 0.0;
         case "!=":
         case "<>":
            return l.equals(r) ? 0.0 : 1.0;
      }
      boolean sametype = (l.asString() == null) == (r.asString() == null);
      if (!sametype)
         return 1.0 / 3;
      int cmp = l.compareTo(r);
      boolean result;
      switch (opr) {
         case "<":  result = cmp < 0;  break;
         case "<=": result = cmp <= 0; break;
         case ">":  result = cmp > 0;  break;
         case ">=": result = cmp >= 0; break;
         default:   return 1.0 / 3;
      }
      return result ? 1.0 : 0.0;
   }

   /**
    * Selectivity of a range term "F op c" (or "c op F").
    * the fraction of the range, e.g. for F >= c
    * (high - c + 1) / (high - low + 1), using the field's low / high
    * values from the stats. >, < and <= count the matching values the same way.
    * Falls back to 1/3 when the stats are not known (e.g. strings, F op F).
    */
   private double rangeSelectivity(Plan p) {
      String fldname;
      Constant val;
      String op = opr;
      if (lhs.isFieldName() && !rhs.isFieldName()) {
         fldname = lhs.asFieldName();
         val = rhs.asConstant();
      }
      else if (rhs.isFieldName() && !lhs.isFieldName()) {
         // "c op F" is "F flipped(op) c"
         fldname = rhs.asFieldName();
         val = lhs.asConstant();
         switch (opr) {
            case "<":  op = ">";  break;
            case "<=": op = ">="; break;
            case ">":  op = "<";  break;
            case ">=": op = "<="; break;
         }
      }
      else
         return 1.0 / 3;

      Constant low = p.minValue(fldname);
      Constant high = p.maxValue(fldname);
      if (low == null || high == null || val.asString() != null || low.asString() != null)
         return 1.0 / 3;
      long lo = low.asInt(), hi = high.asInt(), c = val.asInt();
      long domain = hi - lo + 1;
      long matches;
      switch (op) {
         case ">":  matches = hi - c;     break;
         case ">=": matches = hi - c + 1; break;
         case "<":  matches = c - lo;     break;
         case "<=": matches = c - lo + 1; break;
         default:   return 1.0 / 3;
      }
      matches = Math.max(0, Math.min(domain, matches));
      return (double) matches / domain;
   }

   /**
    * Determine if this term is of the form "F=c"
    * where F is the specified field and c is some constant.
    * If so, the method returns that constant.
    * If not, the method returns null.
    * @param fldname the name of the field
    * @return either the constant or null
    */
   public Constant equatesWithConstant(String fldname) {
	  if (!this.opr.equals("=")) {
		  return null;
	  }
	  
      if (lhs.isFieldName() &&
          lhs.asFieldName().equals(fldname) &&
          !rhs.isFieldName())
         return rhs.asConstant();
      else if (rhs.isFieldName() &&
               rhs.asFieldName().equals(fldname) &&
               !lhs.isFieldName())
         return lhs.asConstant();
      else
         return null;
   }
   
   /**
    * Determine if this term is of the form "F1=F2"
    * where F1 is the specified field and F2 is another field.
    * If so, the method returns the name of that field.
    * If not, the method returns null.
    * @param fldname the name of the field
    * @return either the name of the other field, or null
    */
   public String equatesWithField(String fldname) {
	  if (!this.opr.equals("=")) {
		  return null;
	  }
      if (lhs.isFieldName() &&
          lhs.asFieldName().equals(fldname) &&
          rhs.isFieldName())
         return rhs.asFieldName();
      else if (rhs.isFieldName() &&
               rhs.asFieldName().equals(fldname) &&
               lhs.isFieldName())
         return lhs.asFieldName();
      else
         return null;
   }
   
   /**
    * Return true if both of the term's expressions
    * apply to the specified schema.
    * @param sch the schema
    * @return true if both expressions apply to the schema
    */
   public boolean appliesTo(Schema sch) {
      return lhs.appliesTo(sch) && rhs.appliesTo(sch);
   }
   
   /**
    * Returns the names of all fields included in this term, skipping constants
    * (0, 1, or 2 names)
    */
   public List<String> fieldNames() {
	   List<String> result = new ArrayList<>();
	   if (this.lhs.isFieldName()) {
		   result.add(this.lhs.asFieldName());
	   }
	   
	   if (this.rhs.isFieldName()) {
		   result.add(this.rhs.asFieldName());
	   }
	   
	   return result;
   }
   
   public String toString() {
      return lhs.toString() + " " + this.opr + " " + rhs.toString();
   }
}
