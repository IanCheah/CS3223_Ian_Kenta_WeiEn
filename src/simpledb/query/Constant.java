package simpledb.query;

/**
 * The class that denotes values stored in the database.
 * @author Edward Sciore
 */
public class Constant implements Comparable<Constant> {
   private Integer ival = null;
   private String  sval = null;
   private Float fval = null;
   
   public Constant(Integer ival) {
      this.ival = ival;
   }
   
   public Constant(String sval) {
      this.sval = sval;
   }
   
   public Constant(Float fval) {
	   if (!Float.isFinite(fval)) {
		   throw new IllegalArgumentException("non-finte float values are not supported");
	   }
	   
	   this.fval = fval == 0.0f ? 0.0f : fval;
   }
   
   public int asInt() {
	   if (ival == null) {
		   throw new IllegalArgumentException("expected integer");
	   }
      return ival;
   }
   
   // Used for intermediate value calculations, database does not support double data type
   public double asDouble() {
	   if (ival != null) {
		   return ival.doubleValue();
	   }
	   
	   if (fval != null) {
		   return fval.doubleValue();
	   }
	   
	   throw new IllegalArgumentException("expected numeric value");
	   
   }
   
   public Float asFloat() {
	   return (float) asDouble();
   }
   
   public boolean isNumeric() {
	   return ival != null || fval != null;
   }
   
   public boolean isInt() {
	   return ival != null;
   }
   
   public String asString() {
      return sval;
   }
   
   public boolean equals(Object obj) {
	   if (this == obj) {
	        return true;
	    }
	    if (!(obj instanceof Constant)) {
	        return false;
	    }

	    Constant other = (Constant) obj;

	    if (isNumeric() && other.isNumeric()) {
	        return Double.compare(asDouble(), other.asDouble()) == 0;
	    }

	    return sval != null
	            && other.sval != null
	            && sval.equals(other.sval);
   }
   
   public int compareTo(Constant c) {
	   if (isNumeric() && c.isNumeric()) {
	    	  return Double.compare(asDouble(), c.asDouble());
	      }
	   
	   if (sval != null && c.sval != null) {
		   return sval.compareTo(c.sval);
	   }
	   
	   throw new IllegalArgumentException("cannot compare string to a number");
   }
   
   public int hashCode() {
	   if (isNumeric()) {
	        double number = asDouble();

	        if (number >= Integer.MIN_VALUE
	                && number <= Integer.MAX_VALUE
	                && number == Math.rint(number)) {
	            return Integer.hashCode((int) number);
	        }

	        return Double.hashCode(number);
	    }
	   
	   
	   return sval.hashCode();
   }
   
   public String toString() {
      if (ival != null) {
    	  return ival.toString();
      }
      
      if (fval != null) {
    	  return fval.toString();
      }
      
      return sval;
   }   
}
