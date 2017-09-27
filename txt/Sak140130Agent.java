package ctf.agent;


import java.awt.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import ctf.common.AgentEnvironment;
import ctf.agent.Agent;

import ctf.common.AgentAction;




public class Sak140130Agent extends Agent {
	int x,y;
	boolean westTeam = false;
	boolean northSide = false;
	boolean integrated = false;
	int step = 0;
	static Random r = new Random();
	
	Scanner keyboard = new Scanner(System.in);
	MapManager mm;
	
	Sak140130Agent otheragent;
	static Sak140130Agent lastInitialized = null;
	
	
	
	
	public Sak140130Agent(){
		mm = new MapManager(this);
		if(lastInitialized == null){
			lastInitialized = this;
			
		}else{
			otheragent = lastInitialized;
			otheragent.otheragent = this;
			lastInitialized = null;
		}
	}
	
	public static class Location implements Comparable{
		public int x,y;
		public Location(int x, int y){
			this.x=x;
			this.y=y;
		}
		@Override
		public int compareTo(Object arg0) {
			Location l2 = (Location) arg0;
			int r = Integer.compare(this.x, l2.x);
			if(r!=0) return r;
			return Integer.compare(this.y, l2.y);
		}
		
		@Override
		public boolean equals(Object o){
			Location l2 = (Location) o;
			return x==l2.x&&y==l2.y;
		}
		//assumes they are adjacent
		public static int howToGetTo(Location from, Location to){
			
			if(from.x < to.x) return AgentAction.MOVE_EAST;
			if(from.x > to.x) return AgentAction.MOVE_WEST;
			if(from.y < to.y) return AgentAction.MOVE_NORTH;
			if(from.y > to.y) return AgentAction.MOVE_SOUTH;
			return AgentAction.DO_NOTHING;
		}
		@Override
		public String toString(){
			return "("+x+", "+y+")";
		}
		
		@Override
		public int hashCode(){
			return x^y;
		}
		
		
	}
	
	public enum Entity{
		UNKNOWN, EMPTY, WALL, MINE, BASE;
	}
	
	
	
	// implements Agent.getMove() interface
	boolean firstMove = true;
	@Override
	public int getMove(AgentEnvironment ae){ //used for updating state
		step++;
		System.out.println("Step " + step);
		if(firstMove){
			mm.map.put(new Location(0,0), Entity.EMPTY);
			mm.certain.add(new Location(0,0));
			
			this.northSide = ae.isBaseSouth(AgentEnvironment.OUR_TEAM, false);
			this.westTeam = ae.isBaseEast(AgentEnvironment.ENEMY_TEAM, false);
			firstMove = false;
			if(mm.map.size() > 2){
				System.out.println(mm.map.toString());
			}
			if(northSide) mm.predictNextMap(ae); //this is because one predictNextMap triggers another in the other agent - only 1/2 should initiate the prediction.
			
		}
		this.processTag(ae);
		mm.updateMap(ae);
		
		
		int result = findMove(ae);
		
		if(mm.width != null){
			for(Location loc : mm.certain){
				Location cl = this.mm.convertLocation(loc, northSide, westTeam, this.otheragent.northSide, this.otheragent.westTeam, mm.width);
				if(!otheragent.mm.certain.contains(cl)){
					otheragent.mm.certain.add(cl);
					otheragent.mm.map.put(cl, mm.map.get(loc));
				}
			}
			if(otheragent.mm.width == null){
				otheragent.mm.width = mm.width;
				otheragent.mm.flagLevel = mm.flagLevel;
				if(northSide){
					otheragent.mm.flagLevel += mm.width; 
				}else{
					otheragent.mm.flagLevel -= mm.width; 
				}
			}
		}
		
		
		
		if(result == AgentAction.MOVE_NORTH){
			y++;
		}else if(result == AgentAction.MOVE_SOUTH){
			y--;
		}else if(result == AgentAction.MOVE_EAST){
			x++;
		}else if(result == AgentAction.MOVE_WEST){
			x--;
		}else if(result == AgentAction.PLANT_HYPERDEADLY_PROXIMITY_MINE){
			mm.map.put(new Location(x,y), Entity.MINE);
			
			otheragent.mm.map.put(mm.convertLocation(new Location(x,y), northSide, westTeam, otheragent.northSide, otheragent.westTeam, mm.width), Entity.MINE);
		}
		return result;
		
	}
	
	public Location enemyBase(AgentEnvironment ae){
		Integer x=0;
		Integer y=0;
		if(mm.flagLevel != null) y= mm.flagLevel;
		else{
			if(ae.isBaseSouth(AgentEnvironment.ENEMY_TEAM, false)) y=Integer.MIN_VALUE;
			if(ae.isBaseNorth(AgentEnvironment.ENEMY_TEAM, false)) y=Integer.MAX_VALUE;
		}
		if(mm.width != null){
			if(!westTeam){
				x=-mm.width+1;
			}else{
				x=mm.width-1;
			}
		}else{
			if(ae.isBaseEast(AgentEnvironment.ENEMY_TEAM, false)) x=Integer.MAX_VALUE;
			if(ae.isBaseWest(AgentEnvironment.ENEMY_TEAM, false)) x=Integer.MIN_VALUE;
		}
		System.out.println("Enemy base is at " + x + ", " + y);
		return new Location(x,y);
	}
	
	
	public int findMove( AgentEnvironment ae ) {
		if(Math.max(Math.abs(x), Math.abs(y)) > mm.maxRadius){
			mm.maxRadius = Math.max(Math.abs(x), Math.abs(y));
		}
		Location here = new Location(x,y);
		
		
		if( mm.width != null &&
			(ae.isAgentEast(AgentEnvironment.ENEMY_TEAM, true) || ae.isAgentWest(AgentEnvironment.ENEMY_TEAM, true) || ae.isAgentNorth(AgentEnvironment.ENEMY_TEAM, true) || ae.isAgentSouth(AgentEnvironment.ENEMY_TEAM, true))	
			
			&& !(ae.hasFlag() && !ae.hasFlag(AgentEnvironment.ENEMY_TEAM))
			&& (
			//they have the flag and you personally don't
			(ae.hasFlag(AgentEnvironment.ENEMY_TEAM) && !ae.hasFlag()) 
			//or your team has it and you personally don't - clear the way 	
			|| (ae.hasFlag(AgentEnvironment.OUR_TEAM) && ! ae.hasFlag())
			//Neither team has the flag, but they are on your side, so they lose more
			|| (!ae.hasFlag(AgentEnvironment.OUR_TEAM) && !ae.hasFlag(AgentEnvironment.ENEMY_TEAM) && mm.aStarTo(here, this.enemyBase(ae), ae).size() > mm.aStarTo(here, new Location(0, mm.flagLevel), ae).size())  
			//You are carrying the flag back to base, but so are they, and they are closer.
			|| (ae.hasFlag() && ae.hasFlag(AgentEnvironment.ENEMY_TEAM) && mm.aStarTo(here, this.enemyBase(ae), ae).size() < mm.aStarTo(here, new Location(0, mm.flagLevel), ae).size()))
				
			){
			if(ae.isAgentEast(AgentEnvironment.ENEMY_TEAM, true)) return AgentAction.MOVE_EAST;
			if(ae.isAgentWest(AgentEnvironment.ENEMY_TEAM, true)) return AgentAction.MOVE_WEST;
			if(ae.isAgentNorth(AgentEnvironment.ENEMY_TEAM, true)) return AgentAction.MOVE_NORTH;
			if(ae.isAgentSouth(AgentEnvironment.ENEMY_TEAM, true)) return AgentAction.MOVE_SOUTH;
			
		}
		
		while(otheragent.mm.knownMaps.size() < this.mm.knownMaps.size()){
			mm.predictNextMap(ae);
		}
		
		ArrayList<Location> route = mm.aStarTo(here, getGoal(ae), ae);
		System.out.println(route.toString() + "{" + x + ", " + y + "}" + " Distance from enemy base " + this.mm.manhattanDistance(new Location(x,y), this.enemyBase(ae)));
		
		/*if(ae.hasFlag(AgentEnvironment.OUR_TEAM) &&
			!ae.isAgentEast(AgentEnvironment.ENEMY_TEAM, true) &&
			!ae.isAgentWest(AgentEnvironment.ENEMY_TEAM, true) &&
			!ae.isAgentSouth(AgentEnvironment.ENEMY_TEAM, true) &&
			!ae.isAgentNorth(AgentEnvironment.ENEMY_TEAM, true) &&
			this.mm.manhattanDistance(new Location(x,y), this.enemyBase(ae)) == 1
			
				){
			
			
			
			int numMinedEntrances = 0;
			ArrayList<Location> entrances = new ArrayList<>();
			Location enemyBase = this.enemyBase(ae);
			entrances.add(new Location(enemyBase.x, enemyBase.y + 1));
			entrances.add(new Location(enemyBase.x, enemyBase.y - 1));
			entrances.add(new Location(enemyBase.x-1, enemyBase.y));
			entrances.add(new Location(enemyBase.x+1, enemyBase.y));
			for(Location l : entrances){
				if(!mm.isOutOfBounds(l) && mm.map.get(l) == Entity.MINE || mm.map.get(l) == Entity.WALL){
					numMinedEntrances++;
				}
			}
			if(mm.map.get(new Location(x,y)) == Entity.MINE){
				numMinedEntrances += 100; //don't try to plant another mine please
			}
			
			if(numMinedEntrances < 2){
				return AgentAction.PLANT_HYPERDEADLY_PROXIMITY_MINE;
			}else{
				System.out.println("Could have placed a mine. But there were " + numMinedEntrances + " already blocked.");
			}
			
		}*/
		
		
		if(route.size() > 0)return Location.howToGetTo(new Location(x,y), route.get(route.size()-1));
		
		
		System.out.println("Nothing to do");
		return AgentAction.DO_NOTHING;
		/*
		if(input == 8) return AgentAction.MOVE_NORTH;
		if(input == 2) return AgentAction.MOVE_SOUTH;
		if(input == 4) return AgentAction.MOVE_WEST;
		if(input == 6) return AgentAction.MOVE_EAST;
		if(input == 5) return AgentAction.DO_NOTHING;
		if(input == 0) return AgentAction.PLANT_HYPERDEADLY_PROXIMITY_MINE;
		return AgentAction.DO_NOTHING;*/

	}
	
	public Location findInBoundsVersion(int x, int y){
		if(mm.width != null){
			if(Math.abs(0.0+x) > mm.width - 1){
				int sign = Integer.signum(x);
				x = mm.width - 1;
				
				x *= sign;
			}if(Math.abs(0.0+y) > mm.width - 1){
				int sign = Integer.signum(y);
				y = mm.width - 1;
				
				y *= sign;
			}
		}
		
		if(northSide){
			if(y > 0) y = 0;
			
		}else{
			if(y < 0) y = 0;
		}
		if(westTeam){
			if(x < 0) x = 0;
		}else{
			if(x > 0) x = 0;
		}
		
		
		return new Location(x,y);
	}
	
	public Location getGoal(AgentEnvironment ae){
		
		int x = this.x;
		int y = this.y;
		
		if(!integrated){
			if(mm.flagLevel == null && mm.width == null){
				if(ae.isBaseEast(AgentEnvironment.ENEMY_TEAM, false)) x=Integer.MAX_VALUE;
				if(ae.isBaseWest(AgentEnvironment.ENEMY_TEAM, false)) x=Integer.MIN_VALUE;
				if(ae.isBaseSouth(AgentEnvironment.ENEMY_TEAM, false)) y=Integer.MIN_VALUE;
				if(ae.isBaseNorth(AgentEnvironment.ENEMY_TEAM, false)) y=Integer.MAX_VALUE;
				
				
				
				
			}
			else if(mm.flagLevel != null){
				if(ae.hasFlag()){
					return new Location(0,mm.flagLevel);
				}
				
				else if(!ae.hasFlag() && ae.hasFlag(AgentEnvironment.ENEMY_TEAM) && mm.aStarTo(new Location(x,y), this.enemyBase(ae), ae).size() > (int) 1.5*mm.aStarTo(new Location(x,y), new Location(0,mm.flagLevel), ae).size()){
					
					if(ae.isFlagEast(AgentEnvironment.OUR_TEAM, false)) x=Integer.MAX_VALUE;
					if(ae.isFlagWest(AgentEnvironment.OUR_TEAM, false)) x=Integer.MIN_VALUE;
					if(ae.isFlagSouth(AgentEnvironment.OUR_TEAM, false)) y=Integer.MIN_VALUE;
					if(ae.isFlagNorth(AgentEnvironment.OUR_TEAM, false)) y=Integer.MAX_VALUE;
				}
				else if(!ae.hasFlag() && ae.hasFlag(AgentEnvironment.OUR_TEAM)){
					if(x==0 && y==mm.flagLevel){
						ArrayList<Location> neighbors = new ArrayList<>();
						if(!ae.isObstacleNorthImmediate() && mm.okMoveNorth(ae)) neighbors.add(new Location(x, y+1));
						if(!ae.isObstacleEastImmediate() && mm.okMoveEast(ae)) neighbors.add(new Location(x+1, y));
						if(!ae.isObstacleSouthImmediate() && mm.okMoveSouth(ae)) neighbors.add(new Location(x, y-1));
						if(!ae.isObstacleWestImmediate() &&  mm.okMoveWest(ae)) neighbors.add(new Location(x-1, y));
						return neighbors.get(r.nextInt(neighbors.size()));
					}else{
						ArrayList<Location> neighbors = new ArrayList<>();
						neighbors.add(new Location(0, mm.flagLevel+1));
						neighbors.add(new Location(1, mm.flagLevel));
						neighbors.add(new Location(0, mm.flagLevel-1));
						neighbors.add(new Location(-1, mm.flagLevel));
						ArrayList<Location> remove = new ArrayList<>();
						for(Location l : neighbors){
							Entity el = mm.map.get(l);
							if(el == Entity.WALL || mm.isOutOfBounds(l)) remove.add(l);
						}
						neighbors.removeAll(remove);
						
						return neighbors.get(r.nextInt(neighbors.size()));
						
						
					}
				}
				
				else if(mm.width == null){
					
					if(!westTeam)x=Integer.MIN_VALUE;
					else x=Integer.MAX_VALUE;
					y=mm.flagLevel;
				}else{
					if(!westTeam)x=-mm.width+1;
					else x=mm.width-1;
					y=mm.flagLevel;
				}
				
				
			}
			
			
			
		}else{
			//INTEGRATION
			if(ae.hasFlag()){
				if(westTeam){
					return new Location(0, mm.flagLevel);
				}else{
					return new Location(mm.width - 1, mm.width / 2);//replace flaglevel
				}
			}else{
				if(westTeam){
					return new Location(mm.width - 1, mm.width / 2);
				}else{
					return new Location(0, mm.width/2);
				}
			}
		}
		
		//let's assume we don't want an unbounded version
		
		return findInBoundsVersion(x,y);
		
	}
	boolean justGotTagged = false;
	public void processTag(AgentEnvironment ae){
		this.justGotTagged = false;
		if(didIJustGetTagged(ae)){
			justGotTagged = true;
			if(otheragent.justGotTagged){
				System.out.println("Flag captured - removing mines from internal map.");
				
				for(Location l : mm.map.keySet()){
					if(mm.map.get(l) == Entity.MINE){
						mm.map.put(l, Entity.EMPTY);
					}
				}
				for(Location l : otheragent.mm.map.keySet()){
					if(otheragent.mm.map.get(l) == Entity.MINE){
						otheragent.mm.map.put(l, Entity.EMPTY);
					}
				}
			}
			System.out.println("I just got tagged!");
			x=0;
			y=0;
			if(integrated){//INTEGRATION
				if(!westTeam){
					x=mm.width-1;
				}
				if(northSide){
					y=mm.width-1;
				}
			}
			
		}
	}
	
	public boolean didIJustGetTagged(AgentEnvironment ae){
		
		if(!ae.isBaseEast(AgentEnvironment.OUR_TEAM, false) && !ae.isBaseWest(AgentEnvironment.OUR_TEAM, false)){ //must be true for a tag
			
			if((ae.isObstacleNorthImmediate() && northSide) || (ae.isObstacleSouthImmediate() && !northSide)){
				System.out.println("Tagged - resetting location");
				return true;
			}
		}
		return false;
	}
	
	
	
	public static class MapManager{
		Map<Location, Entity> map = new HashMap<>();
		Set<Location> certain = new HashSet<>();
		
		Integer flagLevel = null;
		Integer width = null;
		Sak140130Agent esa;
		static Integer maxRadius = 10;
		
		boolean unknownTerritory = false;
		ArrayList<int[][]> knownMaps = new ArrayList<>();
		static int[][] empty = new int[][]{
			{0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0},
		};
		
		static int[][] simple = new int[][]{
			{0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0},
			{0,0,1,1,0,0,1,1,0,0},
			{0,0,1,1,0,0,1,1,0,0},
			{0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0},
			{0,0,1,1,0,0,1,1,0,0},
			{0,0,1,1,0,0,1,1,0,0},
			{0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0},
		};
		static int[][] traps = new int[][]{
			{0,0,1,0,0,0,0,0,0,0},
			{0,0,1,1,1,1,1,1,0,0},
			{0,0,0,0,0,0,0,1,0,0},
			{0,0,1,1,1,1,1,1,0,0},
			{0,0,1,0,0,0,0,0,0,0},
			{0,0,1,1,1,1,1,1,0,0},
			{0,0,0,0,0,0,0,1,0,0},
			{0,0,1,1,1,1,1,1,0,0},
			{0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0}
		};
		static int[][] wall = new int[][]{
			{0,0,0,0,1,1,0,0,0,0},
			{0,0,0,0,1,1,0,0,0,0},
			{0,0,0,0,1,1,0,0,0,0},
			{0,0,0,0,1,1,0,0,0,0},
			{0,0,0,0,1,1,0,0,0,0},
			{0,0,0,0,1,1,0,0,0,0},
			{0,0,0,0,1,1,0,0,0,0},
			{0,0,0,0,1,1,0,0,0,0},
			{0,0,0,0,1,1,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0}
		};
		static int[][] ex = new int[][]{
			{0,0,0,0,0,0,0,0,0,0},
			{0,1,0,0,0,0,0,0,1,0},
			{0,0,1,0,0,0,0,0,0,0},
			{0,0,0,1,0,0,1,0,0,0},
			{0,0,0,0,1,1,0,0,0,0},
			{0,0,0,0,1,1,0,0,0,0},
			{0,0,0,1,0,0,1,0,0,0},
			{0,0,0,0,0,0,0,1,0,0},
			{0,1,0,0,0,0,0,0,1,0},
			{0,0,0,0,0,0,0,0,0,0}
		};
		
		
		public MapManager(Sak140130Agent esa){
			this.esa=esa;
			maxRadius = 10;
			knownMaps.add(empty);
			knownMaps.add(empty);
			knownMaps.add(simple);
			knownMaps.add(traps);
			knownMaps.add(wall);
			knownMaps.add(ex);
		}
		
		
		
		public void predictNextMap(AgentEnvironment ae){
			if(unknownTerritory) return;
			
			
			
			for(int x = 0; x < 10; x++){
				for(int y = 0; y < 10; y++){
					Location l = new Location(x,y);
					Location cl = convertLocation(l, false, true, esa.northSide, esa.westTeam, 10);
					if(!certain.contains(cl) && map.containsKey(cl)){
						map.remove(cl);
					}
					
				}
			}
			
			
			
			if(knownMaps.size() == 0){
				this.unknownTerritory = true;
				
				return;
			}
			
			
			
			int[][] nmap = knownMaps.get(0);
			knownMaps.remove(0);
			
			
			
			for(int x = 0; x < 10; x++){
				for(int y = 0; y < 10; y++){
					Location l = new Location(x,y);
					Location cl = convertLocation(l, false, true, esa.northSide, esa.westTeam, 10);
										
					if(nmap[9-y][x] == 1){
						if(map.get(cl) != Entity.WALL && map.get(cl) != null){
							predictNextMap(ae);
							return;
						}else{
							map.put(cl, Entity.WALL);
						}
						
					}else{
						if(map.get(cl) == Entity.WALL){
							predictNextMap(ae);
							return;
						}else{
							if(!map.containsKey(cl)) map.put(cl, Entity.EMPTY);
						}
					}
				}
			}
			this.checkForMapContradiction(ae);
			
		}
		
		
		public boolean isOutOfBounds(Location l){
			return isOutOfBounds(l.x,l.y);
		}
		
		public boolean isOutOfBounds(int x, int y){
			if(!esa.integrated){
				if(esa.westTeam){
					if(x<0) return true;
				}else{
					if(x>0) return true;
				}
				if(esa.northSide){
					if(y>0) return true;
				}else{
					if(y<0) return true;
				}
				if(width != null && Math.max(Math.abs(x), Math.abs(y)) >= width){
					return false;
				}
				
				return false;
			}else{
				return x>=width||y>=width||x<0||y<0;
			}
		}
		
		public void checkForMapContradiction(AgentEnvironment ae){
			if(unknownTerritory) return;
			
			
			
			for(Location l : map.keySet()){
				if((Math.abs(l.x) >= 10 || Math.abs(l.y) >= 10) && map.get(l) == Entity.EMPTY){
					knownMaps.clear();
					unknownTerritory = true;
					predictNextMap(ae);
					return;
				}
			}
			
			Location east = new Location(esa.x + 1, esa.y);
			if(ae.isObstacleEastImmediate()){
				if(map.get(east) != Entity.WALL && map.get(east) != null){
					predictNextMap(ae);
					return;
				}
			}else{
				if(map.get(east) == Entity.WALL){
					predictNextMap(ae);
					return;
				}
			}
			Location south = new Location(esa.x, esa.y-1);
			if(ae.isObstacleSouthImmediate()){
				if(map.get(south) != Entity.WALL && map.get(south) != null){
					predictNextMap(ae);
					return;
				}
			}else{
				if(map.get(south) == Entity.WALL){
					predictNextMap(ae);
					return;
				}
			}
			Location west = new Location(esa.x-1, esa.y);
			if(ae.isObstacleWestImmediate()){
				if(map.get(west) != Entity.WALL && map.get(west) != null){
					predictNextMap(ae);
					return;
				}
			}else{
				if(map.get(west) == Entity.WALL){
					predictNextMap(ae);
					return;
				}
			}
			Location north = new Location(esa.x, esa.y+1);
			if(ae.isObstacleNorthImmediate()){
				if(map.get(north) != Entity.WALL && map.get(north) != null){
					predictNextMap(ae);
					return;
				}
			}else{
				if(map.get(north) == Entity.WALL){
					predictNextMap(ae);
					return;
				}
			}
		}
		
		public void updateMap(AgentEnvironment ae){
			this.checkForMapContradiction(ae);
			
			//east
			Location east = new Location(esa.x + 1, esa.y);
			if(!certain.contains(east)){
				if(ae.isObstacleEastImmediate()) map.put(east, Entity.WALL);
				else if(ae.isBaseEast(ae.OUR_TEAM, true)) map.put(east, Entity.BASE);
				else map.put(east, Entity.EMPTY);
				certain.add(east);
			}
			//south
			Location south = new Location(esa.x, esa.y-1);
			if(!certain.contains(south)){
				if(ae.isObstacleSouthImmediate()) map.put(south, Entity.WALL);
				else if(ae.isBaseSouth(ae.OUR_TEAM, true)) map.put(south, Entity.BASE);
				else map.put(south, Entity.EMPTY);
				certain.add(south);
			}
			//west
			Location west = new Location(esa.x-1, esa.y);
			if(!certain.contains(west)){
				if(ae.isObstacleWestImmediate()) map.put(west, Entity.WALL);
				else if(ae.isBaseWest(ae.OUR_TEAM, true)) map.put(west, Entity.BASE);
				else map.put(west, Entity.EMPTY);
				certain.add(west);
			}
			//north
			Location north = new Location(esa.x, esa.y+1);
			if(!certain.contains(north)){
				if(ae.isObstacleNorthImmediate()) map.put(north, Entity.WALL);
				else if(ae.isBaseNorth(ae.OUR_TEAM, true)) map.put(north, Entity.BASE);
				else map.put(north, Entity.EMPTY);
				certain.add(west);
			}
			
			
			//did we find an adjacent agent that would tell us the width?
			if(ae.isAgentEast(AgentEnvironment.OUR_TEAM, true) || ae.isAgentWest(AgentEnvironment.OUR_TEAM, true)){
				this.width = Math.abs(esa.otheragent.y) + Math.abs(esa.y) + 1;
			}else if(ae.isAgentNorth(AgentEnvironment.OUR_TEAM, true)){
				this.width = Math.abs(esa.otheragent.y) + Math.abs(esa.y) + 1 - (int) Math.signum(esa.otheragent.y);
			}else if(ae.isAgentSouth(AgentEnvironment.OUR_TEAM, true)){
				this.width = Math.abs(esa.otheragent.y) + Math.abs(esa.y) + 1 + (int) Math.signum(esa.otheragent.y);
			}
			
			//did we find the flag level?
			if(!ae.isBaseNorth(AgentEnvironment.ENEMY_TEAM, false) && !ae.isBaseSouth(AgentEnvironment.ENEMY_TEAM, false)){
				flagLevel = esa.y;
			}
			if(ae.isBaseNorth(AgentEnvironment.ENEMY_TEAM, true) || ae.isBaseNorth(AgentEnvironment.OUR_TEAM, true)){
				flagLevel = esa.y + 1;
			}
			if(ae.isBaseSouth(AgentEnvironment.ENEMY_TEAM, true) || ae.isBaseSouth(AgentEnvironment.OUR_TEAM, true)){
				flagLevel = esa.y - 1;
			}
			//did we find the width INTEGRATION pending
			if(!ae.isBaseEast(AgentEnvironment.ENEMY_TEAM, false) && ! ae.isBaseWest(AgentEnvironment.ENEMY_TEAM, false)){
				width = Math.abs(esa.x)+1;
				
				
			}
			if(width != null){
				if(esa.northSide){
					this.flagLevel = -width/2;
					if(width%2==1) flagLevel--;
				}else{
					this.flagLevel = width /2 - 1;
				}
			}
			
			if((flagLevel != null && flagLevel != 4 && flagLevel != -5) || (width != null && width != 10)){
				knownMaps.clear();
				predictNextMap(ae);
			}
			
			
			
		}
		
		
		
		public boolean okMoveNorth(AgentEnvironment ae){
			if(ae.isAgentNorth(AgentEnvironment.OUR_TEAM, true)){
				return false;
			}
			if(ae.isAgentNorth(AgentEnvironment.ENEMY_TEAM, true)){
				if(ae.hasFlag(AgentEnvironment.ENEMY_TEAM) && !ae.hasFlag()){
					return true;
				}else{
					return false;
				}
				
			}
			return true;
		}
		
		public boolean okMoveSouth(AgentEnvironment ae){
			if(ae.isAgentSouth(AgentEnvironment.OUR_TEAM, true)){
				return false;
			}
			if(ae.isAgentSouth(AgentEnvironment.ENEMY_TEAM, true)){
				if(ae.hasFlag(AgentEnvironment.ENEMY_TEAM) && !ae.hasFlag()){
					return true;
				}else{
					return false;
				}
				
			}
			return true;
		}
		
		public boolean okMoveEast(AgentEnvironment ae){
			if(ae.isAgentEast(AgentEnvironment.OUR_TEAM, true)){
				return false;
			}
			if(ae.isAgentEast(AgentEnvironment.ENEMY_TEAM, true)){
				if(ae.hasFlag(AgentEnvironment.ENEMY_TEAM) && !ae.hasFlag()){
					return true;
				}else{
					return false;
				}
				
			}
			return true;
		}
		
		public boolean okMoveWest(AgentEnvironment ae){
			if(ae.isAgentWest(AgentEnvironment.OUR_TEAM, true)){
				return false;
			}
			if(ae.isAgentWest(AgentEnvironment.ENEMY_TEAM, true)){
				if(ae.hasFlag(AgentEnvironment.ENEMY_TEAM) && !ae.hasFlag()){
					return true;
				}else{
					return false;
				}
				
			}
			return true;
		}
		
		public ArrayList<Location> aStarTo(final Location start, final Location goal, final AgentEnvironment ae){
			
			final HashMap<Location, Location> predecessor = new HashMap<>();
			final HashMap<Location, Integer> costs = new HashMap<>();
			
			Comparator<Location> aStarComparator = new Comparator<Location>(){

				@Override
				public int compare(Location arg0, Location arg1) {
					Long acost1 = (long) costs.get(arg0);
					Long acost2 = (long) costs.get(arg1);
					acost1+= manhattanDistance(goal, arg0);
					acost2+= manhattanDistance(goal, arg1);
					if(acost1==acost2){
						if(r.nextBoolean()){
							acost1--;
						}else{
							acost2--;
						}
					}
					
					return acost1.compareTo(acost2);
					
				}
			};
			
			PriorityQueue<Location> queue = new PriorityQueue<Location>(10, aStarComparator);
			TreeSet<Location> unknowns = new TreeSet<Location>(aStarComparator);
			
			
			queue.add(start);
			predecessor.put(start, null);
			costs.put(start, 0);
			
			boolean foundGoal = false;
			Location tempGoal = null;
			boolean firstMove = true;
			while(!queue.isEmpty()){
				Location next = queue.poll();
				
				if(next.equals(goal)){
					foundGoal = true;
					break;
				}
				
				ArrayList<Location> neighbors = new ArrayList<>();
				if(!firstMove || okMoveNorth(ae)) neighbors.add(new Location(next.x, next.y+1));
				if(!firstMove || okMoveEast(ae)) neighbors.add(new Location(next.x+1, next.y));
				if(!firstMove || okMoveSouth(ae)) neighbors.add(new Location(next.x, next.y-1));
				if(!firstMove || okMoveWest(ae)) neighbors.add(new Location(next.x-1, next.y));
				firstMove = false;
				
				for(Location loc : neighbors){
					//if(isOutOfBounds(loc)) System.out.println(loc.toString() + " is out of bounds");
					//System.out.println(loc.toString() + " " + map.get(loc) + " " + ae.hasFlag(AgentEnvironment.ENEMY_TEAM));
					
					
					if(!isOutOfBounds(loc) && map.get(loc) != Entity.WALL && !((map.get(loc) == Entity.BASE) && !ae.hasFlag(AgentEnvironment.ENEMY_TEAM) && !ae.hasFlag())){
						
						//System.out.println("Considering " + loc.toString());
						if(map.get(loc) != null && map.get(loc) != Entity.UNKNOWN){
							if(map.get(loc) != Entity.MINE && (costs.get(loc) == null || costs.get(loc) > costs.get(next) + 1)){
								costs.put(loc, costs.get(next) + 1);
								predecessor.put(loc, next);
								queue.add(loc);
								
							}else if(map.get(loc) == Entity.MINE){
								int returndist = this.aStarTo(new Location(0,0), next, ae).size() + costs.get(next) + 1;
								if(costs.get(loc) == null || costs.get(loc) > returndist){
									costs.put(loc, returndist);
									predecessor.put(loc, next);
									queue.add(loc);
								}else{
									continue;
								}
								
								
							}
							else continue;
						}else{
							if(costs.get(loc) == null || costs.get(loc) > costs.get(next) + 3){
								costs.put(loc, costs.get(next) + 2);
								predecessor.put(loc, next);
								if(width == null || Math.max(Math.abs(loc.x), Math.abs(loc.y)) > maxRadius + 10){
									unknowns.add(loc);
								}
								else queue.add(loc);
								
								
							}
							else continue;
						}
						
						
						
						
					}
				}
			}
			
			ArrayList<Location> result = new ArrayList<>();
			
			Location next = goal;
			if(!foundGoal){
				if(unknowns.size() > 0) next = unknowns.first();
				else{
					//you must be stuck
					return result;
				}
			}
			while(!next.equals(start)){
				result.add(next);
				next = predecessor.get(next);
				
			}
			
			
			
			return result;
		}
		
		public Location convertLocation(Location source, boolean sourceNorth, boolean sourceWest, boolean destNorth, boolean destWest, int width){
			int x = source.x;
			int y = source.y;
			if(sourceNorth){
				y += width - 1;
			}
			if(!sourceWest){
				x += width - 1;
			}
			
			if(destNorth){
				y -= (width - 1);
			}
			if(!destWest){
				x -= width - 1;
			}
			
			return new Location(x,y);
		}
		
		public void foundWidth(int width){
			/*if(this.width != null) return;
			this.width = width;
			this.flagLevel = width / 2;
			
			HashMap<Location, Entity> newMap = new HashMap<>();
			for(Location loc : newMap.keySet()){
				newMap.put(convertLocation(loc, this.esa), map.get(loc));
				
				
			}
			Location here = new Location(this.esa.x, this.esa.y);
			this.esa.x = convertLocation(here, esa).x;
			this.esa.y = convertLocation(here, esa).y;*/
			
		}
		
		public void didIFindWidth(AgentEnvironment ae){
			if(width != null) return;
			if(!ae.isBaseEast(AgentEnvironment.ENEMY_TEAM, false) && !ae.isBaseWest(AgentEnvironment.ENEMY_TEAM, false)){
				foundWidth(Math.abs(esa.x) + 1);
				return;
			}
			if(ae.isBaseEast(AgentEnvironment.ENEMY_TEAM, true) || ae.isBaseWest(AgentEnvironment.ENEMY_TEAM, true)){
				foundWidth(Math.abs(esa.x) + 2);
				return;
			}
		}
		
		public static Long manhattanDistance(Location l1, Location l2){
			long cost = 0;
			cost += Math.abs(((long) l1.x) - l2.x);
			cost += Math.abs(((long) l1.y) - l2.y);
			//System.out.println("MD from " + l1.toString() + " to " + l2.toString() + " is " + cost);
			return cost;
			
		}
	}
}