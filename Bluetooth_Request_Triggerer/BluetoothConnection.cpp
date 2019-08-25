#include "BluetoothConnection.h"

BluetoothConnection::BluetoothConnection(String bluetoothDeviceName) {
  //bluetoothSerial.begin(bluetoothDeviceName);
  bluetoothSerial.begin("ESP32");
}

void BluetoothConnection::performConnectionHandshake() {
  Serial.println("Waiting for availability");
  
  // Wait for the client to send a message indicating that it has paired with the ESP32
  while (!bluetoothSerial.available()) {}

  Serial.println("Got response:");

  // Read the whole response
  String response = "";
  while (bluetoothSerial.available()) {
    response += (char) bluetoothSerial.read();
  }

  Serial.println(response);
}

void BluetoothConnection::writeCharacter(char characterToWrite) {
  bluetoothSerial.write(characterToWrite);
}

void BluetoothConnection::writeString(String dataToWrite) {
  for (int i = 0; i < dataToWrite.length(); i++) {
    bluetoothSerial.write(dataToWrite.charAt(i));
  }
}

char BluetoothConnection::readCharacter() {
  if (bluetoothSerial.available()) {
    return bluetoothSerial.read();
  }
  return -1;
}
