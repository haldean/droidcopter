#include <Servo.h>

// Motors
#define PIN_FRONT 11
#define PIN_REAR 9
#define PIN_LEFT 10
#define PIN_RIGHT 6
#define MINCOMMAND 0

struct Motors {
  int command[4];
  int axisCommand[3];
  boolean armed;
  int minimum;
} motor = {
  { MINCOMMAND, MINCOMMAND, MINCOMMAND, MINCOMMAND },
  { 0, 0, 0 },
  false,
  1000
};

Servo motorFront;
Servo motorRear;
Servo motorLeft;
Servo motorRight;

void setupMotors() {
  motorFront.attach(PIN_FRONT);
  motorRear.attach(PIN_REAR);
  motorLeft.attach(PIN_LEFT);
  motorRight.attach(PIN_RIGHT);

  // Arm ESCs
  setAllMotors(MINCOMMAND);
  updateMotors();
}

void setup() {
  setupMotors();
}

void loop() {;}

void setAllMotors(int cmd) {
  int i;
  for (i=0; i<4; i++) {
    motor.command[i] = cmd;
  }
}

void updateMotors() {
  int i;
  for (i=0; i<4; i++) {
    motorFront.write(motor.command[i]);
  }
}

