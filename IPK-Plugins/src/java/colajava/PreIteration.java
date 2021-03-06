/*
 * ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.31
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * -----------------------------------------------------------------------------
 */

package colajava;

public class PreIteration {
	private long swigCPtr;
	protected boolean swigCMemOwn;
	
	protected PreIteration(long cPtr, boolean cMemoryOwn) {
		swigCMemOwn = cMemoryOwn;
		swigCPtr = cPtr;
	}
	
	protected static long getCPtr(PreIteration obj) {
		return (obj == null) ? 0 : obj.swigCPtr;
	}
	
	@Override
	protected void finalize() {
		delete();
	}
	
	public synchronized void delete() {
		if (swigCPtr != 0 && swigCMemOwn) {
			swigCMemOwn = false;
			colaJNI.delete_PreIteration(swigCPtr);
		}
		swigCPtr = 0;
	}
	
	public PreIteration(ColaLocks locks, ColaResizes resizes) {
		this(colaJNI.new_PreIteration__SWIG_0(ColaLocks.getCPtr(locks), locks, ColaResizes.getCPtr(resizes), resizes), true);
	}
	
	public PreIteration(ColaLocks locks) {
		this(colaJNI.new_PreIteration__SWIG_1(ColaLocks.getCPtr(locks), locks), true);
	}
	
	public PreIteration() {
		this(colaJNI.new_PreIteration__SWIG_2(), true);
	}
	
	public PreIteration(ColaResizes resizes) {
		this(colaJNI.new_PreIteration__SWIG_3(ColaResizes.getCPtr(resizes), resizes), true);
	}
	
	public void setLocks(ColaLocks value) {
		colaJNI.PreIteration_locks_set(swigCPtr, this, ColaLocks.getCPtr(value), value);
	}
	
	public ColaLocks getLocks() {
		return new ColaLocks(colaJNI.PreIteration_locks_get(swigCPtr, this), false);
	}
	
	public void setResizes(ColaResizes value) {
		colaJNI.PreIteration_resizes_set(swigCPtr, this, ColaResizes.getCPtr(value), value);
	}
	
	public ColaResizes getResizes() {
		return new ColaResizes(colaJNI.PreIteration_resizes_get(swigCPtr, this), false);
	}
	
	public void setChanged(boolean value) {
		colaJNI.PreIteration_changed_set(swigCPtr, this, value);
	}
	
	public boolean getChanged() {
		return colaJNI.PreIteration_changed_get(swigCPtr, this);
	}
	
}
