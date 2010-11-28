#!/usr/bin/env python

import serial, sys, time

ser = serial.Serial(sys.argv[0], 115200)
ser.open()

ser.write('255 255 255 255\r\n')
time.sleep(1.0)
ser.write('0 0 0 0\r\n')
