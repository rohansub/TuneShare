import pygatt
import logging
import threading

def scan():
    ## Scan for devices
    scanner = pygatt.GATTToolBackend()
    scanner.start()
    ble_devices = scanner.scan(timeout = 5, run_as_root = True)
    scanner.stop()

    
    addresses = [b["address"] for b in ble_devices]
    ## TODO: attempt to connect to all the addresses

    
    
#logging.basicConfig()
#logging.getLogger('pygatt').setLevel(logging.DEBUG)
    
scan()
