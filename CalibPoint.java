package calibration;  

/**
 * Class to hold a calibration data point.
 */
public class CalibPoint {

    double timeStamp;
    int    leftEncoder;
    int    rightEncoder;
    double leftVelocity;
    double rightVelocity;
    
    /*
     * Primary class constructor.
     */
    public CalibPoint(double timeStamp, int leftEncoder, int rightEncoder, double leftVelocity, double rightVelocity) {
        this.timeStamp     = timeStamp;
        this.leftEncoder   = leftEncoder;
        this.rightEncoder  = rightEncoder;
        this.leftVelocity  = leftVelocity;
        this.rightVelocity = rightVelocity;
    }        
}
