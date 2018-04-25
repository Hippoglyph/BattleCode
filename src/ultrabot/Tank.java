package ultrabot;

import battlecode.common.*;

class Tank extends Bot{
	
	//Attacking
	RobotInfo enemyArchon;
	boolean foundArchon = false;
	boolean enemyArchonIsDead = false;
	boolean noNearByEnemies = false;
	
	@Override
	protected void beforeStep() throws GameActionException{
		super.beforeStep();
		// Do something when the Archon is spawned
		receiveEnemyArchonLocations();
	}

	@Override
	protected void mainStep() throws GameActionException{
		super.mainStep();
		//scanForEnemies();
		if(noNearByEnemies)
		{
			moveToEnemyBase();
		}
		else
		{
			//Need to Implement
			//shootAtEnemy();
		}
	}

	@Override
	protected void afterStep() throws GameActionException{
		super.afterStep();
		//Do something after each round
	}
	
	protected void moveToEnemyBase() throws GameActionException{
		// LOCATION FIX !!!!
		tryMove(enemyArchonLocations.get(0));
	}
	
	protected void attackEnemyBase() throws GameActionException{
		if(!foundArchon){
			// LOCATION FIX !!!!
			if(distToLoc(enemyArchonLocations.get(0)) < rc.getType().sensorRadius){
				scanForEnemyArchon();
			} else{
				// LOCATION FIX !!!!
				tryMove(enemyArchonLocations.get(0));
			}
		} else if(!enemyArchonIsDead){
			attackEnemyArchon();
		} else if(!noNearByEnemies){
			//strikeNearByEnemies();
		} else{
			//headToHomeBase(); ???
		}
	}
	
	protected void scanForEnemyArchon() {
		RobotInfo[] closeRobots =  rc.senseNearbyRobots();
		for(int i=0; i<closeRobots.length; i++){
			if(closeRobots[i].getType().equals(RobotType.ARCHON) 
			&& closeRobots[i].getTeam().equals(rc.getTeam().opponent())){
				//found enemy archon
				enemyArchon = closeRobots[i];
				// LOCATION FIX !!!!
				enemyArchonLocations.add(0, enemyArchon.getLocation());
				foundArchon = true;
				break;
			}
		}
		if(!foundArchon){
			foundArchon = true;
			enemyArchonIsDead = true; // Is this needed?
		}
	}
	
	protected void attackEnemyArchon() throws GameActionException{
		// LOCATION FIX !!!!
		if(distToLoc(enemyArchonLocations.get(0)) < 2.0f){
			if(rc.canSenseRobot(enemyArchon.getID())){
				rc.strike();
			} else{
				enemyArchonIsDead = true;
			}
		} else{
			// LOCATION FIX !!!
			tryMove(enemyArchonLocations.get(0));
		}
	}
}