package escape.g2c;

import java.util.List;
import java.util.ArrayList;

/*
 * Helper class to store data for odd/even rounds
 */
public class RoundInformation {
    protected int ownership;
    protected List<List<Integer>> conflicts;
    protected double[] weights;

    public RoundInformation(int n) {
        ownership = 0;
        
        conflicts = new ArrayList<List<Integer>>(n+1);
        for (int i=0; i<n+1; i++) {
                conflicts.add(new ArrayList<Integer>());
        }
        
        weights = new double[n+1];
    }
}
