package calibration;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.function.IntToDoubleFunction;
import java.util.List;
import java.util.stream.IntStream;

// https://introcs.cs.princeton.edu/java/97data/LinearRegression.java.html
 
public class LinearRegression { 
    public static List<Double> linRegression(double[] x, double[] y) {

        List<Double> params = new ArrayList<>();

        if (x.length != y.length) {
            System.out.println("BOO!! Lengths of x,y don't match");
            return params;
        }
        int n = x.length;

        double sumx  = Arrays.stream(x).sum();
        double sumx2 = Arrays.stream(x).map(a -> a * a).sum();
        double sumy  = Arrays.stream(y).sum();
        double xbar = sumx / n;
        double ybar = sumy / n;

        // second pass: compute summary statistics
        double xxbar = 0.0; 
        double yybar = 0.0; 
        double xybar = 0.0;
        for (int i = 0; i < n; i++) {
            xxbar += (x[i] - xbar) * (x[i] - xbar);
            yybar += (y[i] - ybar) * (y[i] - ybar);
            xybar += (x[i] - xbar) * (y[i] - ybar);
        }
        double beta1 = xybar / xxbar;
        double beta0 = ybar - beta1 * xbar;

        // print results
        System.out.println("y = " + beta1 + " x + " + beta0);

        // analyze results
        double ssr = 0.0;      // regression sum of squares
        for (int i = 0; i < n; i++) {
            double fit = beta1*x[i] + beta0;
            ssr += (fit - ybar) * (fit - ybar);
        }
        double R2    = ssr / yybar;
        System.out.println("R^2 = " + R2);

        // return list of parameter values, 
        // in N order of R**2 followed by constants N=0,1,2 for x**N        
        params.add(R2);
        params.add(beta0);
        params.add(beta1);
        return params;        
    }

    public static void main(String[] args) {
//        int[] x = IntStream.range(0, 11).toArray();
        int[] xi = IntStream.range(-3,4).toArray();
        double[] x = new double[xi.length];
        for (int i = 0; i < xi.length; i++) {
            x[i] = xi[i];
        }
//        int[] y = new int[]{1, 6, 17, 34, 57, 86, 121, 162, 209, 262, 321};
        double[] y = new double[]{7.5, 3, 0.5, 1, 3, 6, 14};
        linRegression(x, y);
    }
}
