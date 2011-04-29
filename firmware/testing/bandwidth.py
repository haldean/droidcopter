#!/usr/bin/env python

import random, serial, time

def test(flags, msgs, port='/dev/ttyUSB0'):
    s = serial.Serial(port)
    
    def send_speeds():
        for f in flags:
            msg = "%s%d\x13" % (f, random.randint(0,100))
            s.write(msg)

    t = time.time()
    [send_speeds() for i in range(msgs)]
    t = time.time() - t 
    print "%f seconds for %d messages (average: %f)" % (t, msgs, t/msgs)
    
