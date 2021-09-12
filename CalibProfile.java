package calibration;  

import java.util.ArrayList;
import java.util.List;

/**
 * Class to hold a calibration data point.
 */
public class CalibProfile {

    List<CalibPoint> calibPoints;
    double           nominalPower;
    double           accelerationThrottle;
    double           batteryVoltage;
    String           sequence;
    double           leftMeasure1;
    double           rightMeasure1;
    double           leftMeasure2;
    double           rightMeasure2;
    List<Double>     linearRegressionParameters;
    
    public CalibProfile(List<CalibPoint> calibPoints, double nominalPower, double batteryVoltage) {
        this(calibPoints, nominalPower, 1.0, batteryVoltage, "LR", 0.0, 0.0, 0.0, 0.0);
    }
    
    public CalibProfile(List<CalibPoint> calibPoints, double nominalPower, double batteryVoltage, double leftMeasure1, double rightMeasure1) {
        this(calibPoints, nominalPower, 1.0, batteryVoltage, "LR", leftMeasure1, rightMeasure1, 0.0, 0.0);
    }
    
    /*
     * Primary class constructor.
     */
    public CalibProfile(List<CalibPoint> calibPoints, double nominalPower, double accelerationThrottle, double batteryVoltage, String sequence,
                        double leftMeasure1, double rightMeasure1, double leftMeasure2, double rightMeasure2) {
        this.calibPoints    = new ArrayList<>();
        this.calibPoints.addAll(calibPoints);
        this.nominalPower   = nominalPower;
        this.accelerationThrottle = accelerationThrottle;
        this.batteryVoltage = batteryVoltage;
        this.sequence       = sequence;
        this.leftMeasure1   = leftMeasure1;
        this.rightMeasure1  = rightMeasure1;
        this.leftMeasure2   = leftMeasure2;
        this.rightMeasure2  = rightMeasure2;
        this.linearRegressionParameters = new ArrayList<>();
    }        
}
