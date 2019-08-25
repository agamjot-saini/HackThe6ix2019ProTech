#ifndef BluetoothConnection_h
#define BluetoothConnection_h

#include "BluetoothSerial.h"

class BluetoothConnection {
public:
  BluetoothConnection(String bluetoothDeviceName);

  void performConnectionHandshake();

  void writeCharacter(char characterToWrite);
  void writeString(String dataToWrite);
  char readCharacter(); // Non-blocking

private:
  BluetoothSerial bluetoothSerial;
};

#endif
