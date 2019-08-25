#include "UltrasonicSensor.h"

#include "Arduino.h"

UltrasonicSensor::UltrasonicSensor(int trigPin, int echoPin) {
  UltrasonicSensor::trigPin = trigPin;
  UltrasonicSensor::echoPin = echoPin;

  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);
}

int UltrasonicSensor::getEchoPinPulseDuration() {
    digitalWrite(trigPin, LOW);  // Added this line
    delayMicroseconds(2); // Added this line
    digitalWrite(trigPin, HIGH);
    delayMicroseconds(10); // Added this line
    digitalWrite(trigPin, LOW);

    return pulseIn(echoPin, HIGH);
}

int UltrasonicSensor::getObjectDistanceCM() {
    int distanceCM = (getEchoPinPulseDuration() / 2) / 29.1;

    if (distanceCM >= MAX_DISTANCE_DETECTABLE) {
      return -1; 
    }
    return distanceCM;
}
