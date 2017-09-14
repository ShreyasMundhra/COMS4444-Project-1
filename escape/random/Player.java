package escape.random;

import java.util.List;
import java.util.Random;

public class Player implements escape.sim.Player {
	private Random rand;
	private int n = 0;
	private int lastMove = 0;

	public Player() {
		rand = new Random();
	}

	@Override
	public int init(int n) {
		this.n = n;
		lastMove = rand.nextInt(n);
		return lastMove + 1;
	}

	@Override
	public int attempt(List<Integer> conflicts) {
		int move = rand.nextInt(n);
		while (move == lastMove)
			move = rand.nextInt(n);
		lastMove = move;
		return lastMove + 1;
	}

}
