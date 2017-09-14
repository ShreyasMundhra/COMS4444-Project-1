package escape.sim;

import java.util.List;

public interface Player {

	// n: number of players
	// ID of player and handle ranges from 1 to n
	// You have ID 1, all other IDs are randomized
	// Return the ID of handle you want to hold in the first round
	public int init(int n);

	// conflicts: IDs of player who tried to hold the same handle as you in previous round
	// conflicts.size() == 0 implies your success last round
	// Return the ID of handle you want to hold this round
	public int attempt(List<Integer> conflicts);
}
