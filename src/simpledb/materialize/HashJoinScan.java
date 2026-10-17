package simpledb.materialize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import simpledb.query.Constant;
import simpledb.query.Scan;
import simpledb.record.Schema;

// HashJoinScan(rPartsFinal, sPartsFinal, shouldBuildLeft, this.field1, this.field2);
public class HashJoinScan implements Scan {
	private List<TempTable> rParts;
	private String rField;
	private Schema rSchema;
	
	private List<TempTable> sParts;
	private String sField;
	private Schema sSchema;
	private List<Boolean> shouldBuildLeft;
	
	// State variables
	private int i; // Corresponds to ith partition
	private String probeField;
	private Map<Constant, List<Map<String, Constant>>> hashTable;
	private Scan probeCursor = null;
	
	private List<Map<String, Constant>> currMatches; // an entry of hash table
	private int matchIdx;
	
	public HashJoinScan(List<TempTable> rParts, List<TempTable> sParts, List<Boolean> shouldBuildLeft, String rField, String sField) {
		this.rParts = rParts;
		this.rField = rField;
		this.rSchema = rParts.get(0).getLayout().schema();
		
		this.sParts = sParts;
		this.sField = sField;
		this.sSchema = sParts.get(0).getLayout().schema();
		
		this.shouldBuildLeft = shouldBuildLeft;
		
		beforeFirst();
	}
	
	public void beforeFirst() {
		// close the probeCursor
		if (probeCursor != null) {
			probeCursor.close();
			probeCursor = null;
		}
		
		this.i = -1;
		this.hashTable = null;
		this.currMatches = null;
		this.matchIdx = -1;
	}
	
	public boolean next() {
		while (true) {
			if (this.currMatches != null && this.matchIdx + 1 < currMatches.size()) {
				this.matchIdx++;
				return true;
			}
			
			// Reach here if currMatches is null OR currMatches is exhausted
			// Hence, we should consider the next probe row
			if (this.probeCursor != null && probeCursor.next()) {
				Constant value = this.probeCursor.getVal(this.probeField);
				this.currMatches = hashTable.get(value);
				this.matchIdx = -1;
				continue;
			}
			
			if (!loadNextPair()) {
				return false;
			}
		}
	}
	
	private boolean loadNextPair() {
		// close the probeCursor
		if (probeCursor != null) {
			probeCursor.close();
			probeCursor = null;
		}
		// Consider the next 
		this.i++;
		if (i >= rParts.size() || i >= sParts.size()) {
			return false;
		}
		
		// Decide which side to build on (left correspond to R)
		boolean buildLeft = this.shouldBuildLeft.get(i);
		TempTable buildTable = buildLeft ? this.rParts.get(i) : this.sParts.get(i);
		TempTable probeTable = buildLeft ? this.sParts.get(i) : this.rParts.get(i);
		String buildField = buildLeft ? this.rField : this.sField;
		this.probeField = buildLeft ? this.sField : this.rField;
		
		// Build the hashTable
		this.hashTable = new HashMap<>();
		Scan buildCursor = buildTable.open();
		while (buildCursor.next()) {
			Constant val = buildCursor.getVal(buildField);
			Map<String, Constant> eachRow = new HashMap<>();
			
			for (String fieldName : buildTable.getLayout().schema().fields()) {
				eachRow.put(fieldName, buildCursor.getVal(fieldName));
			}
			
			if (this.hashTable.containsKey(val)) {
				List<Map<String, Constant>> currEntry = this.hashTable.get(val);
				currEntry.add(eachRow);
			} else {
				List<Map<String, Constant>> newEntry = new ArrayList<>();
				newEntry.add(eachRow);
				this.hashTable.put(val, newEntry);
			}
		}
		buildCursor.close();
		
		// Get ready the probe phase
		this.probeCursor = probeTable.open();
		this.currMatches = null;
		this.matchIdx = -1;
		
		return true;
	}
	
	// ------------------- end of next() ---------------------------------
	
	public Constant getVal(String fldname) {
		Map<String, Constant> buildRow = this.currMatches.get(matchIdx);
		if (buildRow.containsKey(fldname)) {
			return buildRow.get(fldname);
		} else {
			return this.probeCursor.getVal(fldname);
		}
	}
	
	public int getInt(String fldname) {
		return this.getVal(fldname).asInt();
	}
   
   public String getString(String fldname) {
	   return this.getVal(fldname).asString();
   }
   
   public boolean hasField(String fldname) {
	   return this.rSchema.hasField(fldname) || this.sSchema.hasField(fldname);
   }
   
   public void close() {
	if (probeCursor != null) {
		probeCursor.close();
		probeCursor = null;
	}
   }
}
