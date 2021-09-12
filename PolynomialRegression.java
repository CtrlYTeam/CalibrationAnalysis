package calibration;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.function.IntToDoubleFunction;
import java.util.List;
import java.util.stream.IntStream;

// http://rosettacode.org/wiki/Polynomial_regression#Java
// https://www.codeproject.com/Articles/63170/Least-Squares-Regression-for-Quadratic-Curve-Fitti
 
public class PolynomialRegression {
    public static List<Double> polyRegression(double[] x, double[] y) {
    
        List<Double> params = new ArrayList<>();
    
        if (x.length != y.length) {
            System.out.println("BOO!! Lengths of x,y don't match");
            return params;
        }
        int n = x.length;
        
        double s00 = x.length;
        double s10 = Arrays.stream(x).sum();
        double s20 = Arrays.stream(x).map(a -> a * a).sum();
        double s30 = Arrays.stream(x).map(a -> a * a * a).sum();
        double s40 = Arrays.stream(x).map(a -> a * a * a * a).sum();        
        double s01 = Arrays.stream(y).sum();
        double s11 = 0.0;
        for (int i = 0; i < n; i++) {
            s11 += x[i] * y[i];
        }
        double s21 = 0.0;
        for (int i = 0; i < n; i++) {
            s21 += x[i] * x[i] * y[i];
        }
        
        //System.out.printf("s00 = %f\n", s00);
        //System.out.printf("s10 = %f\n", s10);
        //System.out.printf("s20 = %f\n", s20);
        //System.out.printf("s30 = %f\n", s30);
        //System.out.printf("s40 = %f\n", s40);
        //System.out.printf("s01 = %f\n", s01);
        //System.out.printf("s11 = %f\n", s11);
        //System.out.printf("s21 = %f\n", s21);
        
        double det  = s40*(s20*s00 - s10*s10) - s30*(s30*s00 - s10*s20) + s20*(s30*s10 - s20*s20);
        double detA = s21*(s20*s00 - s10*s10) - s11*(s30*s00 - s10*s20) + s01*(s30*s10 - s20*s20);
        double detB = s40*(s11*s00 - s01*s10) - s30*(s21*s00 - s01*s20) + s20*(s21*s10 - s11*s20);
        double detC = s40*(s20*s01 - s10*s11) - s30*(s30*s01 - s10*s21) + s20*(s30*s11 - s20*s21);
        
        double a = detA/det;
        double b = detB/det;
        double c = detC/det;
         
        double[] abc = new double[n];
        for (int i = 0; i < n; i++) {
            abc[i] = a + b*x[i] + c*x[i]*x[i];
        }
        
        double ymean = s01/n;
        
        
        double totalSumOfSquares = 0.0;
        double residualSumOfSquares = 0.0;
        for (int i = 0; i < n; i++) {
            totalSumOfSquares += Math.pow(y[i] - ymean, 2);
            residualSumOfSquares += Math.pow(y[i] - (x[i]*x[i]*a + x[i]*b + c), 2);
        }
        double rSquared = 1 - (residualSumOfSquares/totalSumOfSquares);
        
 
        System.out.println("y = " + c + " + " + b + " x + " + a + " x^2");
        //System.out.println(" Input  Approximation");
        //System.out.println(" x   y     y1");
        //for (int i = 0; i < n; ++i) {
            //System.out.printf("%3f  %3f  %5.1f\n", x[i], y[i], abc[i]);
        //}
        System.out.printf("R^2 = %f\n", rSquared);
        
        // return list of parameter values, 
        // in N order of R**2 followed by constants N=0,1,2 for x**N
        params.add(rSquared);
        params.add(c);
        params.add(b);
        params.add(a);
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
        polyRegression(x, y);
    }
}
