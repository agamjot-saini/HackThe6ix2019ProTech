#ifndef ULTRASONIC_SENSOR_h
#define ULTRASONIC_SENSOR_h

class UltrasonicSensor {
  private:
    int trigPin = -1, echoPin = -1;
    static const int MAX_DISTANCE_DETECTABLE = 85;

    int getEchoPinPulseDuration();

  public:
    UltrasonicSensor(int trigPin, int echoPin);
    int getObjectDistanceCM(); 
};

#endif
