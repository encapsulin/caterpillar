/*
  https://docs.arduino.cc/built-in-examples/basics/Blink/
*/
#include <SoftwareSerial.h>
// const byte rxPin = 8;
// const byte txPin = 9;
// SoftwareSerial mySerial (rxPin, txPin);
#define rxPin 8
#define txPin 9
SoftwareSerial mySerial = SoftwareSerial(rxPin, txPin);

#define AIN1 7
#define AIN2 5
#define APWM 6
// int AsensorValue = 300;  // value read from the pot
// int AoutputValue = 0;

#define BIN1 2
#define BIN2 4
#define BPWM 3
// int BsensorValue = 300;  // value read from the pot
// int BoutputValue = 0;

int powergo = 0;

void setup() {
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.begin(9600);

  pinMode(AIN1, OUTPUT);
  pinMode(AIN2, OUTPUT);

  pinMode(BIN1, OUTPUT);
  pinMode(BIN2, OUTPUT);
  // BoutputValue = map(BsensorValue, 0, 1023, 0, 255);
  // analogWrite(BPWM, BoutputValue);
  setDirection('f');
  setPowerA('f');

  pinMode(rxPin, INPUT);
  pinMode(txPin, OUTPUT);
  mySerial.begin(9600);
}

// the loop function runs over and over again forever
void loop() {
  digitalWrite(LED_BUILTIN, HIGH);  // turn the LED on (HIGH is the voltage level)
  delay(10);                        // wait for a second
  digitalWrite(LED_BUILTIN, LOW);   // turn the LED off by making the voltage LOW
  delay(100);

  String sin = "";
  if (Serial.available()) {
    sin = serial_read();
    mySerial.print(sin);
  }

  if (mySerial.available() > 0) {
    sin = myserial_read();
    Serial.print(sin);
  }

  serial_process(sin);
  process_powergo();
}

String serial_read() {
  String cmd = "";
  cmd = Serial.readString();
  return cmd;
}


String myserial_read() {
  String cmd = "";
  while (mySerial.available() > 0) {
    char c = mySerial.read();
    cmd += c;
  }
  return cmd;
}

void serial_process(String sin) {

  if (sin.length() >= 2) {
    // Serial.println("serial_process()" + sin);
    // Serial.println(sin.length());
    char cmd = sin.charAt(0);
    char cval = sin.charAt(1);
    char allowedPower[] = "PpSs";
    if (strchr(allowedPower, cmd)) {  //Power
      setPowerA(cval);
    }
    if (cmd == 'D' || cmd == 'd') {  //Power
      setDirection(cval);
    }
  }
}

void setPowerA(char c) {
  Serial.print("pow:");
  Serial.println(c);
  if (c >= 'a' && c <= 'k') {
    int value = c - 'a';  // 'a'→0, 'b'→1, ..., 'k'→10
    int AoutputValue = map(value, 0, 10, 0, 255);
    analogWrite(APWM, AoutputValue);
    analogWrite(BPWM, AoutputValue);
    Serial.println(AoutputValue);
  }
}

void setDirection(char c) {
  Serial.print("dir:");
  Serial.println(c);
  if (c == 'f') {
    digitalWrite(AIN1, HIGH);
    digitalWrite(AIN2, LOW);
    digitalWrite(BIN1, HIGH);
    digitalWrite(BIN2, LOW);
  } else if (c == 'b') {
    digitalWrite(AIN2, HIGH);
    digitalWrite(AIN1, LOW);
    digitalWrite(BIN2, HIGH);
    digitalWrite(BIN1, LOW);
  } else if (c == 'l') {
    digitalWrite(AIN2, HIGH);
    digitalWrite(AIN1, LOW);
    digitalWrite(BIN1, HIGH);
    digitalWrite(BIN2, LOW);
  } else if (c == 'r') {
    digitalWrite(AIN1, HIGH);
    digitalWrite(AIN2, LOW);
    digitalWrite(BIN2, HIGH);
    digitalWrite(BIN1, LOW);
  }
  powergo = 3;
}

void process_powergo() {
  if (powergo > 0)
    powergo--;
  else if (powergo == 0) {
    digitalWrite(AIN1, LOW);
    digitalWrite(AIN2, LOW);
    digitalWrite(BIN2, LOW);
    digitalWrite(BIN1, LOW);
  }
}