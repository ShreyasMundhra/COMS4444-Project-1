package escape.g2c;

import java.util.List;
import java.util.Random;
import java.lang.Math;

public class Player implements escape.sim.Player {
	private Random rand;
	private int turn;
        private int lastMove;
        private int n;

        private RoundInformation odd;
        private RoundInformation even;

	public Player() {
		rand = new Random();
	}

	@Override
	public int init(int n) {
		this.turn = 0;
                this.n = n;

                odd = new RoundInformation(n);
                even = new RoundInformation(n);

                for (int i=0; i < n+1; i++) {
                    odd.weights[i] = even.weights[i] = 1; // All weights start at 1.0
                }

                lastMove = rand.nextInt(n) + 1;
                return lastMove;
	}

	// Main Strategy:
	// 1. Each player may have ownership of a single handle for odd-turn rounds.
        // 2. Each player may have ownership of a single handle for even-turn rounds.
	// 3. If a player is the only one on a handle, they obtain ownership of that handle.
        // 4. If a player has ownership for a particular round, they must always return to that handle.
        // 5. If a player does not have ownership, they will choose a handle based on a distributed probability.
        // 6. Cannot be owner of the same number handle for both even and odd rounds.
        // 7. If the player collides with a player at the same spot multiple times, the player becomes less likely to return to that spot.
        // 8. If a player collides with another player during an even round, it knows that player does not own that spot for odd rounds.
	@Override
	public int attempt(List<Integer> conflicts) {
                turn ++;
                int move = 0;
                      
                // Marking conflicts from last turn
                boolean[] lastMoveConflicts = new boolean[n+1];
                for (int i=0; i < conflicts.size(); i++) {
                        lastMoveConflicts[conflicts.get(i)] = true;
                }

                if ((turn % 2) == 0) {
                        if (conflicts.size() == 0) { // Obtain ownership
                                odd.ownership = lastMove;
                                even.weights[odd.ownership] = 0;
                        }

                        if (lastMove != odd.ownership) {
                                weightCalculator(odd,even,lastMoveConflicts);
                        }
                        
                        if (even.ownership > 0) { // Checking for round ownership
                                lastMove = even.ownership;
                                return lastMove;
                        }
                        
                        move = weightedMoveGenerator(even.weights);
                        int cycle = 0;
                        while (move == odd.ownership || (move == lastMove && conflicts.size() > 0)) {
                                move = weightedMoveGenerator(even.weights);
                                cycle++;
                                if (cycle > n) {
                                        odd.weights[move] = .6/Math.pow(2,turn/2);
                                }
                        }
                } else {
                        if (conflicts.size() == 0){ // Obtain ownership
                                even.ownership = lastMove;
                                odd.weights[even.ownership] = 0;
                        }
                       
                        if (lastMove != even.ownership) {
                                weightCalculator(even,odd,lastMoveConflicts);
                        }

                        if (odd.ownership > 0) { // Checking for next round ownership
                                lastMove = odd.ownership;
                                return lastMove;
                        }

                        move = weightedMoveGenerator(odd.weights);

                        int cycle = 0; // Prevents infinite loop when a single handle weight is skewed
                        while (move == even.ownership || (move == lastMove && conflicts.size() > 0)) {
                                move = weightedMoveGenerator(odd.weights);
                                cycle++;
                                if (cycle > n) {
                                        odd.weights[move] = .6/Math.pow(2,turn/2);
                                }
                        }
                }
       
                /*
                 * Give up even ownership when seen to be in deadlock.
                 */
                if (turn > 3*n) {
                        odd.weights[even.ownership] = 1;
                        even.ownership = 0;
                }
        
                lastMove = move;
                return lastMove;
	}

        private void weightCalculator(RoundInformation previous, RoundInformation current, boolean[] lastMoveConflicts) {
                /*
                 * Use last turn's conflicts to adjust weights.
                 * Conflicting with the same player consecutive times 
                 * when visiting a particular handle will decrease the weight of that
                 * handle for odd turns. In addition, conflicting with a player on a
                 * handle on odd turns means they most likely do not have ownership of that handle
                 * on even turns.
                 *
                 * 'RoundInformation previous' is round of last move.
                 */
                for (int player=1; player<n+1; player++) {
                        if (lastMoveConflicts[player] == true) {
                                if (!previous.conflicts.get(lastMove).contains(player))
                                        previous.conflicts.get(lastMove).add(player);

                                /* Iterate through each handle, removing conflicts
                                 * with the same player at other locations
                                 */
                                for (int handle=1; handle<n+1; handle++) {
                                    if (handle != lastMove) {
                                        previous.conflicts.get(handle).remove(new Integer(player));
                                        // Checking if conflicted with other players at this handle
                                        if (previous.conflicts.get(handle).size() == 0) {
                                                previous.weights[handle] = 1;
                                        }
                                    }
                                }

                                /* If the player owns a handle during a previous round,
                                 * it will not have ownership of that handle in a current round
                                 */
                                current.conflicts.get(lastMove).remove(new Integer(player));
                                if (current.conflicts.get(lastMove).size() == 0) {
                                        current.weights[lastMove] = 1;
                                }
                                
                        } else { // No conflict
                                previous.conflicts.get(lastMove).remove(new Integer(player));
                                if (previous.conflicts.get(lastMove).size() == 0) {
                                        previous.weights[lastMove] = 1; 
                                }
                        }
                }
                        
                /*
                 * Max conflict is calculated by finding the maximum for the number of
                 * consecutive times a player has conflicting with another specific player
                 * when visiting a handle.
                 */
                if (previous.conflicts.get(lastMove).size() > 0)
                        previous.weights[lastMove] = .6/Math.pow(2,turn/2);
        }

        private int weightedMoveGenerator(double[] weights) {
                double totalWeight = 0;

                for (int i=1;i<n+1;i++) {
                        totalWeight += weights[i];
                }
                        
                double random = rand.nextDouble()*totalWeight;
                for (int i=1; i<n+1; i++) {
                        random -= weights[i];
                        if (random <= 0) {
                                return i;
                        }          
                }
                return 0; // Should never reach
        }
}
