#define NUMBER_OF_PROXIMITY_DETECTIONS_THRESHOLD 5

bool currentProximityFlag = false, previousProximityFlag = false;
int numberOfDetections = 0;

void setup() {
  Serial.begin(9600);
  
  initBluetooth();
  initUltrasonic();
}

void loop() {
  currentProximityFlag = isObjectInProximity();
  
  /*if (currentProximityFlag && !previousProximityFlag) {
    numberOfDetections++;
  }
  else {
    numberOfDetections = 0;
    previousProximityFlag = currentProximityFlag;
  }

  if (numberOfDetections > NUMBER_OF_PROXIMITY_DETECTIONS_THRESHOLD) {
    sendEventTriggerViaBluetooth();
    numberOfDetections = 0;
    previousProximityFlag = currentProximityFlag;
  }*/

  if (currentProximityFlag) {
    sendEventTriggerViaBluetooth();
    delay(1000);
  }
  
  delay(50);
}

// Using the HC-SR04 ultrasonic sensor to detect if there is an object/person crossing the window
//************************************************************
#include "UltrasonicSensor.h"

#define ULTRASONIC_TRIG_PIN 25
#define ULTRASONIC_ECHO_PIN 26

UltrasonicSensor* sensor;
long objectDistanceCM;

void initUltrasonic() {
  sensor = new UltrasonicSensor(ULTRASONIC_TRIG_PIN, ULTRASONIC_ECHO_PIN);
}

bool isObjectInProximity() {
  objectDistanceCM = sensor->getObjectDistanceCM();
  //if (objectDistanceCM != -1) {
    Serial.println(objectDistanceCM);
  //}
  //else {
    //Serial.println("Out of range.");
  //}
  return objectDistanceCM != -1;
}
//************************************************************

// Using the Bluetooth functionality in the ESP32, trigger the smartphone/camera to start recording video
//************************************************************
#include "BluetoothConnection.h"

BluetoothConnection* bluetoothConnection;

void initBluetooth() {
  bluetoothConnection = new BluetoothConnection("ESP32");
  bluetoothConnection->performConnectionHandshake();
}


void sendEventTriggerViaBluetooth() {
  bluetoothConnection->writeString("EVENT DETECTED\r\n");
}
//************************************************************
