package simpledb.materialize;

import java.util.ArrayList;
import java.util.List;

import simpledb.plan.Plan;
import simpledb.query.Constant;
import simpledb.query.Scan;
import simpledb.query.UpdateScan;
import simpledb.tx.Transaction;
import simpledb.record.Schema;

public class HashJoinPlan implements Plan {
	private static final int MAX_LEVEL = 3;
	private Transaction tx;
	private Plan r, s;
	private String field1, field2;
	private Schema schema = new Schema();
	
	public HashJoinPlan(Transaction tx, Plan r, Plan s, String field1, String field2) {
		this.tx = tx;
		this.r = r;
		this.s = s;
		this.field1 = field1;
		this.field2 = field2;
		
		this.schema.addAll(r.schema());
		this.schema.addAll(s.schema());
	}
	
	public Scan open() {
		// Correspond to k in lecture notes
		int partitionNum = Math.max(1, tx.availableBuffs() - 1);
		List<TempTable> rPartitions = partition(this.r.open(), this.r.schema(), this.field1, partitionNum, 0);
		List<TempTable> sPartitions = partition(this.s.open(), this.s.schema(), this.field2, partitionNum, 0);
		
		// Recursive logic (see recursivePartition)
		List<TempTable> rPartsFinal = new ArrayList<>();
		List<TempTable> sPartsFinal = new ArrayList<>();
		List<Boolean> shouldBuildLeft = new ArrayList<>();
		
		for (int i = 0; i < partitionNum; i++) {
			recursivePartition(
					rPartsFinal,
					sPartsFinal,
					shouldBuildLeft,
					rPartitions.get(i),
					sPartitions.get(i),
					1,
					partitionNum);
		}
		
		return new HashJoinScan(rPartsFinal, sPartsFinal, shouldBuildLeft, this.field1, this.field2);
	}
	
	private void recursivePartition(
			List<TempTable> rPartsFinal, 
			List<TempTable> sPartsFinal,
			List<Boolean> shouldBuildLeft,
			TempTable r,
			TempTable s,
			int level,
			int partitionNum) {
		// Each partition size should be <= B-2 according to lecture
		int maxSize = tx.availableBuffs() - 2;
		int rSize = numBlocks(r);
		int sSize = numBlocks(s);
		
		if (level > MAX_LEVEL || rSize <= maxSize || sSize <= maxSize) {
			rPartsFinal.add(r);
			sPartsFinal.add(s);
			// left refers to r
			if (rSize <= sSize) {
				shouldBuildLeft.add(true);
			} else {
				shouldBuildLeft.add(false);
			}
			return;
		}
		
		List<TempTable> rPartitions = partition(r.open(), this.r.schema(), this.field1, partitionNum, level);
		List<TempTable> sPartitions = partition(s.open(), this.s.schema(), this.field2, partitionNum, level);
		for (int i = 0; i < partitionNum; i++) {
			recursivePartition(
					rPartsFinal,
					sPartsFinal,
					shouldBuildLeft,
					rPartitions.get(i),
					sPartitions.get(i),
					level + 1,
					partitionNum);
		}
	}
	
	private List<TempTable> partition(Scan cursor, Schema schema, String field, int partitionNum, int level) {
		List<TempTable> result = new ArrayList<>();
		List<UpdateScan> writableResult = new ArrayList<>();
		// Create paritionNum (correspond to k) number of empty List<TempTable>
		for (int i = 0; i < partitionNum; i++) {
			TempTable tmp = new TempTable(tx, schema);
			result.add(tmp);
			writableResult.add(tmp.open());
		}
		// ACtual partition logic
		while (cursor.next()) {
			Constant fieldVal = cursor.getVal(field);
			// hashCode correspond the index in result / writableResult
			int idx = hash(fieldVal, partitionNum, level);
			UpdateScan targetTable = writableResult.get(idx);
			targetTable.insert();
			
			for (String fieldName : schema.fields()) {
				targetTable.setVal(fieldName, cursor.getVal(fieldName));
			}
			System.out.println("fieldVal is mapped to idx " + idx);
		}
		
		// Close all the tables that we opened
		for (UpdateScan t : writableResult) {
			t.close();
		}
		cursor.close();
		System.out.println("DEBUG" + result.toString());
		return result;
	}
	
	private int hash(Constant value, int partitionNum, int level) {
		long hashCode = Math.abs((long) value.hashCode());
		// Generate a different hash function for different level
		for (int i = 0; i < level; i++) {
			hashCode = hashCode / partitionNum;
		}
		
		return (int) (hashCode % partitionNum);
	}
	
	private int numBlocks(TempTable t) {
		return this.tx.size(t.tableName() + ".tbl");
	}
	
	// -------------------- end of Open() method ---------------------
	public int blocksAccessed() {
		int B = this.tx.availableBuffs();
		int partitionNum = Math.max(2, B - 1); // Set to 2 cause we cant divide with 0 = Math.Log(1)
		int maxSize = Math.max(1, B - 2);
		int M = this.r.blocksAccessed();
		int N = this.s.blocksAccessed();
		
		int small = Math.min(M, N);
		int numPasses = (int) Math.ceil(Math.log((double) small / maxSize) / Math.log(partitionNum));
		numPasses = Math.max(1, Math.min(numPasses, MAX_LEVEL + 1));
		return (2 * numPasses + 1) * (M + N);
	}
	
   public int recordsOutput() {
	    int maxvals = Math.max(this.r.distinctValues(field1),
	                           this.s.distinctValues(field2));
	    return (this.r.recordsOutput() * this.s.recordsOutput()) / maxvals;
   }
   
   public Schema schema() {
	   return this.schema;
   }
   
   public int distinctValues(String fldname) {
	   if (this.r.schema().hasField(fldname)) {
		   return this.r.distinctValues(fldname);
	   } else {
		   return this.s.distinctValues(fldname);
	   }
   }
}
