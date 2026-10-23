package simpledb.materialize;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import simpledb.query.Constant;
import simpledb.query.Scan;

/**
 * Concatenates the records of 2 underlying scans (union-all)
 */
public class UnionScan implements Scan {
	private Scan leftScan;
	private Scan rightScan;
	private Map<String, String> fieldMap;
	private boolean scanningRight;
	
	/**
     * Creates a union scan over 2 underlying scans.
     *
     * @param leftScan    the first scan to read from
     * @param rightScan   the second scan to read from after the first is exhausted
     * @param leftFields  the schema field names for the left scan
     * @param rightFields the schema field names for the right scan (must match leftFields in order)
     */
    public UnionScan(Scan leftScan, Scan rightScan, List<String> leftFields, List<String> rightFields) {
        this.leftScan = leftScan;
        this.rightScan = rightScan;
        this.fieldMap = new HashMap<>();
        
        for (int i = 0; i < leftFields.size(); i++) {
            this.fieldMap.put(leftFields.get(i), rightFields.get(i));
        }
        
        beforeFirst();
    }
    
    /**
     * Positions the scan before the first record.
     * Resets pointer to left scan.
     */
    public void beforeFirst() {
    	this.leftScan.beforeFirst();
    	this.rightScan.beforeFirst();
    	scanningRight = false;
    }
    
    /**
     * Moves the scan to the next record.
     * 
     * @return true if there is a next record, false if both scans are exhausted
     */
    public boolean next() {
    	if (!this.scanningRight) {
    		if (this.leftScan.next()) {
    			return true;
    		}
    		this.scanningRight = true;
    	}
    	return this.rightScan.next();
    }
    
    /**
     * Returns the value of the specified field in the current record.
     * 
     * @param fldname the name of the field (match the left scan's schema)
     * @return the Constant value associated with the field
     */
    public Constant getVal(String fldname) {
    	if (this.scanningRight) {
    		return this.rightScan.getVal(this.fieldMap.get(fldname));
    	}
    	return this.leftScan.getVal(fldname);
    }
    
    /**
     * Returns the integer value of the specified field name.
     * 
     * @param fldname the name of the field
     * @return the integer value of the field
     */
    public int getInt(String fldname) {
    	return this.getVal(fldname).asInt();
    }

    /**
     * Returns the String value of the specified field name.
     * 
     * @param fldname the name of the field
     * @return the String value of the field
     */
    public String getString(String fldname) {
    	return this.getVal(fldname).asString();
    }
    
    /**
     * Checks if the scan contains the specified field.
     *
     * @param fldname the name of the field to check
     * @return true if the field exists in the unified schema, false otherwise
     */
    public boolean hasField(String fldname) {
        return fieldMap.containsKey(fldname);
    }
    
    /**
     * Closes both underlying scans.
     */
    public void close() {
        leftScan.close();
        rightScan.close();
    }
}
