/*
 * ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.31
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * -----------------------------------------------------------------------------
 */

package colajava;

public class ColaLocks {
	private long swigCPtr;
	protected boolean swigCMemOwn;
	
	protected ColaLocks(long cPtr, boolean cMemoryOwn) {
		swigCMemOwn = cMemoryOwn;
		swigCPtr = cPtr;
	}
	
	protected static long getCPtr(ColaLocks obj) {
		return (obj == null) ? 0 : obj.swigCPtr;
	}
	
	@Override
	protected void finalize() {
		delete();
	}
	
	public synchronized void delete() {
		if (swigCPtr != 0 && swigCMemOwn) {
			swigCMemOwn = false;
			colaJNI.delete_ColaLocks(swigCPtr);
		}
		swigCPtr = 0;
	}
	
	public ColaLocks() {
		this(colaJNI.new_ColaLocks__SWIG_0(), true);
	}
	
	public ColaLocks(long n) {
		this(colaJNI.new_ColaLocks__SWIG_1(n), true);
	}
	
	public long size() {
		return colaJNI.ColaLocks_size(swigCPtr, this);
	}
	
	public long capacity() {
		return colaJNI.ColaLocks_capacity(swigCPtr, this);
	}
	
	public void reserve(long n) {
		colaJNI.ColaLocks_reserve(swigCPtr, this, n);
	}
	
	public boolean isEmpty() {
		return colaJNI.ColaLocks_isEmpty(swigCPtr, this);
	}
	
	public void clear() {
		colaJNI.ColaLocks_clear(swigCPtr, this);
	}
	
	public void add(Lock x) {
		colaJNI.ColaLocks_add(swigCPtr, this, Lock.getCPtr(x), x);
	}
	
	public Lock get(int i) {
		return new Lock(colaJNI.ColaLocks_get(swigCPtr, this, i), false);
	}
	
	public void set(int i, Lock x) {
		colaJNI.ColaLocks_set(swigCPtr, this, i, Lock.getCPtr(x), x);
	}
	
}
