package ultrabot;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import battlecode.common.*;



class Soldier extends Bot{
	boolean defend = false;
	boolean foundEnemyArchon = false;
	boolean atEnemyArchonLocation = false;
	boolean enemyArchonIsDead = false;
	boolean hasTargetRobot = false;
	RobotInfo enemyArchon;
	RobotInfo targetRobot;
	List<RobotInfo> knownRobots = new ArrayList<RobotInfo>();
	/**
	 * The strategy for the soldier:
	 *  1) Will attack unless told otherwise. This means that it will
	 *  try to move towards the enemy archon location, shooting anything
	 *  in its way
	 *  2) If told to defend, it will do the same, but move towards the
	 *  friendly archon
	 */

	@Override
	protected void beforeStep() throws GameActionException{
		super.beforeStep();
		receiveEnemyArchonLocations();
		sortEnemyArchonLocations();
		// Do something when the soldier is spawned
	}

	@Override
	protected void mainStep() throws GameActionException{
		super.mainStep();
		// Why is this here? 
		receiveEnemyArchonLocations();
		if(defend){
			//defend();
		} else{
			attack();
		}
		if(!hasTargetRobot){
			senseEnemyRobots();
		}
	}

	@Override
	protected void afterStep() throws GameActionException{
		super.afterStep();
		System.out.println(Clock.getBytecodeNum());
		//Do something after each round
	}

	protected void sortEnemyArchonLocations(){
		Collections.sort(enemyArchonLocations,
			(m1, m2) -> Float.compare(
				distToLoc(m1), distToLoc(m2)));
	}

	protected void attack() throws GameActionException{
		if(atEnemyArchonLocation && !enemyArchonIsDead){
			attackEnemyArchon();
		} else{
			if(hasTargetRobot){
				attackTargetRobot();
			} else if(!enemyArchonIsDead){
				// LOCATION FIX !!!
				tryMove(enemyArchonLocations.get(0));
				if(rc.canSenseLocation(enemyArchonLocations.get(0))){
					atEnemyArchonLocation = true;
				}
			}
		}
	}

	protected void attackEnemyArchon() throws GameActionException{
		if(!foundEnemyArchon){
			RobotInfo[] closeRobots =  rc.senseNearbyRobots();
			for(int i=0; i<closeRobots.length; i++){
				if(closeRobots[i].getType().equals(RobotType.ARCHON) 
				&& closeRobots[i].getTeam().equals(rc.getTeam().opponent())){
					//found enemy archon
					enemyArchon = closeRobots[i];
					// LOCATION FIX !!!!
					enemyArchonLocations.add(0, enemyArchon.getLocation());
					foundEnemyArchon = true;
					break;
				}
			}
			if(!foundEnemyArchon){
				enemyArchonLocations.remove(0);
				if(enemyArchonLocations.size() == 0){
					foundEnemyArchon = true;
					enemyArchonIsDead = true;
				}
			}
		} else{
			// Do the attack here
			if(rc.canFirePentadShot()){
				// assuming that the archon is not moving
				rc.firePentadShot(new Direction(rc.getLocation(), enemyArchonLocations.get(0)));
			}
		}
	}

	protected void senseEnemyRobots(){
		List<RobotInfo> newRobots = Arrays.asList(rc.senseNearbyRobots(rc.getType().sensorRadius, 
		rc.getTeam().opponent()));
		knownRobots.removeAll(newRobots);
		// Could use this information to report back the location of the archon maybe
		knownRobots.addAll(newRobots);
		sortKnownRobots();
		setTargetRobot();
	}

	protected void setTargetRobot(){
		if(!hasTargetRobot && knownRobots.size() > 0){
			for(int i=0; i<knownRobots.size(); i++){
				if(knownRobots.get(i).getType().equals(RobotType.SOLDIER)){
					hasTargetRobot = true;
					targetRobot = knownRobots.remove(i);
					break;
				}
			}
			if(!hasTargetRobot){
				hasTargetRobot = true;
				targetRobot = knownRobots.remove(0);
			}
		}
		
	}

	protected void attackTargetRobot() throws GameActionException{
		if(rc.canSenseRobot(targetRobot.getID())){
			//attack
			targetRobot = rc.senseRobot(targetRobot.getID()); 
			if(rc.canFirePentadShot() && distToLoc(targetRobot.getLocation()) < 3.5f ){
				rc.firePentadShot(new Direction(rc.getLocation(), targetRobot.getLocation()));
			} else if(rc.canFireTriadShot()){
				rc.fireTriadShot(new Direction(rc.getLocation(), targetRobot.getLocation()));
			} else if(rc.canFireSingleShot()){
				rc.fireSingleShot(new Direction(rc.getLocation(), targetRobot.getLocation()));
			}
			if(!rc.hasMoved()){
				avoidBullets(targetRobot);
			}
		} else{
			List<RobotInfo> nearbyRobots = Arrays.asList(rc.senseNearbyRobots(
					rc.getType().sensorRadius, rc.getTeam().opponent()));
			if(nearbyRobots.size() > 0){
				targetRobot = nearbyRobots.get(0);
				attackTargetRobot();
			} else if(distToLoc(targetRobot.getLocation()) < rc.getType().sensorRadius){
				//probably dead
				hasTargetRobot = false;
			} else{
				tryMove(targetRobot.getLocation());
			}
		}
	}

	protected void sortKnownRobots(){
		Collections.sort(knownRobots,
				(t1, t2) -> Float.compare(distToLoc(t1.location), (distToLoc(t2.location))));
	}
}