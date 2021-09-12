package calibration;  

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class CalibrationFileHandler {
    // readCalibrationDataFile
    // openWriteFile
        
        
    /**
     * Read data from calibration file.
     * @param fileName - name of calibration data file
     * @return         - list of calibration data points
     */
    public static List<CalibProfile> readCalibrationDataFile(String fileName) {
        
        List<CalibProfile> calibprofiles = new ArrayList<>();
        List<CalibPoint> calibpoints = new ArrayList<>();
        
        BufferedReader reader;
        String[] chunks;
        String[] subchunks;
        
        double batteryVoltage = 0.0;        
        double nominalPower   = 0.0;
        double accelerationThrottle = 1.0;
        String sequence       = "";
        double leftMeasure1   = 0.0;
        double leftMeasure2   = 0.0;
        double rightMeasure1  = 0.0;
        double rightMeasure2  = 0.0;
        boolean acquireData   = false;

        try {
            reader = new BufferedReader(new FileReader(fileName));
            System.out.println("Reading Vector data file: "+fileName);
            int lineNum = 1;
            String line = reader.readLine();
            while (line != null) {
                chunks = line.split("\t");
                
                // looking for something like:
                // 005.123 : Robot battery voltage = 12.687
                if (chunks[0].contains("battery voltage")) {
                    subchunks = chunks[0].split("=");
                    batteryVoltage = Double.parseDouble(subchunks[1]);
                }
                // looking for something like:
                // 005.123 : Nominal power: 0.40
                if (chunks[0].contains("power:")) {
                    subchunks = chunks[0].split(":");
                    if (subchunks.length == 3) {
                        //System.out.println(subchunks[0]);
                        //System.out.println(subchunks[1]);
                        nominalPower = Double.parseDouble(subchunks[2]);
                    }
                }
                // looking for something like:
                // 005.123 : IMU heading at Begin: -0.000000
                if (chunks[0].contains("IMU") && chunks[0].contains("Begin")) {
                    calibpoints = new ArrayList<>();
                    acquireData = true;
                }
                // looking for something like:
                // 005.123 : 
                if (chunks[0].contains("Stopped")) {
//                    System.out.println(nominalPower);
                    CalibProfile profile = new CalibProfile(calibpoints, nominalPower, accelerationThrottle, batteryVoltage, sequence, leftMeasure1, rightMeasure1, leftMeasure2, rightMeasure2 );
                    calibprofiles.add(profile);
                    acquireData = false;
                }
                // looking for something like:
                // 005.123 : Left Measure : 0.4
                if (chunks[0].contains("Left Measure :")) {
                    subchunks = chunks[0].split(":");
                    if (subchunks.length == 3) {
                        leftMeasure1 = Double.parseDouble(subchunks[2]);
//                        System.out.println(leftMeasure1);
                    }
                }
                // looking for something like:
                // 005.123 : Right Measure : 0.4
                if (chunks[0].contains("Right Measure :")) {
                    subchunks = chunks[0].split(":");
                    if (subchunks.length == 3) {
                        rightMeasure1 = Double.parseDouble(subchunks[2]);
                    }
                }
                // looking for something like:
                // 005.123 : Left Measure2 : 0.4
                if (chunks[0].contains("Left Measure2 :")) {
                    subchunks = chunks[0].split(":");
                    if (subchunks.length == 3) {
                        leftMeasure2 = Double.parseDouble(subchunks[2]);
                    }
                }
                // looking for something like:
                // 005.123 : Right Measure2 : 0.4
                if (chunks[0].contains("Right Measure2 :")) {
                    subchunks = chunks[0].split(":");
                    if (subchunks.length == 3) {
                        rightMeasure2 = Double.parseDouble(subchunks[2]);
                    }
                }
                // looking for something like:
                // 005.123 : Sequence : LR
                if (chunks[0].contains("Sequence:")) {
                    subchunks = chunks[0].split(":");
                    if (subchunks.length == 3) {
                        sequence = subchunks[2].trim();
//                        System.out.println(sequence);
                    }
                }
                // looking for something like:
                // 005.123 : Acceleration throttle : 0.001
                if (chunks[0].contains("Acceleration throttle:")) {
                    subchunks = chunks[0].split(":");
                    if (subchunks.length == 3) {
                        accelerationThrottle = Double.parseDouble(subchunks[2]);
                    }
                }
                
                // looking for something like:
                // 005.123 : \t  50.000\t  20\t   20\t 0.520\t 0.440
                if (acquireData && (chunks.length == 6)) {
                    try {
                    
                        double ts = Double.parseDouble(chunks[1]);      // timeStamp
                        int    le = Integer.parseInt(chunks[2].trim()); // left encoder
                        int    re = Integer.parseInt(chunks[3].trim()); // right encoder
                        double lv = Double.parseDouble(chunks[4]);      // left velocity
                        double rv = Double.parseDouble(chunks[5]);      // right velocity
                        CalibPoint cpt = new CalibPoint( ts, le, re, lv, rv );
                        calibpoints.add(cpt);
                        
                    } catch ( NumberFormatException | NullPointerException e) { 
                        //e.printStackTrace(); 
                    }
                }   
                line = reader.readLine();
            }
            reader.close();
        }
        catch (IOException e) { 
            System.out.println(e);
            return null;
        }
        return calibprofiles;
    }
        
    /**
     * Read data from tick calibration file.
     * @param fileName - name of calibration data file
     * @return         - list of calibration data points
     */
    public static List<CalibProfile> readTickDataFile(String fileName) {
        
        List<CalibProfile> calibprofiles = new ArrayList<>();
        List<CalibPoint> calibpoints = new ArrayList<>();
        
        BufferedReader reader;
        String[] chunks;
        String[] subchunks;
        
        double batteryVoltage = 0.0;        
        double nominalPower   = 0.0;
        boolean acquireData   = false;

        try {
            reader = new BufferedReader(new FileReader(fileName));
            System.out.println("Reading Vector data file: "+fileName);
            int lineNum = 1;
            String line = reader.readLine();
            while (line != null) {
                chunks = line.split("\t");
                
                // looking for something like:
                // 005.123 : Robot battery voltage = 12.687
                if (chunks[0].contains("battery voltage")) {
                    subchunks = chunks[0].split("=");
                    batteryVoltage = Double.parseDouble(subchunks[1]);
                }
                // looking for something like:
                // 005.123 : Nominal power: 0.40
                if (chunks[0].contains("Nominal power:")) {
                    subchunks = chunks[0].split(":");
                    if (subchunks.length == 3) {
                        //System.out.println(subchunks[0]);
                        //System.out.println(subchunks[1]);
                        nominalPower = Double.parseDouble(subchunks[2]);
                    }
                }
                // looking for something like:
                // 005.123 : IMU heading at Begin: -0.000000
                if (chunks[0].contains("IMU") && chunks[0].contains("Begin")) {
                    calibpoints = new ArrayList<>();
                    acquireData = true;
                }
                // looking for something like:
                // 005.123 : IMU heading at Begin: -1.000000
                if (chunks[0].contains("IMU") && chunks[0].contains("End")) {
                    System.out.println(nominalPower);
                    CalibProfile profile = new CalibProfile(calibpoints, nominalPower, batteryVoltage );
                    calibprofiles.add(profile);
                    acquireData = false;
                }
                
                // looking for something like:
                // 005.123 : \t  50.000\t  20\t   20\t 0.520\t 0.440
                if (acquireData && (chunks.length == 6)) {
                    try {
                    
                        double ts = Double.parseDouble(chunks[1]);      // timeStamp
                        int    le = Integer.parseInt(chunks[2].trim()); // left encoder
                        int    re = Integer.parseInt(chunks[3].trim()); // right encoder
                        double lv = Double.parseDouble(chunks[4]);      // left velocity
                        double rv = Double.parseDouble(chunks[5]);      // right velocity
                        CalibPoint cpt = new CalibPoint( ts, le, re, lv, rv );
                        calibpoints.add(cpt);
                        
                    } catch ( NumberFormatException | NullPointerException e) { 
                        //e.printStackTrace(); 
                    }
                }   
                line = reader.readLine();
            }
            reader.close();
        }
        catch (IOException e) { 
            System.out.println(e);
            return null;
        }
        return calibprofiles;
    }
        
        
        
    /**
     * Open a file to write to.
     * @param fileName - name of file
     * @return         - BufferedWriter, pointer to filestream
     */
    public static BufferedWriter openWriteFile(String fileName) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(fileName));         
        } catch (IOException e) {
            System.out.println(e);
        }
        return writer;
    }
    

        
}
