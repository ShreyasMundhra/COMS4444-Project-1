package escape.g2c;

import java.util.List;
import java.util.Random;
import java.lang.Math;
import java.util.Arrays;

public class Player implements escape.sim.Player {
	private Random rand;
	private int turn;
        private int lastMove;
        private int n;

        private int oddOwnership;
        private double[] oddWeights;
        private int[] oddPlayerWithMostConflicts;
        private int[][] oddConflicts; // Matrix represents [handles][players]

        private int evenOwnership;
        private double[] evenWeights;
        private int[] evenPlayerWithMostConflicts;
        private int[][] evenConflicts;

	public Player() {
		rand = new Random();
	}

	@Override
	public int init(int n) {
		this.turn = 0;
                this.n = n;

		this.oddOwnership = 0;
                this.oddWeights = new double[n+1];
                this.oddPlayerWithMostConflicts = new int[n+1];
                this.oddConflicts = new int[n+1][n+1];

                this.evenOwnership = 0;
                this.evenWeights = new double[n+1];
                this.evenPlayerWithMostConflicts = new int[n+1];
                this.evenConflicts = new int[n+1][n+1];

                for (int i=0; i < n+1; i++) {
                    oddWeights[i] = evenWeights[i] = 1; // All weights start at 1.0
                }

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
        // 8. If the player collides with a player at the same spot multiple times, the player becomes less likely to return to that spot.
        // 9. If a player collides with another player during an even round, it knows that player does not own that spot for odd rounds.
	@Override
	public int attempt(List<Integer> conflicts) {
                turn ++;
                int move = 0;
                int maxConflicts = 0;
                      
                // Marking conflicts from last turn
                boolean[] lastMoveConflicts = new boolean[n+1];
                for (int i=0; i < conflicts.size(); i++) {
                        lastMoveConflicts[conflicts.get(i)] = true;
                }

                if ((turn % 2) == 0) {
                        if (conflicts.size() == 0) { // Obtain ownership
                                oddOwnership = lastMove;
                        }

                        /*
                         * Use last turn's conflicts to adjust weights.
                         * Conflicting with the same player consecutive times 
                         * when visiting a particular handle will decrease the weight of that
                         * handle for odd turns. In addition, conflicting with a player on a
                         * handle on odd turns means they most likely do not have ownership of that handle
                         * on even turns.
                         */
                        for (int player=1; player<n+1; player++) {
                            if (lastMoveConflicts[player] == true) {
                                oddConflicts[lastMove][player]++;

                                for (int handle=1; handle<n+1; handle++) {
                                    
                                    // Remove conflicts with same player at other handles
                                    if (handle != lastMove) {
                                        oddConflicts[handle][player] = 0;
                                        if (oddPlayerWithMostConflicts[handle] == player) {
                                                oddPlayerWithMostConflicts[handle] = 0;
                                                oddWeights[handle] = 1;
                                        }
                                    }
                                }
                                
                                if (oddConflicts[lastMove][player] > maxConflicts) {
                                    maxConflicts = oddConflicts[lastMove][player];
                                    oddPlayerWithMostConflicts[lastMove] = player;
                                }

                                /* If the player owns a handle during an even round,
                                 * it will not have ownership of that handle in an odd round
                                 */ 
                                evenConflicts[lastMove][player] = 0;
                                if (evenPlayerWithMostConflicts[lastMove] == player) {
                                        evenPlayerWithMostConflicts[lastMove] = 0;
                                        evenWeights[lastMove] = 1;
                                }
                                
                            } else { // No conflict
                                oddConflicts[lastMove][player] = 0;
                                if (oddPlayerWithMostConflicts[lastMove] == player) {
                                        oddPlayerWithMostConflicts[lastMove] = 0;
                                        oddWeights[lastMove] = 1;
                                }
                            }
                        }
                        
                        /*
                         * Max conflict is calculated by finding the maximum for the number of
                         * consecutive times a player has conflicting with another specific player
                         * when visiting a handle.
                         */
                        oddWeights[lastMove] = Math.pow(.6/Math.pow(2,Math.log(turn)),maxConflicts);
                                
                        if (evenOwnership > 0) { // Checking for next round ownership
                                if (evenOwnership == oddOwnership || (evenOwnership == lastMove && conflicts.size() > 0)) {
                                        evenWeights[evenOwnership] = 1;
                                        evenConflicts[evenOwnership] = new int[n+1];
                                        evenOwnership = 0;
                                } else {
                                        lastMove = evenOwnership;
                                        return lastMove;
                                }
                        }

                        double totalWeight = 0;
                        for (int i=1;i<n+1;i++) {
                                totalWeight += evenWeights[i] * (1/Math.sqrt(oddWeights[i]));
                        }
                        
                        // Weighted random choice generator
                        while (true) {
                                double random = rand.nextDouble()*totalWeight;
                                for (int i=1; i<n+1; i++) {
                                    random -= evenWeights[i]*(1/Math.sqrt(oddWeights[i]));
                                    if (random <= 0) {
                                                move = i;
                                                break;
                                        }          
                                }
                                
                                if (move != lastMove || conflicts.size() == 0)
                                    break;
                        }
                } else {
                        if (evenOwnership != lastMove && conflicts.size() == 0){ // Obtain ownership
                                evenOwnership = lastMove;
                                System.out.println(evenOwnership);
                        }
                        
                        // Use last turn's conflicts to adjust weights
                        for (int player=1; player < n+1; player++) {
                            if (lastMoveConflicts[player] == true) {
                                evenConflicts[lastMove][player]++;
                                
                                for (int handle=1; handle<n+1; handle++) {

                                    // Remove conflicts with same player at other handles
                                    if (handle != lastMove) {
                                        evenConflicts[handle][player] = 0;
                                        if (evenPlayerWithMostConflicts[handle] == player) {
                                                evenPlayerWithMostConflicts[handle] = 0;
                                                evenWeights[handle] = 1;
                                        }
                                    }
                                }

                                if (evenConflicts[lastMove][player] > maxConflicts) {
                                    maxConflicts = evenConflicts[lastMove][player];
                                    evenPlayerWithMostConflicts[lastMove] = player;
                                }

                                /*
                                 * If the player owns a handle during an odd round,
                                 * it will not ahve ownership of that handle in an even round
                                 */
                                oddConflicts[lastMove][player] = 0;
                                if (oddPlayerWithMostConflicts[lastMove] == player) {
                                        oddPlayerWithMostConflicts[lastMove] = 0;
                                        oddWeights[lastMove] = 1;
                                }
                            } else { // No conflict
                                evenConflicts[lastMove][player] = 0;
                                if (evenPlayerWithMostConflicts[lastMove] == player) {
                                        evenPlayerWithMostConflicts[lastMove] = 0;
                                        evenWeights[lastMove] = 1 ;
                                }
                            }
                        }
                       
                        // Each handle weighted by 1/((8*(ln(turn)+1)^(Max Conflicts) 
                        evenWeights[lastMove] = Math.pow(.6/Math.pow(2,Math.log(turn)),maxConflicts);
                        
                        if (oddOwnership > 0) { // Checking for next round ownership
                                if (evenOwnership == oddOwnership || (oddOwnership == lastMove && conflicts.size() > 0)) {
                                        oddWeights[oddOwnership] = 1;
                                        oddConflicts[oddOwnership] = new int[n+1];
                                        oddOwnership = 0;
                                } else {
                                        lastMove = oddOwnership;
                                        return lastMove;
                                }
                        }

                        double totalWeight = 0;
                        for (int i=1;i<n+1;i++) {
                                totalWeight += oddWeights[i]*(1/Math.sqrt(evenWeights[i]));
                        }

                        // Weighted random choice generator
                        while (true) {
                                double random = rand.nextDouble()*totalWeight;
                                for (int i=1; i<n+1; i++) {
                                        random -= oddWeights[i]*(1/Math.sqrt(evenWeights[i]));
                                        if (random <= 0) {
                                                move = i;
                                                break;
                                        }          
                                }
                                if (move != lastMove || conflicts.size() == 0)
                                    break;
                        }
                }
                
                lastMove = move;
                return lastMove;
	}
}
