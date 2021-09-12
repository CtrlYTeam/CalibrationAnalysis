package calibration;  

import java.io.BufferedWriter;
import java.io.IOException;

import java.math.BigDecimal;
import java.math.MathContext;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;



/**
 * Class to perform calibration analysis.
 */
public class CalibrateAccel {

    String dataFile = "CalibAccel.txt";        // default data file name, can be overwritten from a command line argument.
    String outFile  = "ParametersAccel.txt";   // default output file name, can be overwritten from a command line argument.
    
    StringBuffer msgOutFile = new StringBuffer();
    
    int rampUpEndIdx = -1;
    int steadyIdx = -1;
    double steadyLeftVelocity;
    double steadyRightVelocity;
    double ratioV;

    
    /**
     * Class constructor, runs analysis of calibration data.
     */
    public CalibrateAccel(String[] args){
    
        // Error-checking flags. If any of these are asserted false then something
        // is wrong with the calibration data provided. 
        boolean allProfilesValid = true;        
        boolean allProfilesRampUp = true;
        
        // This checks the command line arguments and loads
        // parameters into the SettingsFrame mySettings.
        parseArgs(args);
        
        // Open, read & close calibration data file
        // If there is a file problem or if there are no profiles, then simply exit
        List<CalibProfile> calibProfiles = CalibrationFileHandler.readCalibrationDataFile(dataFile);
        if (calibProfiles == null) { return; }
        
        // Open calibration parameter file to write
        // If there is a file problem, simply exit
        BufferedWriter writer = CalibrationFileHandler.openWriteFile(outFile);
        if (writer == null) { return; }
        
        // pull properties from each profile
        List<Double>  leftMeasure  = new ArrayList<>();
        List<Double>  rightMeasure = new ArrayList<>();
        List<Integer> leftTicks    = new ArrayList<>();
        List<Integer> rightTicks   = new ArrayList<>();
        List<Double>  powerList    = new ArrayList<>();
        List<Double>  throttleList = new ArrayList<>();

        // Iterate through list of profiles
        for (CalibProfile profile : calibProfiles) {
              
            System.out.printf("lm=%f\n", profile.leftMeasure1);
              
            // Set up 5 arrays, containing the series of data values from the data file:
            //
            //      Timestamp
            //      Left encoder
            //      Left velocity
            //      Right encoder
            //      Right velocity
            //  
            int numPoints = profile.calibPoints.size();
            //        
            double[] ts = new double[numPoints];
            int[]    le = new int   [numPoints];
            double[] lv = new double[numPoints];
            int[]    re = new int   [numPoints];
            double[] rv = new double[numPoints];
            int idx = 0;
            //
            for (CalibPoint cp : profile.calibPoints) {
                ts[idx] = cp.timeStamp;
                le[idx] = cp.leftEncoder;
                lv[idx] = cp.leftVelocity;
                re[idx] = cp.rightEncoder;
                rv[idx] = cp.rightVelocity;
                idx++;
            }
                        
            
            // Validity check for data
            // If invalid, set profileIsValid flag
            // As soon as any invalidity is found then this entire data set is bad, don't need to look for 
            //   any other invalidities for the given profile
            //
            // The timestamp, and left,right encoders should all be strictly increasing.
            // If a robot wasn't fully stopped it may show encoder readings from a previous run.
            // Using data from a robot that wasn't stopped will give a bad regression.
            // 
            boolean profileIsValid = true;
            for (int vdx = 1; vdx < numPoints; vdx++) {
                if (ts[vdx] < ts[vdx-1]) {
                    System.out.printf("BOO! power %f time %f at %d not increasing over time %f at %d\n", 
                                        profile.nominalPower, ts[vdx], vdx, ts[vdx-1], vdx-1);
                    profileIsValid = false;
                    allProfilesValid = false;
                    break;
                }
                if (le[vdx] < le[vdx-1]) {
                    System.out.printf("BOO! power %f left encoder %d at %d not increasing over %d at %d\n", 
                                        profile.nominalPower, le[vdx], vdx, le[vdx-1], vdx-1);
                    profileIsValid = false;
                    allProfilesValid = false;
                    break;
                }
                if (re[vdx] < re[vdx-1]) {
                    System.out.printf("BOO! power %f right encoder %d at %d not increasing over %d at %d\n", 
                                        profile.nominalPower, re[vdx], vdx, re[vdx-1], vdx-1);
                    profileIsValid = false;
                    allProfilesValid = false;
                    break;
                }                    
            }
            // if the data is invalid, then terminate this iteration of the for-loop and
            // move on to the next profile
            if (!profileIsValid) {
                continue;
            }
            
            // From each profile, need to take:
            // left,right of ticks, measures
            // nominal power and acceleration throttle
            leftMeasure.add(profile.leftMeasure1);
            rightMeasure.add(profile.rightMeasure1);
            leftTicks.add(le[le.length-1]);
            rightTicks.add(re[re.length-1]);
            powerList.add(profile.nominalPower);
            throttleList.add(profile.accelerationThrottle);
        }
        
        
        // Now consider the profiles as a whole...
        
        // Search for profiles with matching pairs of acceleration throttle and nominal power
        // These pairs of profiles are required to solve for S,L in Tn = S*In + L
        
        double maxPassFwd = 0.0;
        double maxPassBck = 0.0;
        double minFailFwd = 1.0;
        double minFailBck = 1.0;
        
        
        for (int idx = 0; idx < powerList.size(); idx++) {
            for (int jdx = idx+1; jdx < powerList.size(); jdx++) {
//                System.out.printf("%f %f %f %f\n", powerList.get(idx), powerList.get(jdx), throttleList.get(idx), throttleList.get(jdx));
                
                double pwrDiff = Math.abs(powerList.get(idx)    - powerList.get(jdx));
                double thrDiff = Math.abs(throttleList.get(idx) - throttleList.get(jdx));
                if (  (pwrDiff < 0.000001 ) &&  
                      (thrDiff < 0.000001 ) 
                   ) {
                                        
                    double tpiLeft  = Math.abs( (leftTicks.get(idx) - leftTicks.get(jdx)) / (leftMeasure.get(idx) - leftMeasure.get(jdx)) );
                    double slipLeft = leftTicks.get(idx) - tpiLeft * leftMeasure.get(idx);
                    
                    double tpiRight  = Math.abs( (rightTicks.get(idx) - rightTicks.get(jdx)) / (rightMeasure.get(idx) - rightMeasure.get(jdx)) );
                    double slipRight = rightTicks.get(idx) - tpiRight * rightMeasure.get(idx);
                    
                    System.out.printf("NominalPower=%f AccelerationThrottle=%f\n", powerList.get(idx), throttleList.get(idx));
                    System.out.printf("S(Lt)=%f L=%f\n", tpiLeft, slipLeft); 
                    System.out.printf("S(Rt)=%f L=%f\n", tpiRight, slipRight); 
                    
                    double slipThreshold = 0.25;
                    double ratioL = slipLeft  / tpiLeft;
                    double ratioR = slipRight / tpiRight;
                    boolean leftOk = (ratioL <= slipThreshold);
                    boolean rightOk = (ratioR <= slipThreshold);
                    
                    System.out.printf("Lt:%f %s Rt:%f %s\n", ratioL, leftOk ? "PASS" : "FAIL", ratioR, rightOk ? "PASS" : "FAIL");
                    
                    // For passes, track the highest passing throttle
                    if (leftOk && rightOk) {
                        if ((powerList.get(idx) > 0) && (throttleList.get(idx) > maxPassFwd)) {
                            maxPassFwd = throttleList.get(idx);
                        }
                        if ((powerList.get(idx) > 0) && (throttleList.get(idx) > maxPassBck)) {
                            maxPassBck = throttleList.get(idx);
                        }                        
                    }
                    // For fails, track the lowest failing throttle
                    else {
                        if ((powerList.get(idx) > 0) && (throttleList.get(idx) < minFailFwd)) {
                            minFailFwd = throttleList.get(idx);
                        }
                        if ((powerList.get(idx) > 0) && (throttleList.get(idx) > minFailBck)) {
                            minFailBck = throttleList.get(idx);
                        }                        
                    
                    }                    
                }
            }
        }
        
        // Take the minimum of the highest passing throttle and the lowest failing throttle
        double maxThrottleFwd = Math.min(maxPassFwd, minFailFwd);
        double maxThrottleBck = Math.min(maxPassBck, minFailBck);
        String msg = "MAX_FWD_PWR_ACCEL = " + string3sig(maxThrottleFwd) + "\n";
        System.out.printf("%s", msg);
        msgOutFile.append(msg);
        msg = "MAX_BCK_PWR_ACCEL = " + string3sig(maxThrottleBck) + "\n";
        System.out.printf("%s", msg);
        msgOutFile.append(msg);

        
/*                
        // Do linear regression on ticks/measure for left, right; both forward, backward
        int tdx;
        // Forward movement
        for (tdx = 0; tdx < stopFwdTicks.size(); tdx++) {
            System.out.printf("Forward velocity: %f ticks to stop: %d\n", stopFwdVelocity.get(tdx), stopFwdTicks.get(tdx));
        }
        double[] tckF = stopFwdTicks.stream().mapToDouble(a -> a).toArray();
        double[] velF = stopFwdVelocity.stream().mapToDouble(a -> a).toArray();
        List<Double> lrStopParameters = LinearRegression.linRegression(velF, tckF);  // 'x' = vel, 'y' = tck
        System.out.printf("Forward: Ticks = %f * velocity(t/ms) + %f\n", lrStopParameters.get(2), lrStopParameters.get(1));
        //        
        msgOutFile.append("FLOAT_DN_FWD_PWR_SLOPE  = "+string3sig(lrStopParameters.get(2))+"\n");
        msgOutFile.append("FLOAT_DN_FWD_PWR_OFFSET = "+string3sig(lrStopParameters.get(1))+"\n");
        //
        // Backward movement
        for (tdx = 0; tdx < stopBckTicks.size(); tdx++) {
            System.out.printf("Backward velocity: %f ticks to stop: %d\n", stopBckVelocity.get(tdx), stopBckTicks.get(tdx));
        }
        double[] tckB = stopBckTicks.stream().mapToDouble(a -> a).toArray();
        double[] velB = stopBckVelocity.stream().mapToDouble(a -> a).toArray();
        lrStopParameters.clear();
        lrStopParameters = LinearRegression.linRegression(velB, tckB);  // 'x' = vel, 'y' = tck
        System.out.printf("Backward: Ticks = %f * velocity(t/ms) + %f\n", lrStopParameters.get(2), lrStopParameters.get(1));
        //        
        msgOutFile.append("FLOAT_DN_BCK_PWR_SLOPE  = "+string3sig(lrStopParameters.get(2))+"\n");
        msgOutFile.append("FLOAT_DN_BCK_PWR_OFFSET = "+string3sig(lrStopParameters.get(1))+"\n");
        
        
*/        
        
        if (!allProfilesValid) {
            System.out.printf("\nBOO! Calibration data file has critical problems.\n");
        } else {
            System.out.printf("Success\n");
        }
        
        // Write and Close write file
        try {
            writer.write(msgOutFile.toString());
            writer.close();
        } catch (IOException e) {
            System.out.printf("\nBOO! Failed to write to Output file.\n");
        }
        

    }
    
    /**
     * Parse the commane line arugments
     * @param args - Array of command line arguments
     */
    private void parseArgs(String[] args) {
    
        boolean setOutputFilename = false;
        
        // If '--help' is any of the arguments then 
        // show proper command line usage and exit
        for (String arg : args) {
            //
            // Always good to have an option to show what the valid command arguments are.
            //
            if (arg.equals("--help") || arg.equals("-help")) {
                System.out.println("Usage:");
                System.out.println("Calibration [<DataFileName>] | [[-]-help] |");
                System.out.println("    [-o <OutputFileName>]");                                
                System.exit(0);
            }
            //
            // Look for -o command. 
            // The next arg after -o is the image output filename.            
            //
            if (setOutputFilename) {
                outFile = arg;
                setOutputFilename = false;
            }
            if (arg.equals("-o")) {
                setOutputFilename = true;                            
            }
            
            
            // Data file name can only be first argument.
            // All commands have a '-' prefix.
            if ((args.length > 0) && (args[0].charAt(0) != '-')) {
                dataFile = args[0];
            }
        }
    }
       
    /**
     * Return String of double rounded to 3 significant figures.
     * Trailing zeros are omitted, even if they are a significant digit.
     */
    private String string3sig(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.round(new MathContext(3));
        return String.valueOf(bd.doubleValue());
    }

       
    /**
     * Find the end of the ramp-up and find the steady-state ratio of the left/right velocities
     */
    private void inflectionPoint(double[] timeStamp, double[] leftVelocity, double[] rightVelocity) {
    
        final double TIME_FLATNESS = 100.0; // ms
        int idx;
        
        //
        // Look for the first stretch of time of TIME_FLATNESS duration that has each of 
        // all samples of left and right velocities within a fixed value of each other.
        //
        // By observation, the Hub reports velocites to the 0.02 ticks/millisecond.
        //
        for (idx = 0; idx < timeStamp.length; idx++) {
            double endTime = timeStamp[idx] + TIME_FLATNESS;
            int jdx = idx;
            while ((jdx < timeStamp.length) && (timeStamp[jdx] < endTime)) { jdx++; }
            if (jdx < timeStamp.length) {
                double maxLeftVelocity = 0.0;
                double minLeftVelocity = Double.POSITIVE_INFINITY;
                double maxRightVelocity = 0.0;
                double minRightVelocity = Double.POSITIVE_INFINITY;
                double sumLeftVelocity = 0.0;
                double sumRightVelocity = 0.0;
                for (int i = idx; i <= jdx ; i++) {
                    maxLeftVelocity = Math.max(maxLeftVelocity, leftVelocity[i]);
                    minLeftVelocity = Math.min(minLeftVelocity, leftVelocity[i]);
                    sumLeftVelocity += leftVelocity[i];
                    maxRightVelocity = Math.max(maxRightVelocity, rightVelocity[i]);
                    minRightVelocity = Math.min(minRightVelocity, rightVelocity[i]);
                    sumRightVelocity += rightVelocity[i];
                }
                if (((maxLeftVelocity - minLeftVelocity) < 0.021) &&
                    ((maxRightVelocity - minRightVelocity) < 0.021)) {
                    steadyLeftVelocity = sumLeftVelocity / (jdx - idx + 1);
                    steadyRightVelocity = sumRightVelocity / (jdx - idx + 1);
                    steadyIdx = idx;
                    break;
                }
            }
        }
        
        //
        // Given the steady-state velocities, define the end-of-ramp-up as the sample
        // where the difference in velocity ratios is within 3% of the steady-state ratio.
        // Note: 3% is chosen by observing data taken when writing this file
        //
        ratioV = Math.min(steadyLeftVelocity, steadyRightVelocity)/Math.max(steadyLeftVelocity, steadyRightVelocity);
        boolean leftIsMax = steadyLeftVelocity >= steadyRightVelocity;
        System.out.println("leftIsMax="+leftIsMax);
        
        for (idx = 0; idx < timeStamp.length; idx++) {
            if ((leftVelocity[idx] > 0.0) && (rightVelocity[idx] > 0.0)) {            
                double ratio = rightVelocity[idx]/leftVelocity[idx];
                if (ratio > 1) { ratio = 1.0/ratio; }
                if (ratio > (1-0.03)*ratioV) {
                    rampUpEndIdx = idx;
                    break;
                }
            }            
        }
        
    }       
       
       
    /**
     * Best Stdev
     */
     
    public List<Double> bestStdDev ( List<List<Double>> valueSets ) {
    
        List<Double> params = new ArrayList<>();
    
        // identify the worst sample of each set
        double[] worst = new double[valueSets.size()];
        int wdx = 0;
        for (List<Double> valueSet : valueSets) {
            // find std dev for all samples
            double[] values = valueSet.stream().mapToDouble(a -> a).toArray();
//            double stdDev = StandardDeviation.calcStandardDeviation(values);
            double mean = Arrays.stream(values).sum() / valueSet.size();
            
            // meh until fully vetting how to tweak out a bad sample, use mean of all of them
            params.add(mean);
        }
        return params;
    }
            
/*            

            worst[wdx] = 0;
            double worstDeviation = -1;
            for (int i = 0; i < values.length; i++) {
                double deviation = Math.abs(values[i] - mean) / stdDev;
                if (deviation > worstDeviation) {
                    worst[wdx] = i;
                    worstDeviation = deviation;
                }
            }
        }
        
        int worstSample = worst[0];
        boolean worstInAllSets = true;
        for (int j = 1; j < worst.length; j++) {
            if (worstSample != worst[j]) {
                worstInAllSets = false;
                break;
            }
        }
        if (worstInAllSets) {
            // meh is it REALLY way-off from the rest??
        }                
    }
*/    
    
    /**
     * Main method run at command line.
     */
    public static void main(String[] args) {
        new CalibrateAccel(args);
    }
}
