/*
 * ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.31
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * -----------------------------------------------------------------------------
 */

package colajava;

public class PointRep {
	private long swigCPtr;
	protected boolean swigCMemOwn;
	
	protected PointRep(long cPtr, boolean cMemoryOwn) {
		swigCMemOwn = cMemoryOwn;
		swigCPtr = cPtr;
	}
	
	protected static long getCPtr(PointRep obj) {
		return (obj == null) ? 0 : obj.swigCPtr;
	}
	
	@Override
	protected void finalize() {
		delete();
	}
	
	public synchronized void delete() {
		if (swigCPtr != 0 && swigCMemOwn) {
			swigCMemOwn = false;
			colaJNI.delete_PointRep(swigCPtr);
		}
		swigCPtr = 0;
	}
	
	public PointRep(SWIGTYPE_p_Point p) {
		this(colaJNI.new_PointRep(SWIGTYPE_p_Point.getCPtr(p)), true);
	}
	
	public boolean follow_inner(PointRep target) {
		return colaJNI.PointRep_follow_inner(swigCPtr, this, PointRep.getCPtr(target), target);
	}
	
	public void setPoint(SWIGTYPE_p_Point value) {
		colaJNI.PointRep_point_set(swigCPtr, this, SWIGTYPE_p_Point.getCPtr(value));
	}
	
	public SWIGTYPE_p_Point getPoint() {
		long cPtr = colaJNI.PointRep_point_get(swigCPtr, this);
		return (cPtr == 0) ? null : new SWIGTYPE_p_Point(cPtr, false);
	}
	
	public void setInner_set(SWIGTYPE_p_std__setTAvoid__PointRep_p_t value) {
		colaJNI.PointRep_inner_set_set(swigCPtr, this, SWIGTYPE_p_std__setTAvoid__PointRep_p_t.getCPtr(value));
	}
	
	public SWIGTYPE_p_std__setTAvoid__PointRep_p_t getInner_set() {
		long cPtr = colaJNI.PointRep_inner_set_get(swigCPtr, this);
		return (cPtr == 0) ? null : new SWIGTYPE_p_std__setTAvoid__PointRep_p_t(cPtr, false);
	}
	
}
