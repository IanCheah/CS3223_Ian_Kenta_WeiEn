package simpledb.parse;


import java.util.*;
import simpledb.query.Constant;
import java.io.*;
/**
 * The lexical analyzer.
 * @author Edward Sciore
 */
public class Lexer {
   private Collection<String> keywords;
   private StreamTokenizer tok;
   
   /**
    * Creates a new lexical analyzer for SQL statement s.
    * @param s the SQL statement
    */
   public Lexer(String s) {
      initKeywords();
      tok = new StreamTokenizer(new StringReader(s));
      tok.wordChars('_', '_'); //allow "_" in identifiers
      tok.lowerCaseMode(true); //ids and keywords are converted
      nextToken();
   }
   
//Methods to check the status of the current token
   
   /**
    * Returns true if the current token is
    * the specified delimiter character.
    * @param d a character denoting the delimiter
    * @return true if the delimiter is the current token
    */
   public boolean matchDelim(char d) {
      return d == (char)tok.ttype;
   }
   
   /**
    * Returns true if the current token is an integer.
    * @return true if the current token is an integer
    */
   public boolean matchIntConstant() {
      return tok.ttype == StreamTokenizer.TT_NUMBER;
   }
   
   /**
    * Returns true if the current token is a string.
    * @return true if the current token is a string
    */
   public boolean matchStringConstant() {
      return '\'' == (char)tok.ttype;
   }
   
   /**
    * Returns true if the current token is the specified keyword.
    * @param w the keyword string
    * @return true if that keyword is the current token
    */
   public boolean matchKeyword(String w) {
      return tok.ttype == StreamTokenizer.TT_WORD && tok.sval.equals(w);
   }
   
   /**
    * Returns true if the current token is a legal identifier.
    * @return true if the current token is an identifier
    */
   public boolean matchId() {
      return  tok.ttype==StreamTokenizer.TT_WORD && !keywords.contains(tok.sval);
   }
   
   /**
    * Returns true if the current token is the first character
    * of a comparison operator
    * @return true if the current token is a comparison operator
    */
   public boolean matchOpr() {
	   return this.matchDelim('=') || this.matchDelim('<') || this.matchDelim('>') || this.matchDelim('!');
   }
   
//Methods to "eat" the current token
   /**
    * Process the current operator and move on to the next one
    * @return the string value of the operator
    */
   public String eatOpr() {
	   if (!this.matchOpr()) {
		   throw new BadSyntaxException();
	   }
	   
	   char first = (char) tok.ttype;
	   this.nextToken();
	   
      switch (first) {
         case '=':
            if (this.matchOpr()) {
               throw new BadSyntaxException();
            }
            return "=";
         case '<':
            if (this.matchDelim('=')) {
               this.nextToken();
               return "<=";
            } else if (this.matchDelim('>')) {
               this.nextToken();
               return "<>";
            } else if (this.matchOpr()) {
               throw new BadSyntaxException();
            }
            return "<";
         case '>':
            if (this.matchDelim('=')) {
               this.nextToken();
               return ">=";
            } else if (this.matchOpr()) {
               throw new BadSyntaxException();
            }
            return ">";
         default:
            if (this.matchDelim('=')) {
               this.nextToken();
               return "!=";
            }
            throw new BadSyntaxException();
      }
   }
   
   /**
    * Throws an exception if the current token is not the
    * specified delimiter. 
    * Otherwise, moves to the next token.
    * @param d a character denoting the delimiter
    */
   public void eatDelim(char d) {
      if (!matchDelim(d))
         throw new BadSyntaxException();
      nextToken();
   }
   
   /**
    * Throws an exception if the current token is not 
    * an integer. 
    * Otherwise, returns that integer and moves to the next token.
    * @return the integer value of the current token
    */
   public int eatIntConstant() {
	    if (tok.ttype != StreamTokenizer.TT_NUMBER) {
	        throw new BadSyntaxException();
	    }

	    double number = tok.nval;

	    if (!Double.isFinite(number)
	            || number < Integer.MIN_VALUE
	            || number > Integer.MAX_VALUE
	            || number != Math.rint(number)) {
	        throw new BadSyntaxException();
	    }

	    nextToken();
	    return (int) number;
	}
   
   /**
    * Throws an exception if the current token is not 
    * a numeric value. 
    * Otherwise, returns that numeric value and moves to the next token.
    * @return the numeric value of the current token
    */
   public Constant eatNumericConstant() {
	    if (tok.ttype != StreamTokenizer.TT_NUMBER) {
	        throw new BadSyntaxException();
	    }

	    double number = tok.nval;
	    nextToken();

	    if (!Double.isFinite(number)) {
	        throw new BadSyntaxException();
	    }

	    if (number >= Integer.MIN_VALUE
	            && number <= Integer.MAX_VALUE
	            && number == Math.rint(number)) {
	        return new Constant((int) number);
	    }

	    float value = (float) number;

	    if (!Float.isFinite(value)) {
	        throw new BadSyntaxException();
	    }

	    return new Constant(value);
	}
   
   /**
    * Throws an exception if the current token is not 
    * a string. 
    * Otherwise, returns that string and moves to the next token.
    * @return the string value of the current token
    */
   public String eatStringConstant() {
      if (!matchStringConstant())
         throw new BadSyntaxException();
      String s = tok.sval; //constants are not converted to lower case
      nextToken();
      return s;
   }
   
   /**
    * Throws an exception if the current token is not the
    * specified keyword. 
    * Otherwise, moves to the next token.
    * @param w the keyword string
    */
   public void eatKeyword(String w) {
      if (!matchKeyword(w))
         throw new BadSyntaxException();
      nextToken();
   }
   
   /**
    * Throws an exception if the current token is not 
    * an identifier. 
    * Otherwise, returns the identifier string 
    * and moves to the next token.
    * @return the string value of the current token
    */
   public String eatId() {
      if (!matchId())
         throw new BadSyntaxException();
      String s = tok.sval;
      nextToken();
      return s;
   }
   
   private void nextToken() {
      try {
         tok.nextToken();
      }
      catch(IOException e) {
         throw new BadSyntaxException();
      }
   }
   
   private void initKeywords() {
      keywords = Arrays.asList("select", "from", "where", "and",
                               "insert", "into", "values", "delete", "update", "set", 
                               "create", "table", "int", "float", "varchar", "view", "as", "index", "on",
                               "using", "order", "by", "asc", "desc",
                               "distinct", "group", "sum", "count", "avg", "min", "max",
                               "union", "all", "intersect", "except");
   }
   
   /**
    * Tests whether all input tokens have been consumed.
    *
    * @return true if the current token is end-of-input
    */
   public boolean atEnd() {
	   return this.tok.ttype == StreamTokenizer.TT_EOF;
   }
}