import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;

public class MyAI extends Agent
{
	
	private class pair implements Comparable{
		int dis;
		int[] point = new int[2];
		public pair (int dist, int x, int y) {
			dis = dist;
			point[0] = x;
			point[1] = y;
		}
		public int compareTo(Object o)
		{
		    	if (this.dis - ((pair)o).dis > 0) return 1;
		    	else if (this.dis - ((pair)o).dis == 0) return 0;
		    	else return -1;
		}
	}
	
	private class Node implements Comparable{
		int xp;
		int yp;
		int g;
		int h;
		Node parent = null;
		public Node (int x, int y, Node p) {
			xp = x;
			yp = y;
			parent = p;
		}
		public Node() {
			
		}
		public int compareTo(Object o)
		{
		    	if ((this.g+this.h) == (((Node)o).h + ((Node)o).g)) return 0;
		    	else if ((this.g+this.h) == (((Node)o).h + ((Node)o).g)) return -1;
		    	else return 1;  	
		}
	}
	
	private class map {
		Node node;
		// int wumpus = 0; // 0 unknown 1 known 2 possible
		// int pit = 0;
		// int safe = 0;
		// int both = 0;
		// int visited = 0;
		String state = "";
		public map (int x, int y) {
			node = new Node(x, y, null);
		}
	}
	
	Queue<Action> cur_path;
	PriorityQueue<pair> dist_pq;
	int x = 0;
	int y = 0;
	int row = 20;
	int col = 20;
	int direction = 1;
	map[][] map_array = new map[20][20];
	int wumpus[][] = new int[20][20];
	int safe[][] = new int[20][20];
	int pit[][] = new int[20][20];
	int visited[][] = new int[20][20];
	int dis[][] = new int[20][20];
	boolean hasArrow = true;
	boolean hasGold = false;
	List<int[]> path;
	int safenum = 0;
	boolean back = false; 
	Stack<pair> safeSquare;
	Stack<int[]> curpath;
	int wumpusState = 0; //0 for unknown 1 for possible 2 for known
	boolean killed = false;
	int[] wumpusLoc;
	int num = 0;
        int count = 0;
	public MyAI ( )
	{
		visited[0][0] = 1;
		path = new ArrayList<int[]>();
		cur_path = new LinkedList<Agent.Action>();
		safeSquare = new Stack<pair>();
		curpath = new Stack<int[]>();
		safeSquare.add(new pair(0,0,0));
		//dist_pq = new PriorityQueue<pair>(new Comparator<pair>() {
		//	public int compare(pair a, pair b) {
		//		return (int) (a.dis - b.dis);
		//	}
		//});
		dist_pq = new PriorityQueue<pair>();
		for (int i = 0; i < 20; i++) {
			for (int j = 0; j < 20; j++) {
				map_array[i][j] = new map(i,j);
			}
		}
		map_array[0][0].state = "safe";
		wumpusLoc = new int[2];
		count = 0;
	}
	
	public Action getAction
	(
		boolean stench,
		boolean breeze,
		boolean glitter,
		boolean bump,
		boolean scream
	)
	{
		
		Action action = Action.CLIMB;
		if (bump) {
			updatePositionWhenBump();
			setRowOrCol();
		}
//		updateState(breeze, stench);
		
		if (scream)
		{
			if (breeze)
				map_array[wumpusLoc[0]][wumpusLoc[1]].state = "possible_pit";
			else
				map_array[wumpusLoc[0]][wumpusLoc[1]].state = "safe";
			killed = true;
			wumpusState = 2;
		}
		
		if (x == 0 && y == 0)
		{
			if (breeze)
				return Action.CLIMB;
			else if (stench && !killed)
			{
				wumpusState = 1;
				wumpusLoc[0] = x;
				wumpusLoc[1] = y;
				if (hasArrow)
				{
					hasArrow = false;
					return Action.SHOOT;
				}
				//If already shot and didn't kill wumpus then leave
				return Action.CLIMB;
			}
		}
		// if (++count > 200) {
		// 	back = true;
  //                       goTo(0,0);
		// }

		if (back)
		{
			if (x == 0 && y == 0)
			{
				return Action.CLIMB;
			}
			action = cur_path.peek();
			cur_path.poll();
		}
		
		if (glitter && !back)
		{
			action = Action.GRAB;
			hasGold = true;
			back = true;
			goTo(0, 0);
		}
		
		if (!back && !hasGold) {
			updateState(breeze, stench);
			if (wumpusState == 2 && !killed && hasArrow) {
				Kill_Wumpus();
			}
			if (cur_path.isEmpty()) {
				closest_node();
//				System.out.print("peek dist_pq x:" + dist_pq.peek().point[0]+"y:" +dist_pq.peek().point[1]);
				dist_pq.poll();
				
//				System.out.print("dist_pq size = " + dist_pq.size());
				while (!dist_pq.isEmpty()) {
					if (map_array[dist_pq.peek().point[0]][dist_pq.peek().point[1]].state == "safe") {
						int i = dist_pq.peek().point[0];
						int j = dist_pq.peek().point[1];
						int val = dist_pq.peek().dis;
						if (i == 0 && j == 0) num++;
						if (num == 2) back = true;
						// System.out.println("i = "+i);
						// System.out.println("j = "+j);
//						System.out.print("val = "+val);
						dist_pq.poll();
						goTo(i, j);
//						System.out.print(1);
						dist_pq = new PriorityQueue<pair>();
						break;
					}
					else {
//						System.out.print("polled dist_pq" + dist_pq.peek().point[0]+dist_pq.peek().point[1]);
						dist_pq.poll();
					}
				}
//				System.out.print(1);
				if (cur_path.isEmpty()) {
//					System.out.print(2);
					if (wumpusState != 0 && !killed && hasArrow)
						Kill_Wumpus();
					else
					{
						back = true;
						goTo(0, 0);
					}
//					back = true;
//					goTo(0,0);
				}
			}
//			System.out.print(1);
//			System.out.print(action.toString());
			action = cur_path.peek();
			cur_path.poll();
		}
		if (action == Action.TURN_LEFT) {
//			System.out.print("1");
			Turn(false);
		}
		else if (action == Action.TURN_RIGHT) {
//			System.out.print("2");
			Turn(true);
		}
		else if (action == Action.FORWARD && !bump) {
//			System.out.print("3");
			UpdatePosition();
		}
//		System.out.print(action.toString());
		if (action == Action.SHOOT) {
			hasArrow = false;
		}
		return action;
	}
	
//	public void Kill_Wumpus() 
//	{
//		map_array[wumpusLoc[0]][wumpusLoc[1]].safe = 1;
//		goTo(wumpusLoc[0], wumpusLoc[1]);
//		cur_path.poll();
//		cur_path.offer(Action.SHOOT);
//	}
	
	public void closest_node() {
		dist_pq = new PriorityQueue<pair>();
		for (int i = 0; i < row; i++) //i means x, j means y
		{
			for (int j = 0; j < col; j++)
			{
				int turn = 0;
				switch(direction) {
					case 1:
						if (j < x) turn = 2;
						if (j >= x && i != y) turn = 1;
					    break;
					case 2:
						if (i > y) turn = 2;
						if (i <= y && j != x) turn = 1;
					case 3:
						if (j > x) turn = 2;
						if (j <= x && i != y) turn = 1;
						break;
					case 0:
						if (i < y) turn = 2;
						if (i >= y && j != x) turn = 1;
						break;	
				}
//				System.out.print("x:"+j);
//				System.out.print("y:"+i);
//				System.out.print("value:"+Math.abs(x-j)+Math.abs(y-i)+turn);
			    dist_pq.offer(new pair(Math.abs(x-j)+Math.abs(y-i)+turn, j, i));
			    // dist_pq.offer(new pair(Math.abs(x-j)+Math.abs(y-i), j, i));
			}
		}
	}
	
	public void Kill_Wumpus()
	{
		// System.out.println("kill");
		map_array[wumpusLoc[0]][wumpusLoc[1]].state ="safe";
		goTo(wumpusLoc[0], wumpusLoc[1]);
		Queue<Action> cur = new LinkedList<Action>();
		while (cur_path.size() > 1) {
			// System.out.println(cur_path.peek());
			cur.offer(cur_path.peek());
			cur_path.poll();
		}
		cur.offer(Action.SHOOT);
		cur_path = new LinkedList<Action>(cur);
	}
	
	public void goTo(int dx, int dy) {
		cur_path = new LinkedList<Action>();
		Cal_Dis_From(dx, dy);
		PriorityQueue<Node> open_list = new PriorityQueue<Node>();
	    List<Node> closed_list = new ArrayList<Node>();
	    open_list.offer(map_array[x][y].node);
//	    System.out.print("open_peek:"+open_list.peek().xp);
	    Node end_node = CreatePath(open_list, closed_list);
//	    System.out.print(end_node.parent.xp);
	    Node old_node = null;
	    Stack<Integer> dirs = new Stack<Integer>();
	    int dir_size = 0;
	    while (end_node.parent != null)
		{
			if (end_node.xp < end_node.parent.xp)
				dirs.push(3);
			else if (end_node.xp > end_node.parent.xp)
				dirs.push(1);
			else if (end_node.yp > end_node.parent.yp)
				dirs.push(0);
			else if (end_node.yp < end_node.parent.yp)
				dirs.push(2);
			dir_size++;
			old_node = end_node;
			end_node = end_node.parent;
			old_node.parent = null;
		}
	    int temp_dir = direction;
		for (int i = 0; i < dir_size; ++i)
		{
			//If the player is already facing that direction then just go forward
			if(dirs.peek() != temp_dir)
			{
				//Else they add the number of turns needed to face that direction
				switch (temp_dir)
				{
				case 0:
					switch (dirs.peek())
					{
					case 1:
						cur_path.offer(Action.TURN_RIGHT); 
						temp_dir = temp_dir + 1 == 4 ? 0 : temp_dir + 1;
						break;
					case 2:
						cur_path.offer(Action.TURN_RIGHT);
						cur_path.offer(Action.TURN_RIGHT); 
						temp_dir = temp_dir + 1 == 4 ? 0 : temp_dir + 1;
						temp_dir = temp_dir + 1 == 4 ? 0 : temp_dir + 1;
						break;
					case 3:
						cur_path.offer(Action.TURN_LEFT); 
						temp_dir = temp_dir - 1 == -1 ? 3 : temp_dir - 1;
						break;
					}break;
				case 1:
					switch (dirs.peek())
					{
					case 2:
						cur_path.offer(Action.TURN_RIGHT); 
						temp_dir = temp_dir + 1 == 4 ? 0 : temp_dir + 1;
						break;
					case 3:
						cur_path.offer(Action.TURN_RIGHT);
						cur_path.offer(Action.TURN_RIGHT);
						temp_dir = temp_dir + 1 == 4 ? 0 : temp_dir + 1;
						temp_dir = temp_dir + 1 == 4 ? 0 : temp_dir + 1;
						break;
					case 0:
						cur_path.offer(Action.TURN_LEFT); 
						temp_dir = temp_dir - 1 == -1 ? 3 : temp_dir - 1;
						break;
					}break;
				case 2:
					switch (dirs.peek())
					{
					case 3:
						cur_path.offer(Action.TURN_RIGHT); 
						temp_dir = temp_dir + 1 == 4 ? 0 : temp_dir + 1;
						break;
					case 0:
						cur_path.offer(Action.TURN_RIGHT);
						cur_path.offer(Action.TURN_RIGHT); 
						temp_dir = temp_dir + 1 == 4 ? 0 : temp_dir + 1;
						temp_dir = temp_dir + 1 == 4 ? 0 : temp_dir + 1;
						break;
					case 1:
						cur_path.offer(Action.TURN_LEFT); 
						temp_dir = temp_dir - 1 == -1 ? 3 : temp_dir - 1;
						break;
					}break;
				case 3:
					switch (dirs.peek())
					{
					case 0:
						cur_path.offer(Action.TURN_RIGHT); 
						temp_dir = temp_dir + 1 == 4 ? 0 : temp_dir + 1;
						break;
					case 1:
						cur_path.offer(Action.TURN_RIGHT);
						cur_path.offer(Action.TURN_RIGHT); 
						temp_dir = temp_dir + 1 == 4 ? 0 : temp_dir + 1;
						temp_dir = temp_dir + 1 == 4 ? 0 : temp_dir + 1;
						break;
					case 2:
						cur_path.offer(Action.TURN_LEFT); 
						temp_dir = temp_dir - 1 == -1 ? 3 : temp_dir - 1;
						break;
					}break;
				}
			}
			cur_path.offer(Action.FORWARD);
			dirs.pop();
		}
	}
	
	public Node CreatePath(PriorityQueue<Node> open, List<Node> closed) {
		Node current = new Node();
		int vector_count = 0;
		do {
			current = open.peek();
//			System.out.print("current:"+current.xp);
			open.poll();
			closed.add(current);
			for (int cx = current.xp - 1; cx < current.xp + 2; ++cx)
			{
				for (int cy = current.yp - 1; cy < current.yp + 2; ++cy)
				{
					if (cx >= 0 && cy >= 0 && cy < row && cx < col && ((cx == current.xp) != (cy == current.yp)))
					{
						if (!((map_array[cx][cy].state == "safe") || (map_array[cx][cy].state == "visited")))
						{
							continue;
						}
						int i = 0;
						for (; i < closed.size(); i++) {
							if (equal(closed.get(i), map_array[cx][cy].node))
							    break;
						}
						if (i < closed.size()) continue;
						boolean in_open = PQContains(open, map_array[cx][cy].node);
						if (!in_open || current.g + 10 < map_array[cx][cy].node.g)
						{
//							System.out.print("cx:"+cx);
//							System.out.print("cy:"+cy);
							map_array[cx][cy].node.g = current.g + 10;
							map_array[cx][cy].node.parent = current;
							if (!in_open)
								open.offer(map_array[cx][cy].node);
						}
					}
				}
			}
		} while (!open.isEmpty() && current.h != 0);
		return current;
	}
	
	public boolean equal(Node a, Node b) {
		if (a.xp == b.xp && a.yp == b.yp) 
			return true;
		else 
			return false;
	}
	public void Turn(boolean right) 
	{
		if (right)
		{
			if (++direction > 3)
				direction = 0;
		}
		else
		{
			if (--direction < 0)
				direction = 3;
		}
	}
	
	public void UpdatePosition()
	{
		if (direction == 0)
			y++;
		else if (direction == 1)
			x++;
		else if (direction == 2)
			y--;
		else if (direction == 3)
			x--;	
	}
	
	public void updatePositionWhenBump()
	{
		if (direction == 0)
			y--;
		else if (direction == 1)
			x--;
		else if (direction == 2)
			y++;
		else if (direction == 3)
			x++;	
	}
//	public void TrimMap()
//	{
//		if (direction == 1 && !trimmed_right)
//		{
//			for (int y = 0; y < map_array.size(); ++y)
//				map_array[y].resize(x);
//			//decriment x since hit 
//			--x;
//			trimmed_right = true;
//		}
//		else if (cur_direction == 0 && !trimmed_up)
//		{
//			map_array.resize(y);
//			trimmed_up = true;
//			--y;
//		}
//		else if (cur_direction == 0)
//			--y;
//		else if (cur_direction == 1)
//			--x;
//		else if (cur_direction == 2)
//			++y;
//		else if (cur_direction == 3)
//			++x;
//	}
	
	public boolean PQContains(PriorityQueue<Node> open, Node to_check)
	{
		List<Node> list_to_check = new ArrayList<>();
		int size = open.size();
		boolean to_return = false;

		for (int i = 0; i < size; ++i)
		{
			list_to_check.add(open.peek());
			open.poll();
		}
		for (int i = 0; i < list_to_check.size(); ++i)
		{
			open.offer(list_to_check.get(i));
			if (equal(list_to_check.get(i), to_check))
				to_return = true;
		}
		return to_return;
	}
	
	public void Cal_Dis_From(int dx, int dy) {
		for (int i = 0; i < row; i++)
		{
			for (int j = 0; j < col; j++)
			{
			     map_array[j][i].node.h = Math.abs(dx-j)+Math.abs(dy-i);
			}
		}
	}
//    public Action turnAround() {
//        return Action.TURN_LEFT;
//    }

    public void setSafe() {
        if (x < col-1) 
            safe[x+1][y] = 1;
        if (y < row-1)
            safe[x][y+1] = 1;
        if (x > 0)
            safe[x-1][y] = 1;
        if (y > 0)
            safe[x][y-1] = 1;
    }
    
    public void updateState(boolean breeze, boolean stench) {
	    	if (map_array[x][y] == null)
	    		return;
	    	if (map_array[x][y].state == "visited")
	    		return;
	    	String to_mark = "?";
	    	if (stench && breeze)
	    	{
	    		if (!killed && wumpusState != 2)
	    			to_mark = "possible_both";
	    		else
	    			to_mark = "possible_pit";
	    	}
	    	else if (stench && !killed)
	    		to_mark = "possible_wumpus";
	    	else if (breeze)
	    		to_mark = "possible_pit";
	    	else
	    		to_mark = "safe";
	
	    	for (int cy = y - 1; cy <= y + 1; ++cy)
	    	{
	    		for (int cx = x - 1; cx <= x + 1; ++cx)
	    		{
	    			if (cy > -1 && cx > -1 && cy < row && cx < col)
	    			{
	    		
	    				//Checks that it only gets the cardinal directions
	    				if ((cx == x) != (cy == y))
	    				{
	    					if ((map_array[cx][cy].state == "wumpus" || map_array[cx][cy].state == "pit" || map_array[cx][cy].state == "visited") && to_mark != "safe")
	    						continue;
	    					if ((map_array[cx][cy].state == "possible_wumpus" || map_array[cx][cy].state == "possible_both") && stench)
	    					{
	    						// System.out.println("step");
	    						if (wumpusState != 2) {
	    							// System.out.println("step1"+" "+cx+" "+cy);
	    							map_array[cx][cy].state = "wumpus";
	    							wumpusLoc[0] = cx;
	    							wumpusLoc[1] = cy;
	    							ChangeState("possible_wumpus", "safe");
	    							ChangeState("possible_both", "possible_pit");
	    							wumpusState = 2;
	    						}
	    						else if (breeze)
	    							map_array[cx][cy].state = "possible_pit";
	    						else
	    							map_array[cx][cy].state = "safe";
	    					}
	    					else if ((map_array[cx][cy].state == "possible_pit" || map_array[cx][cy].state == "possible_both") && breeze)
	    						map_array[cx][cy].state = "pit";
	    					else if ((map_array[cx][cy].state == "possible_pit" && stench) || (map_array[cx][cy].state == "possible_wumpus" && breeze))
	    						map_array[cx][cy].state = "safe";
	    					else if (map_array[cx][cy].state != "safe" && map_array[cx][cy].state != "visited")
	    					{
	    						if (to_mark == "possible_wumpus")
	    						{
	    							if (wumpusState == 2)
	    							{
	    								map_array[cx][cy].state = "safe";
	    								continue;
	    							}
	    							else
	    							{
	    								// System.out.println("wumpus"+" "+cx+" "+cy);
	    								wumpusState = 1;
	    								wumpusLoc[0] = cx;
	    								wumpusLoc[1] = cy;
	    							}
	    						}
	    						map_array[cx][cy].state = to_mark;
	    					}
	    				}
	    			}
	    		}
	    	}
	    	//This means it was walked on
	    	map_array[x][y].state = "visited";
    }
    
   public void ChangeState(String a, String b) {
		for (int j = 0; j < row; ++j)
		{
			for (int i = 0; i < col; ++i)
			{
				
				if (map_array[i][j].state.equals(a))
					map_array[i][j].state = b;	
			}
		}
   		
   }
//    void updateState(boolean breeze, boolean stench) {
//        if (breeze) {
//            if (!stench) {
//                if (x < col-1 && safe[x+1][y] == 0) {
//                    if (wumpus[x+1][y] == 0)
//                        pit[x + 1][y] = 1;
//                    else {
//                        safe[x+1][y] = 1;
//                        wumpus[x+1][y] = 0;
//                    }
//                }
//                if (y < row-1 && safe[x][y+1] == 0) {
//                    if (wumpus[x][y+1] == 0)
//                        pit[x][y + 1] = 1;
//                    else {
//                        safe[x][y+1] = 1;
//                        wumpus[x][y+1] = 0;
//                    }
//                }
//                if (x > 0 && safe[x-1][y] == 0) {
//                    if (wumpus[x-1][y] == 0)
//                        pit[x - 1][y] = 1;
//                    else {
//                        safe[x-1][y] = 1;
//                        wumpus[x-1][y] = 0;
//                    }
//                }
//                if (y>0 && safe[x][y-1] == 0) {
//                    if (wumpus[x][y-1] == 0)
//                        pit[x][y-1] = 1;
//                    else {
//                        safe[x][y-1] = 1;
//                        wumpus[x][y-1] = 0;
//                    }
//                }
//            }
//            else {
//                if (x < col-1 && safe[x+1][y] == 0) {
//                    if (wumpus[x+1][y] == 0 && pit[x+1][y] == 0) {
//                        pit[x + 1][y] = 1;
//                        wumpus[x+1][y] = 1;
//                    }
//                }
//
//                if (y < row-1 && safe[x][y+1] == 0) {
//                    if (wumpus[x][y+1] == 0 && pit[x][y+1] == 0) {
//                        pit[x][y + 1] = 1;
//                        wumpus[x][y+1] = 1;
//                    }
//                }
//
//                if (x > 0 && safe[x-1][y] == 0) {
//                    if (wumpus[x-1][y] == 0 && pit[x-1][y] == 0) {
//                        pit[x - 1][y] = 1;
//                        wumpus[x-1][y] = 1;
//                    }
//                }
//                if (y > 0 && safe[x][y-1] == 0) {
//                    if (wumpus[x][y-1] == 0 && pit[x][y-1] == 0) {
//                        pit[x][y-1] = 1;
//                        wumpus[x-1][y] = 1;
//                    }
//                }
//            }
//        }
//        else {
//            if (stench) {
//                if (x < col-1 && safe[x+1][y] == 0) {
//                    if (pit[x+1][y] == 0)
//                        wumpus[x + 1][y] = 1;
//                    else {
//                        safe[x+1][y] = 1;
//                        pit[x+1][y] = 0;
//                    }
//                }
//                if (y < row-1 && safe[x][y+1] == 0) {
//                    if (pit[x][y+1] == 0)
//                        wumpus[x][y + 1] = 1;
//                    else {
//                        safe[x][y+1] = 1;
//                        pit[x][y+1] = 0;
//                    }
//                }
//                if (x > 0 && safe[x-1][y] == 0) {
//                    if (pit[x-1][y] == 0)
//                        wumpus[x - 1][y] = 1;
//                    else {
//                        safe[x-1][y] = 1;
//                        pit[x-1][y] = 0;
//                    }
//                }
//                if (y > 0 && safe[x][y-1] == 0) {
//                    if (pit[x][y-1] == 0)
//                        wumpus[x][y - 1] = 1;
//                    else {
//                        safe[x][y-1] = 1;
//                        pit[x][y-1] = 0;
//                    }
//                }
//            }
//            else {
//                setSafe();
//            	}
//        }
//    }


    public void setRowOrCol() {
    		if (direction == 0)
    			row = y+1;
    		if (direction == 1) 
    			col = x+1;
    }

//    public Action move() {
//        if (direction == 0) {
//            if (y < row - 1) {
//                visited[x][y + 1] = 1;
//                return Action.FORWARD;
//            }
//            else {
//                if (x == 0) {
//                    direction = 1;
//                    return Action.TURN_RIGHT;
//                }
//                if (x == col-1) {
//                    direction = 3;
//                    return Action.TURN_LEFT;
//                }
//                else {
//                    if (visited[x+1][y] == 0) {
//                        direction = 1;
//                        return Agent.Action.TURN_RIGHT;
//                    }
//                    else {
//	                    direction = 3;
//	                    return Agent.Action.TURN_LEFT;
//                    }
//                }
//            }
//        }
//
//        else if (direction == 1) {
//            if (x < col -1) {
//                visited[x+1][y] = 1;
//                return Agent.Action.FORWARD;
//            }
//            else {
//                if (y == 0) {
//                    direction = 0;
//                    return Agent.Action.TURN_LEFT;
//                }
//                if (y == row-1) {
//                    direction = 2;
//                    return Agent.Action.TURN_RIGHT;
//                }
//                else {
//                    if (visited[x][y+1] == 0) {
//                        direction = 0;
//                        return Agent.Action.TURN_LEFT;
//                    }
//                    else {
//                        direction = 2;
//                        return Agent.Action.TURN_RIGHT;
//                    }
//                }
//            }
//        }
//
//        else if (direction == 2) {
//            if (y > 0) {
//                visited[x][y-1] = 1;
//                return Agent.Action.FORWARD;
//            }
//            else {
//                if (x == 0) {
//                    direction = 1;
//                    return Agent.Action.TURN_LEFT;
//                }
//                if (x == col-1) {
//                    direction = 3;
//                    return Agent.Action.TURN_RIGHT;
//                }
//                else {
//                    if (visited[x+1][y] == 0) {
//                        direction = 1;
//                        return Agent.Action.TURN_LEFT;
//                    }
//                    else {
//                        direction = 3;
//                        return Agent.Action.TURN_RIGHT;
//                    }
//                }
//            }
//        }
//
//        else {
//            if (x > 0) {
//                visited[x-1][y] = 1;
//                return Agent.Action.FORWARD;
//            }
//            else {
//                if (y == 0) {
//                    direction = 0;
//                    return Agent.Action.TURN_RIGHT;
//                }
//                if (y == row-1) {
//                    direction = 2;
//                    return Agent.Action.TURN_LEFT;
//                }
//                else {
//                    if (visited[x][y+1] == 0) {
//                        direction = 0;
//                        return Agent.Action.TURN_RIGHT;
//                    }
//                    else {
//                        direction = 2;
//                        return Agent.Action.TURN_LEFT;
//                    }
//                }
//            }
//        }
//    }

//    public int findPath(int curx, int cury, int dx, int dy, List<int[]> curpath) {
//        if (safe[curx][cury] == 0) return 1000;
//        int point[] = {curx, cury};
//        curpath.add(point);
//        if (curx == dx && cury == dy)
//            return 1;
//        List<int[]> leftpath = new ArrayList(curpath);
//        int left = 1+findPath(curx-1, cury, dx, dy, path);
//        List<int[]> uppath = new ArrayList(curpath);
//        int up = 1+ findPath(curx, cury+1, dx, dy, path);
//        List<int[]> rightpath = new ArrayList(curpath);
//        int right = 1 + findPath(curx+1, cury, dx, dy, path);
//        List<int[]> downpath = new ArrayList(curpath);
//        int down = 1 + findPath(curx, cury-1, dx, dy, path);
//        int min = Math.min(left, Math.min(up, Math.min(right, down)));
//        if (min == left) path = new ArrayList(leftpath);
//        if (min == right) path = new ArrayList(rightpath);
//        if (min == up) path = new ArrayList(uppath);
//        if (min == down) path = new ArrayList(downpath);
//        return min;
//    }

}
