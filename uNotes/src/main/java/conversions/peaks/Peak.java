package conversions;

public class Peak{
    public double power;
    public double powerRel;
    public double center;
    public double width;

    public Peak(double pow, double powNoise, double center, double width){
        this.power = pow;
        this.powerRel = pow - powNoise;
        this.center = center;
        this.width = width;
    }
}
