package com.philriesch.android.facerec.common.data;

/**
 * @SVN $Id: RectangleRoiData.java 116 2014-12-14 22:17:17Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class RectangleRoiData {

	public final int xa;
	
	public final int ya;
	
	public final int xb;
	
	public final int yb;
	
	public RectangleRoiData (int xa, int ya, int xb, int yb) {
		this.xa = xa;
		this.ya = ya;
		this.xb = xb;
		this.yb = yb;
	}
	
	public int XA () {
		return this.xa;
	}
	
	public int YA () {
		return this.ya;
	}
	
	public int XB () {
		return this.xb;
	}
	
	public int YB () {
		return this.yb;
	}
	
	public int Width () {
		return this.xb - this.xa;
	}
	
	public int Height () {
		return this.yb - this.ya;
	}
	
	@Override
	public String toString () {
		String s = "ROI={(" + this.xa + ", " + this.ya + "); (" + this.xb + ", " + this.yb + ")}";
		return s;
	}
	
}
