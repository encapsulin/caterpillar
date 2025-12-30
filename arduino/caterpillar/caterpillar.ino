/*
  https://docs.arduino.cc/built-in-examples/basics/Blink/
*/

#define AIN1 7
#define AIN2 8
#define APWM 9
int AsensorValue = 300;  // value read from the pot
int AoutputValue = 0;

#define BIN1 2
#define BIN2 4
#define BPWM 3
int BsensorValue = 300;  // value read from the pot
int BoutputValue = 0;
// the setup function runs once when you press reset or power the board
void setup() {
  pinMode(LED_BUILTIN, OUTPUT);

  pinMode(AIN1, OUTPUT);
  digitalWrite(AIN1, HIGH);
  pinMode(AIN2, OUTPUT);
  digitalWrite(AIN2, LOW);
  AoutputValue = map(AsensorValue, 0, 1023, 0, 255);
  analogWrite(APWM, AoutputValue);

  pinMode(BIN1, OUTPUT);
  digitalWrite(BIN1, HIGH);
  pinMode(BIN2, OUTPUT);
  digitalWrite(BIN2, LOW);
  BoutputValue = map(BsensorValue, 0, 1023, 0, 255);
  analogWrite(BPWM, BoutputValue);
}

// the loop function runs over and over again forever
void loop() {
  digitalWrite(LED_BUILTIN, HIGH);  // turn the LED on (HIGH is the voltage level)
  delay(10);                       // wait for a second
  digitalWrite(LED_BUILTIN, LOW);   // turn the LED off by making the voltage LOW
  delay(1000);                      // wait for a second
}
