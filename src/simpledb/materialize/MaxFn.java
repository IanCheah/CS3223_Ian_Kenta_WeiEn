package simpledb.materialize;

import simpledb.query.*;
import simpledb.record.Schema;

/**
 * The <i>max</i> aggregation function.
 * 
 * @author Edward Sciore
 */
public class MaxFn implements AggregationFn {
	private String fldname;
	private Constant val;

	/**
	 * Create a max aggregation function for the specified field.
	 * 
	 * @param fldname the name of the aggregated field
	 */
	public MaxFn(String fldname) {
		this.fldname = fldname;
	}

	public void reset() {
		val = null;
	}

	/**
	 * Start a new maximum to be the field value in the current record.
	 * 
	 * @see simpledb.materialize.AggregationFn#processFirst(simpledb.query.Scan)
	 */
	public void processFirst(Scan s) {
		reset();
		processNext(s);
	}

	/**
	 * Replace the current maximum by the field value in the current record, if it
	 * is higher.
	 * 
	 * @see simpledb.materialize.AggregationFn#processNext(simpledb.query.Scan)
	 */
	public void processNext(Scan s) {
		Constant newval = s.getVal(fldname);
		if (val == null || newval.compareTo(val) > 0) {
			val = newval;
		}

	}

	/**
	 * Return the field's name, prepended by "maxof".
	 * 
	 * @see simpledb.materialize.AggregationFn#fieldName()
	 */
	public String fieldName() {
		return "maxof" + fldname;
	}

	public String inputField() {
		return fldname;
	}

	/**
	 * Return the current maximum.
	 * 
	 * @see simpledb.materialize.AggregationFn#value()
	 */
	public Constant value() {
		if (val == null) {
			throw new IllegalStateException();
		}

		return val;
	}

	/**
	 * Adds a maximum-result field with the input field's SQL type and declared
	 * length, preserving string capacity where applicable.
	 *
	 * @param input  the schema containing the aggregated field
	 * @param output the schema to receive the maximum field
	 * @throws IllegalArgumentException if the input field does not exist
	 */
	public void addResultField(Schema input, Schema output) {
		if (!input.hasField(fldname)) {
			throw new IllegalArgumentException("Unknown aggregate field: " + fldname);
		}
		output.addField(fieldName(), input.type(fldname), input.length(fldname));
	}

	public boolean hasValueForEmptyInput() {
		return false;
	}
}
