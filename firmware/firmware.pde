#define BAD_REQUEST "BADREQUEST"
#define HEARTBEAT_PULSE "PULSE"
#define MESSAGE_TIMEOUT_CYCLES 100
#define MOTOR_COUNT 4
#define SPEED_HEADER "NEWSPEED"
#define STATUS_LED 13
#define STATUS_LED_CYCLES 30000
#define WORD_BUFFER_LENGTH 4
#define WORD_LENGTH 3

struct motor {
  unsigned char pin;
  unsigned int current_speed;
  unsigned int next_speed;
};

struct motor motors[MOTOR_COUNT] = {
  {6, 0, 0}, {9, 0, 0}, {10, 0, 0}, {11, 0, 0}
};

char buffer[WORD_BUFFER_LENGTH];
unsigned int led_cycles = 0;


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
  Serial.print(SPEED_HEADER);
  for (int i = 0; i < MOTOR_COUNT; i++) {
    Serial.print(":"
    );
    Serial.print(motors[i].current_speed);
  }
  Serial.println();
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
}

/**
 *  Wait for serial information to come in.
 */
void loop(void) {
  if (Serial.available() >= WORD_LENGTH) {
    int i, j, timeout;
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
        motors[i].next_speed = atoi(buffer);
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
  
  if (led_cycles % 10000 == 0) {
   // Serial.println(HEARTBEAT_PULSE);
  }
  
}

