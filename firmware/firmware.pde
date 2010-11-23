#include <Messenger.h>
#define STATUS_LED 13
#define MOTOR_COUNT 4

Messenger serial = Messenger();
int speeds[MOTOR_COUNT];

/**
 *  Updates the speeds of the motors.
 */
void write_speeds(void) {
  void();
}

/**
 *  Handle a message from the controller. Sets the speeds then
 *  requests a speed update.
 */
void handle_message(void) {
  for (int i = 0; i < MOTOR_COUNT; i++) {
    if (! serial.available()) {
      Serial.println("You didn't send me a correctly formatted message.");
      break;
    }

    speeds[i] = serial.readInt();
  }
}

/**
 *  Initialize messenger service and speed vector.
 */
void setup(void) {
  /* Set the serial status LED to output. */
  pinMode(STATUS_LED, OUTPUT);

  /* Initialize all speeds to zero. */
  for (int i = 0; i < MOTOR_COUNT; i++) speeds[i] = 0;

  /* Initialize serial monitor. */
  Serial.begin(9600);
  serial.attach(handle_message);
}

/**
 *  Wait for serial information to come in.
 */
void loop(void) {
  while (Serial.available() > 0) {
    digitalWrite(STATUS_LED, HIGH);
    serial.process(Serial.read());
  }
  digitalWrite(STATUS_LED, LOW);
}
