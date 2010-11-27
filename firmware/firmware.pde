#include <Messenger.h>

#define STATUS_LED 13
#define MOTOR_COUNT 4
#define STATUS_LED_CYCLES 30000
#define DEBUG_RESPONSE 1

Messenger serial = Messenger();
unsigned int led_cycles = 0;

struct motor {
  unsigned char pin;
  unsigned int current_speed;
  unsigned int next_speed;
};

struct motor motors[MOTOR_COUNT] = {
  {6, 0, 0}, {9, 0, 0}, {10, 0, 0}, {11, 0, 0}
};

/**
 *  Updates the speeds of the motors.
 */
void write_speeds(void) {
  for (int i = 0; i < MOTOR_COUNT; i++) {
    motors[i].current_speed = motors[i].next_speed;
    analogWrite(motors[i].pin, motors[i].current_speed);
  }
}

void print_speeds(void) {
  Serial.print("New speeds: ");
  for (int i = 0; i < MOTOR_COUNT; i++) {
    Serial.print(motors[i].current_speed);
    Serial.print(' ');
  }
  Serial.println();
}

/**
 *  Handle a message from the controller. Sets the speeds then
 *  requests a speed update.
 */
void handle_message(void) {
  for (int i = 0; i < MOTOR_COUNT; i++) {
    if (! serial.available()) {
      Serial.println("You didn't send me a correctly formatted message.");
#ifdef DEBUG_RESPONSE
      print_speeds();
#endif
      return;
    } else {
      motors[i].next_speed = serial.readInt() % 256;
    }
  }

  write_speeds();
#ifdef DEBUG_RESPONSE
  print_speeds();
#endif
}

/**
 *  Initialize messenger service and speed vector.
 */
void setup(void) {
  /* Set the serial status LED to output. */
  pinMode(STATUS_LED, OUTPUT);

  /* Initialize the motor controllers. */
  for (int i = 0; i < MOTOR_COUNT; i++) {
    pinMode(motors[i].pin, OUTPUT);
    analogWrite(motors[i].pin, 0);
  }

  /* Initialize serial monitor. */
  Serial.begin(115200);
  serial.attach(handle_message);
}

/**
 *  Wait for serial information to come in.
 */
void loop(void) {
  while (Serial.available() > 0) {
    led_cycles = 0;
    digitalWrite(STATUS_LED, HIGH);

    serial.process(Serial.read());
  }

  led_cycles++;
  if (led_cycles >= STATUS_LED_CYCLES) {
    digitalWrite(STATUS_LED, LOW);
  }
}
