import pygatt
from pygatt.util import uuid16_to_uuid


import logging
import threading
import time

uuid_music_data = uuid16_to_uuid(0x0)

def attempt_connection(scanner, mac_address):
    scanner.start(False)
    try:
        dev = scanner.connect(mac_address, 5, pygatt.BLEAddressType.random)
        if uuid_music_data in dev.discover_characteristics():
            dev.exchange_mtu(512)
            s = dev.char_read(uuid_music_data)
            print(s)
            print("Received data from", mac_address)
        else:
            print("Failed for", mac_address)
    except:
        print("Failed for", mac_address)
    scanner.stop()    


def scan():
    ## Scan for devices
    scanner = pygatt.GATTToolBackend()
    scanner.start()
    ble_devices = scanner.scan(timeout = 5, run_as_root = True)
    scanner.stop()

    
    # attempt connection for all addresses found!
    for b in ble_devices:
        dev = attempt_connection(scanner, b["address"])


#logging.basicConfig()
#logging.getLogger('pygatt').setLevel(logging.DEBUG)
    
while True:
    print("Scanning...")
    scan()
    print("Done with scanning")
    time.sleep(1)
