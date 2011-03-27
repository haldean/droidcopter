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

char buffer[WORD_BUFFER_LENGTH];
unsigned int led_cycles = 0;

/**
 *  Updates the speeds of the motors.
 */
void write_speeds(void) {
  for (int i = 0; i < MOTOR_COUNT; i++) {
    motors[i].current_speed = motors[i].next_speed;
    motors[i].ctrl.write(motors[i].current_speed);
  }
}

/**
 *  Print the speeds of the motors to serial.
 */
void print_speeds(void) {
  Serial.print(SPEED_HEADER);
  for (int i = 0; i < MOTOR_COUNT; i++) {
    Serial.print(":");
    Serial.print(motors[i].current_speed);
  }
  Serial.println();
}

/**
 *  Initialize and arm motors
 */
void init_motors(void) {
  for (int i = 0; i < MOTOR_COUNT; i++) {
    motors[i].ctrl.attach(motors[i].pin);
  }

  long time = millis();
  while (millis() - time < MOTOR_ARM_TIME) {
    for (int i = 0; i < MOTOR_COUNT; i++) {
      motors[i].ctrl.write(MOTOR_ARM_VALUE);
    }
  }
}

/**
 *  Initialize messenger service and speed vector.
 */
void setup(void) {
  /* Initialize serial monitor. */
  Serial.begin(115200);
  
  /* Set the serial status LED to output. */
  pinMode(STATUS_LED, OUTPUT);

  /* Initialize the motor controllers. */
  init_motors();

  Serial.println("BEGIN");
}

/**
 *  Wait for serial information to come in.
 */
void loop(void) {
  if (Serial.available() >= WORD_LENGTH) {
    int i, j, timeout, buffer_val;
    led_cycles = 0;
    digitalWrite(STATUS_LED, HIGH);

    for (i=0; i<MOTOR_COUNT; i++) {
      for (timeout=0; timeout < MESSAGE_TIMEOUT_CYCLES; timeout++) {
        if (Serial.available() >= WORD_LENGTH) break;
      }
      
      if (Serial.available() >= WORD_LENGTH) {
        for (j=0; j<WORD_LENGTH; j++) {
          buffer[j] = Serial.read();
        }
        
        buffer[WORD_LENGTH] = '\0';
        buffer_val = atoi(buffer);
        if (buffer_val > 0) {
          motors[i].next_speed = map(atoi(buffer), -1, 99, 
            MOTOR_MIN_COMMAND, MOTOR_MAX_COMMAND);
        } else {
          motors[i].next_speed = MOTOR_OFF_COMMAND;
        }
      } else {
        Serial.println(BAD_REQUEST);
      }
    }
      
    /* Flush serial buffer after reading a line. */
    while (Serial.available()) {
      Serial.print("");
      Serial.read();
    }
    
    write_speeds();
    print_speeds();
  }

  led_cycles++;
  if (led_cycles >= STATUS_LED_CYCLES) {
    digitalWrite(STATUS_LED, LOW);
  }
  
  #ifdef ENABLE_HEARTBEAT
  if (led_cycles % 10000 == 0) {
    Serial.println(HEARTBEAT_PULSE);
  }
  #endif
  
}

