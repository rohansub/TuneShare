import pygatt
from pygatt.util import uuid16_to_uuid


import logging
import threading


uuid_music_data = uuid16_to_uuid(0x0)

def scan():
    ## Scan for devices
    scanner = pygatt.GATTToolBackend()
    scanner.start()
    ble_devices = scanner.scan(timeout = 5, run_as_root = True)
    scanner.stop()

    
    addresses = [b["address"] for b in ble_devices]
    ## TODO: attempt to connect to all the addresses

    # Test only!
    dev_d = {b["name"]:b["address"] for b in ble_devices}
    print(dev_d["Rohan Phone"])


    #### code for connecting to phone
    scanner.start(False)
    dev = scanner.connect(dev_d["Rohan Phone"], 5, pygatt.BLEAddressType.random)
    print("connected")
    dev.discover_characteristics()
    #dev.subscribe(uuid_music_data, indication=False)
    #print("subscribed")

    print(dev.char_read(uuid_music_data))
    scanner.stop()
#logging.basicConfig()
#logging.getLogger('pygatt').setLevel(logging.DEBUG)
    
scan()
