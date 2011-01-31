#include <Servo.h>
#define MOTOR_MIN 68
#define MOTOR_MAX 172

Servo motor;

void setup() {
  long time = millis();
  motor.attach(11);
  
  while (millis() - time < 5000) {
    motor.write(20);
  }
}

void loop() {
  motor.write(100);
/*  
  time = millis();
  while (millis() - time < 4000) {
    motor.write(172); // MAX
  }
  
  time = millis();
  while (millis() - time < 8000) {
    motor.write(130);
  }
  */
}
