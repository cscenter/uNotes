package conversions.peaks;

public class Peak {
    public double power;
    public double powerRel;
    public double center;
    public double width;
    public double leftBorder;
    public double rightBorder;

    public Peak(double pow, double powNoise, double center, double width) {
        this.power = pow;
        this.powerRel = pow - powNoise;
        if (this.powerRel < 1.0e-9) {
            this.powerRel = 1.0e-9;
        }
        this.center = center;
        this.width = width;
        this.leftBorder = center - width / 2;
        this.rightBorder = center + width / 2;
    }

    public Peak(double pow, double leftBorder, double rightBorder) {
        this.power = pow;
        this.powerRel = pow;
        this.center = (rightBorder + leftBorder) / 2;
        this.width = rightBorder - leftBorder;
        this.leftBorder = leftBorder;
        this.rightBorder = rightBorder;
    }
}
