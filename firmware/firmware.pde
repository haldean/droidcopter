#define ARM_BUTTON_PIN 12
#define BAD_REQUEST "BADREQUEST"
#define HEARTBEAT_PULSE "PULSE"
#define MESSAGE_TIMEOUT_CYCLES 100
#define MOTOR_ARM_TIME 5000
#define MOTOR_ARM_VALUE 20
#define MOTOR_COUNT 4
#define MOTOR_MAX_COMMAND 160
#define MOTOR_MIN_COMMAND 67
#define MOTOR_OFF_COMMAND 50
#define SPEED_HEADER "NEWSPEED"
#define STATUS_LED 13
#define STATUS_LED_CYCLES 30000
#define WORD_BUFFER_LENGTH 4
#define WORD_LENGTH 2

#include <Servo.h>
#include <MeetAndroid.h>

unsigned int led_cycles = 0;
MeetAndroid bt;
bool enable_motors = true;

struct motor {
  unsigned char pin;
  unsigned int current_speed;
  unsigned int next_speed;
  Servo ctrl;
};

struct motor motors[MOTOR_COUNT] = {
  {6, 0, 0, Servo()}, {9, 0, 0, Servo()}, 
  {10, 0, 0, Servo()}, {11, 0, 0, Servo()}
};

/**
 *  Updates the speeds of the motors.
 */
void write_speeds(void) {
  for (int i = 0; i < MOTOR_COUNT; i++) {
    if (enable_motors) {
      motors[i].current_speed = motors[i].next_speed;
      motors[i].ctrl.write(motors[i].current_speed);
    } else {
      motors[i].ctrl.write(MOTOR_OFF_COMMAND);
    }
  }
}

/**
 *  Print the speeds of the motors to serial.
 */
void print_speeds(void) {
  for (int i = 0; i < MOTOR_COUNT; i++) {
    Serial.print(motors[i].current_speed);
    Serial.print(" ");
  }
  Serial.print("\n");
}

/**
 *  Initialize and arm motors
 */
void init_motors(void) {
  for (int i = 0; i < MOTOR_COUNT; i++) {
    motors[i].ctrl.attach(motors[i].pin);
    motors[i].next_speed = MOTOR_OFF_COMMAND;
  }

  long time = millis();
  while (millis() - time < MOTOR_ARM_TIME) {
    for (int i = 0; i < MOTOR_COUNT; i++) {
      motors[i].ctrl.write(MOTOR_ARM_VALUE);
    }
  }
  
  write_speeds();
}

/**
 *  Initialize messenger service and speed vector.
 */
void setup(void) {
  /* Set the serial status LED to output. */
  pinMode(STATUS_LED, OUTPUT);

  /* Initialize the motor controllers. */
  init_motors();

  Serial.begin(115200);
  char flag;
  for (flag='A'; flag<='D'; flag++) {
    bt.registerFunction(motor_message, flag);
  }
}

/**
 *  Read a message from Android.
 */
void motor_message(byte flag, byte numOfValues) {
  int i, motor_val;

  if (flag == 'A') i = 0;
  else if (flag == 'B') i = 1;
  else if (flag == 'C') i = 2;
  else if (flag == 'D') i = 3;
  
  motor_val = bt.getInt();
  
  if (motor_val) {
    motors[i].next_speed = map(motor_val, -1, 99, MOTOR_MIN_COMMAND, MOTOR_MAX_COMMAND);
  } else {
    motors[i].next_speed = MOTOR_OFF_COMMAND;
  }

  //print_speeds();
  Serial.println("done");
  led_cycles = 0;
}

/**
 *  Wait for serial information to come in.
 */
void loop(void) {
  enable_motors = digitalRead(ARM_BUTTON_PIN) == HIGH;
  bt.receive();

  write_speeds();

  if (enable_motors) {
    digitalWrite(STATUS_LED, HIGH);
  } else {
    digitalWrite(STATUS_LED, LOW);
  }
  
  #ifdef ENABLE_HEARTBEAT
  if (led_cycles % 10000 == 0) {
    bt.send(HEARTBEAT_PULSE);
  }
  #endif
}

