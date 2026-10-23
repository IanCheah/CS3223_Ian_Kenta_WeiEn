package simpledb.materialize;

import java.util.ArrayList;
import java.util.List;

import simpledb.parse.SetOpr;
import simpledb.query.Constant;
import simpledb.query.Scan;

/**
 * Merges 2 fully sorted scans to perform INTERSECT or EXCEPT operations.
 */
public class SortedSetOprScan implements Scan {
	private Scan leftScan;
	private List<String> leftFields;
	
	private SetOpr operation;
	
	private Scan rightScan;
	private List<String> rightFields;
	
	private boolean hasLeft;
	private boolean hasRight;
	private List<Constant> currentRecord;
	
	/**
	 * Creates a scan for a sorted set operation.
	 * @param operation   the set operation to perform (INTERSECT or EXCEPT)
	 * @param leftScan    the sorted left input scan
	 * @param rightScan   the sorted right input scan
	 * @param leftFields  the field names of the left scan
	 * @param rightFields the field names of the right scan
	 */
	public SortedSetOprScan(
			SetOpr operation, 
			Scan leftScan,
			Scan rightScan,
			List<String> leftFields,
			List<String> rightFields) {
        this.operation = operation;
        this.leftScan = leftScan;
        this.rightScan = rightScan;
        this.leftFields = leftFields;
        this.rightFields = rightFields;
        beforeFirst();
    }

	@Override
	public void beforeFirst() {
		leftScan.beforeFirst();
		rightScan.beforeFirst();
		hasLeft = leftScan.next();
		hasRight = rightScan.next();
		currentRecord = null;
	}

	@Override
	public boolean next() {
		while (hasLeft) {
			int comparison = hasRight ? compare() : -1;
			List<Constant> leftSnapShot = snapshot(leftScan, leftFields);
			List<Constant> rightSnapShot = hasRight ? snapshot(rightScan, rightFields) : null;
			
			// Right row is smaller
			if (comparison > 0) {
				advanceUntilNotEqual(false, rightSnapShot);
				continue;
			}
			
			advanceUntilNotEqual(true, leftSnapShot);
			
			// Both rows are equal
			if (comparison == 0) {
				advanceUntilNotEqual(false, leftSnapShot);
				
				if (operation == SetOpr.INTERSECT) {
					currentRecord = leftSnapShot;
					return true;
				}
			// Right row is greater
			} else {
				if (operation == SetOpr.EXCEPT) {
					currentRecord = leftSnapShot;
					return true;
				}
			}
		}
		
		return false;
	}
	
	private int compare() {
		for (int i = 0; i < leftFields.size(); i++) {
			int res = leftScan.getVal(leftFields.get(i)).compareTo(rightScan.getVal(rightFields.get(i)));
			if (res != 0) {
				return res;
			}
		}
		return 0;
	}
	
	private List<Constant> snapshot(Scan scan, List<String> fields) {
		List<Constant> vals = new ArrayList<>();
		for (String field : fields) {
			vals.add(scan.getVal(field));
		}
		return vals;
	}
	
	private boolean equalsRow(Scan scan, List<String> fields, List<Constant> row) {
		for (int i = 0; i < fields.size(); i++) {
			Constant currLeftVal = scan.getVal(fields.get(i));
			Constant currRightVal = row.get(i);
			if (!currLeftVal.equals(currRightVal)) {
				return false;
			}
		}
		return true;
	}
	
	private void advanceUntilNotEqual(boolean isLeft, List<Constant> row) {
		if (isLeft) {
			hasLeft = leftScan.next();
			while (hasLeft && equalsRow(leftScan, leftFields, row)) {
				hasLeft = leftScan.next();
			}
		} else {
			hasRight = rightScan.next();
			while (hasRight && equalsRow(rightScan, rightFields, row)) {
				hasRight = rightScan.next();
			}
		}
	}

	@Override
	public int getInt(String fldname) {
		return getVal(fldname).asInt();
	}

	@Override
	public String getString(String fldname) {
		return getVal(fldname).asString();
	}

	@Override
	public Constant getVal(String fldname) {
		int idx = leftFields.indexOf(fldname);
		return currentRecord.get(idx);
	}

	@Override
	public boolean hasField(String fldname) {
		return leftFields.contains(fldname);
	}

	@Override
	public void close() {
		leftScan.close();
		rightScan.close();
	}

}
