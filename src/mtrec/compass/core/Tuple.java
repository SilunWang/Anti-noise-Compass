package mtrec.compass.core;

import java.util.ArrayList;

public class Tuple {
	public double x;
	public double y;
	public double z;

	public Tuple(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void setTuple(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static double mul(Tuple A, Tuple B) {
		return B.x * A.x + B.y * A.y + B.z * A.z;
	}
	
	public static Tuple cross(Tuple A,Tuple B){
		return new Tuple(A.y*B.z-B.y*A.z,A.z*B.x-B.z*A.x,A.x*B.y-B.x*A.y);
	}

	public static Tuple sub(Tuple A, Tuple B) {
		return new Tuple(B.x - A.x, B.y - A.y, B.z - A.z);
	}

	public static double mod(Tuple A) {
		return Math.sqrt(A.x * A.x + A.y * A.y + A.z * A.z);
	}

	public static double dis(Tuple A, Tuple B){
		return Math.sqrt((A.x-B.x)*(A.x-B.x)+(A.y-B.y)*(A.y-B.y)+(A.z-B.z)*(A.z-B.z));
	}
	
	public String toString() {
		return x + "," + y + "," + z;
	}
	
	public static Tuple reverse(Tuple A){
		return new Tuple(-A.x,-A.y,-A.z);
	}
	
	public static Tuple normalize(Tuple A){
		double length = Math.sqrt(A.x*A.x+A.y*A.y+A.z*A.z);
		if(length==0) return new Tuple(1,0,0);
		return new Tuple(A.x/length,A.y/length,A.z/length);
	}
	
	public static ArrayList<Tuple> String2List(String res) {
		String[] ress = res.split("\n");
		ArrayList<Tuple> tuples = new ArrayList<Tuple>();
		for (int i = 0; i < ress.length; i++) {
			String[] TupleStr = ress[i].split(",");
			Tuple tuple = new Tuple(Double.parseDouble(TupleStr[0]),
					Double.parseDouble(TupleStr[1]),
					Double.parseDouble(TupleStr[2]));
			tuples.add(tuple);
		}
		return tuples;
	}
}

