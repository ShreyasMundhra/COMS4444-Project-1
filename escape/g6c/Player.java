package escape.g6c;

import java.util.List;
import java.util.Random;

public class Player implements escape.sim.Player {
	private Random rand;
	private int turn;
        private int lastMove;
        private int n;

        private int oddOwnership;
        private int evenOwnership;

	public Player() {
		rand = new Random();
	}

	@Override
	public int init(int n) {
		this.turn = 0;
                this.n = n;
		this.oddOwnership = 0;
                this.evenOwnership = 0;
		
                lastMove = rand.nextInt(n) + 1;
		return lastMove;
	}

	// Strategy:
	// 1. Each player may have ownership of a single handle for odd-turn rounds.
        // 2. Each player may have ownership of a single handle for even-turn rounds.
	// 3. If a player is the only one on a handle, they obtain ownership of that handle.
        // 4. If a player has ownership for a particular round, they must always return to that handle.
        // 5. If a player does not have ownership, they will randomly choose a handle.
        // 6. Cannot be owner of the same number handle for both even and odd rounds.
        // 7. A player gives up ownership if they choose their owned handle for an opposite round.
	@Override
	public int attempt(List<Integer> conflicts) {
		++ turn;
                if ((turn % 2) == 0) {
                        if (conflicts.size() == 0 && lastMove != oddOwnership) { // Obtain ownership
                                evenOwnership = lastMove;
                        }
                        if (oddOwnership > 0) {
                                lastMove = oddOwnership;
                                return lastMove;
                        }
                } else {
                        if (conflicts.size() == 0 && lastMove != evenOwnership){ // Obtain ownership
                                oddOwnership = lastMove;
                        }
                        if (evenOwnership > 0) {
                                lastMove = evenOwnership;
                                return lastMove;
                        }
                }

                // No ownership for round
                int move = rand.nextInt(n) + 1;
		while (move == lastMove && conflicts.size() > 0)
			move = rand.nextInt(n) + 1;
                
                // Checking if should give up ownership
                if (move == oddOwnership) {
                    oddOwnership = 0;
                } else if (move == evenOwnership) {
                    evenOwnership = 0;
                }

                lastMove = move;
                return lastMove;
	}
}
