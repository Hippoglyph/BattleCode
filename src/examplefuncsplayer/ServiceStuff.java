package examplefuncsplayer;

import battlecode.common.Direction;

public class ServiceStuff {

	public static final float PI = (float)Math.PI;

	public static double getCircleIntersectArea(float radius1, float radius2, float dist) {
		
		if (dist >= radius1+radius2) {
			return 0.0;
		}

		float r2 = radius1*radius1;
		float R2 = radius2*radius2;
		
		float smallerRadius = radius1;
		float biggerRadius = radius2;
		if (smallerRadius > biggerRadius) {
			smallerRadius = radius2;
			biggerRadius = radius1;
		}
		
		if (dist < biggerRadius-smallerRadius) {
			return PI*smallerRadius*smallerRadius;
		}
		
		float d2 = dist*dist;
		return r2 * Math.acos((r2 + d2 - R2)/(2*dist*radius1)) + R2 * Math.acos((R2 + d2 - r2)/(2*dist*radius2)) -
				0.5*Math.sqrt((-dist+radius1+radius2) * (dist+radius1-radius2) * (dist-radius1+radius2)*(dist+radius1+radius2));
	}

	public static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }
	
	
}
