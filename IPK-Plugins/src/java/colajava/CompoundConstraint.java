/*
 * ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.31
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * -----------------------------------------------------------------------------
 */

package colajava;

public class CompoundConstraint {
	private long swigCPtr;
	protected boolean swigCMemOwn;
	
	protected CompoundConstraint(long cPtr, boolean cMemoryOwn) {
		swigCMemOwn = cMemoryOwn;
		swigCPtr = cPtr;
	}
	
	protected static long getCPtr(CompoundConstraint obj) {
		return (obj == null) ? 0 : obj.swigCPtr;
	}
	
	@Override
	protected void finalize() {
		delete();
	}
	
	public synchronized void delete() {
		if (swigCPtr != 0 && swigCMemOwn) {
			swigCMemOwn = false;
			colaJNI.delete_CompoundConstraint(swigCPtr);
		}
		swigCPtr = 0;
	}
	
	public void generateVariables(SWIGTYPE_p_std__vectorTvpsc__Variable_p_t vars) {
		colaJNI.CompoundConstraint_generateVariables(swigCPtr, this, SWIGTYPE_p_std__vectorTvpsc__Variable_p_t.getCPtr(vars));
	}
	
	public void generateSeparationConstraints(SWIGTYPE_p_std__vectorTvpsc__Variable_p_t vars, SWIGTYPE_p_std__vectorTvpsc__Constraint_p_t cs) {
		colaJNI.CompoundConstraint_generateSeparationConstraints(swigCPtr, this, SWIGTYPE_p_std__vectorTvpsc__Variable_p_t.getCPtr(vars),
							SWIGTYPE_p_std__vectorTvpsc__Constraint_p_t.getCPtr(cs));
	}
	
	public void updatePosition() {
		colaJNI.CompoundConstraint_updatePosition(swigCPtr, this);
	}
	
}
