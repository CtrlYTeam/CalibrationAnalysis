package calibration;

/**
 *  Standard deviation of a sample.
 */
public class StandardDeviation {

    /**
     * Constructor, given an array return the standard deviation of the elements of the array.
     *   stdev = sqrt( sum ( (xn - avg(x))^2 ) / ( N-1) )
     */
    public static double calcStandardDeviation(double x[])
    {
        double sum = 0.0;
        double standardDeviation = 0.0;
        int length = x.length;

        for(double num : x) {
            sum += num;
        }

        double mean = sum/length;

        for(double num: x) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation/(length-1));
    }
    
    public static void main(String[] args) {
        double[] x = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        double SD = calcStandardDeviation(x);

        System.out.format("Standard Deviation = %.6f", SD);
    }
    
}
