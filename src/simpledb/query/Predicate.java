package simpledb.query;

import java.util.*;

import simpledb.plan.Plan;
import simpledb.record.*;

/**
 * A predicate is a Boolean combination of terms.
 * @author Edward Sciore
 *
 */
public class Predicate {
   private List<Term> terms = new ArrayList<Term>();

   /**
    * Create an empty predicate, corresponding to "true".
    */
   public Predicate() {}

   /**
    * Create a predicate containing a single term.
    * @param t the term
    */
   public Predicate(Term t) {
      terms.add(t);
   }

   /**
    * Modifies the predicate to be the conjunction of
    * itself and the specified predicate.
    * @param pred the other predicate
    */
   public void conjoinWith(Predicate pred) {
      terms.addAll(pred.terms);
   }

   /**
    * Returns true if the predicate evaluates to true
    * with respect to the specified scan.
    * @param s the scan
    * @return true if the predicate is true in the scan
    */
   public boolean isSatisfied(Scan s) {
      for (Term t : terms)
         if (!t.isSatisfied(s))
            return false;
      return true;
   }

   /**
    * Calculate the fraction of records that satisfy the predicate.
    * Terms are assumed independent, so their selectivities are multiplied.
    * A fraction, not an integer reduction factor, so that e.g. a term
    * keeping 2/3 of the records is not rounded to 1/2.
    * @param p the query's plan
    * @return the selectivity, between 0 and 1
    */
   public double selectivity(Plan p) {
      double sel = 1.0;
      for (Term t : terms)
         sel *= t.selectivity(p);
      return sel;
   }

   /**
    * Return this predicate without the term "F=c",
    * e.g. the term that an index select already enforces.
    * @param fldname the field
    * @param val the constant
    * @return the remaining terms, or null if there are none
    */
   public Predicate withoutConstant(String fldname, Constant val) {
      Predicate result = new Predicate();
      for (Term t : terms) {
         Constant c = t.equatesWithConstant(fldname);
         if (c == null || !c.equals(val))
            result.terms.add(t);
      }
      if (result.terms.size() == 0)
         return null;
      else
         return result;
   }

   /**
    * Return this predicate without the term "F1=F2",
    * e.g. the term that a join algorithm already enforces.
    * @param fldname1 the first field
    * @param fldname2 the second field
    * @return the remaining terms, or null if there are none
    */
   public Predicate withoutEquality(String fldname1, String fldname2) {
      Predicate result = new Predicate();
      for (Term t : terms)
         if (!fldname2.equals(t.equatesWithField(fldname1)))
            result.terms.add(t);
      if (result.terms.size() == 0)
         return null;
      else
         return result;
   }

   /**
    * Return the subpredicate that applies to the specified schema.
    * @param sch the schema
    * @return the subpredicate applying to the schema
    */
   public Predicate selectSubPred(Schema sch) {
      Predicate result = new Predicate();
      for (Term t : terms)
         if (t.appliesTo(sch))
            result.terms.add(t);
      if (result.terms.size() == 0)
         return null;
      else
         return result;
   }

   /**
    * Return the subpredicate consisting of terms that apply
    * to the union of the two specified schemas, 
    * but not to either schema separately.
    * @param sch1 the first schema
    * @param sch2 the second schema
    * @return the subpredicate whose terms apply to the union of the two schemas but not either schema separately.
    */
   public Predicate joinSubPred(Schema sch1, Schema sch2) {
      Predicate result = new Predicate();
      Schema newsch = new Schema();
      newsch.addAll(sch1);
      newsch.addAll(sch2);
      for (Term t : terms)
         if (!t.appliesTo(sch1)  &&
               !t.appliesTo(sch2) &&
               t.appliesTo(newsch))
            result.terms.add(t);
      if (result.terms.size() == 0)
         return null;
      else
         return result;
   }

   /**
    * Determine if there is a term of the form "F=c"
    * where F is the specified field and c is some constant.
    * If so, the method returns that constant.
    * If not, the method returns null.
    * @param fldname the name of the field
    * @return either the constant or null
    */
   public Constant equatesWithConstant(String fldname) {
      for (Term t : terms) {
         Constant c = t.equatesWithConstant(fldname);
         if (c != null)
            return c;
      }
      return null;
   }

   /**
    * Determine if there is a term of the form "F1=F2"
    * where F1 is the specified field and F2 is another field.
    * If so, the method returns the name of that field.
    * If not, the method returns null.
    * @param fldname the name of the field
    * @return the name of the other field, or null
    */
   public String equatesWithField(String fldname) {
      for (Term t : terms) {
         String s = t.equatesWithField(fldname);
         if (s != null)
            return s;
      }
      return null;
   }
   
   public List<String> fieldNames() {
	   List<String> result = new ArrayList<>();
	   for (Term t : this.terms) {
		   result.addAll(t.fieldNames());
	   }
	   return result;
   }

   public String toString() {
      Iterator<Term> iter = terms.iterator();
      if (!iter.hasNext()) 
         return "";
      String result = iter.next().toString();
      while (iter.hasNext())
         result += " and " + iter.next().toString();
      return result;
   }
}
