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
public class CalibrateOP {

    String dataFile = "CalibOP.txt";        // default data file name, can be overwritten from a command line argument.
    String outFile  = "ParametersOP.txt";   // default output file name, can be overwritten from a command line argument.
    
    StringBuffer msgOutFile = new StringBuffer();
    
    int rampUpEndIdx = -1;
    int steadyIdx = -1;
    double steadyLeftVelocity;
    double steadyRightVelocity;
    double ratioV;

    
    /**
     * Class constructor, runs analysis of calibration data.
     */
    public CalibrateOP(String[] args){
    
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
        
        
        // Track the stopping distances from velocities in profile
        List<Double>  fwdLeftMeasure = new ArrayList<>();
        List<Integer> fwdLeftTicks   = new ArrayList<>();
        List<Double>  bckLeftMeasure = new ArrayList<>();
        List<Integer> bckLeftTicks   = new ArrayList<>();

        List<Double>  fwdRightMeasure = new ArrayList<>();
        List<Integer> fwdRightTicks   = new ArrayList<>();
        List<Double>  bckRightMeasure = new ArrayList<>();
        List<Integer> bckRightTicks   = new ArrayList<>();
        
        List<Double>  fwdLR = new ArrayList<>();
        List<Double>  bckLR = new ArrayList<>();
        List<Double>  fwdRL = new ArrayList<>();
        List<Double>  bckRL = new ArrayList<>();
        
        // Iterate through list of profiles
        for (CalibProfile profile : calibProfiles) {
              
//            System.out.printf("lm=%f\n", profile.leftMeasure1);
              
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
            // Check for sequence in profile
            if (!profile.sequence.equals("LR") && !profile.sequence.equals("RL")) {
                profileIsValid = false;
                allProfilesValid = false;
            }
            // if the data is invalid, then terminate this iteration of the for-loop and
            // move on to the next profile
            if (!profileIsValid) {
                continue;
            }
            
            // Find integral of velocity ratio over time
            boolean equaled = false;
            int tdx = 0;
            double ratio;
            double lastRatio = 1.0;
            boolean leftIsOP = profile.sequence.equals("LR");
            double integral = 0;
            
            while (!equaled) {
                if ((lv[tdx] == 0.0) && (rv[tdx] == 0.0)) { ratio = 1.0; }
                else {
                    if (leftIsOP) {
                        ratio = rv[tdx]/lv[tdx];
                    }
                    else {
                        ratio = lv[tdx]/rv[tdx];
                    }
                    equaled = ratio >= 0.97;
                }
                
                if (tdx > 0) {
                    // timestamp of interval * ratio difference
                    integral += (ts[tdx] - ts[tdx-1]) * (ratio + lastRatio)/2.0;
                }                
                tdx++;
                lastRatio = ratio;
                //System.out.printf("%f %f\n", ratio, integral);
            }
            System.out.printf("%s %5.2f %f\n", profile.sequence, profile.nominalPower, integral);
            
            if (profile.nominalPower > 0) {
                if (leftIsOP) fwdLR.add(integral);
                else          fwdRL.add(integral);
            } else {
                if (leftIsOP) bckLR.add(integral);
                else          bckRL.add(integral);
            }

            
        }
        
        
        // Now consider the profiles as a whole...
        
        double fwdLRavg = fwdLR.stream().mapToDouble(a -> a).sum() / fwdLR.size();
        double fwdRLavg = fwdRL.stream().mapToDouble(a -> a).sum() / fwdRL.size();
        double bckLRavg = bckLR.stream().mapToDouble(a -> a).sum() / bckLR.size();
        double bckRLavg = bckRL.stream().mapToDouble(a -> a).sum() / bckRL.size();
        System.out.printf("FLR:%f FRL:%f BLR:%f BRL:%f\n", fwdLRavg, fwdRLavg, bckLRavg, bckRLavg);
        
        String msg;

        if (fwdLRavg >= fwdRLavg) {
            msg = "LEFT_IS_FWD_OP = true\n";
        } else {
            msg = "LEFT_IS_FWD_OP = false\n";
        }
        msgOutFile.append(msg);
        System.out.printf("%s", msg);

        if (bckLRavg >= bckRLavg) {
            msg = "LEFT_IS_BCK_OP = true\n";
        } else {
            msg = "LEFT_IS_BCK_OP = false\n";
        }
        msgOutFile.append(msg);
        System.out.printf("%s", msg);
        
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
     * Main method run at command line.
     */
    public static void main(String[] args) {
        new CalibrateOP(args);
    }
}
