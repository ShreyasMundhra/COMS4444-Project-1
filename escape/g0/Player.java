package escape.g0;

import java.util.List;
import java.util.Random;

public class Player implements escape.sim.Player {
	private Random rand;
	private int n = 0;
	private int lastMove;
	private int turn;

	public Player() {
		rand = new Random();
	}

	@Override
	public int init(int n) {
		this.turn = 0;
		this.n = n;
		lastMove = rand.nextInt(n) + 1;
		return lastMove;
	}

	// Strategy: (Just for demostration, may not work.)
	// 1. If no one grabs your handle, stay.
	// 2. Grab the i-th handle where i is the id of some player who grab the same handle as you do.
	@Override
	public int attempt(List<Integer> conflicts) {
		++ turn;
		if (conflicts.size() > 0) {
			int tmp = conflicts.get(rand.nextInt(conflicts.size()));
			while (tmp == lastMove)
				tmp = rand.nextInt(n) + 1;
			lastMove = tmp;
		}
		return lastMove;
	}

}
