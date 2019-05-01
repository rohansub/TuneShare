
import webbrowser
from urllib.parse import quote
from credentials import *
import time

import requests
from getpass import getpass

import signal
import sys


interrupted = False

def sigint_handler(signal, frame):
    print("done")
    global interrupted
    interrupted = True

signal.signal(signal.SIGINT, sigint_handler)

from music_selector import *


import spotipy
import spotipy.util as util


import pygatt
from pygatt.util import uuid16_to_uuid
import json

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
            a = dev.char_read(uuid_music_data, timeout=3)
            data = json.loads(a.decode())
            selector.add_artists(mac_address, data)
            #dev.char_write(uuid_music_data, bytearray(b"done"), wait_for_response=False)
            dev.disconnect()
            print("Received data from", mac_address)
        else:
            print("Failed for", mac_address)
    except Exception as err:
        print(type(err))
        print(err.args)
        print(err)
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
        if b["name"] == "TuneShareDevice":
            print("try", b["address"])
            attempt_connection(scanner, b["address"])



#logging.basicConfig()
#logging.getLogger('pygatt').setLevel(logging.DEBUG)


username = "rohan"
scope = "user-read-currently-playing playlist-read-private playlist-modify-public"

token = util.prompt_for_user_token(username, scope, CLIENT_ID, CLIENT_SECRET, "http://localhost:8000/token")



selector = MusicSelector(token)


while not interrupted:
    print("Scanning...")
    scan()
    print("Done with scanning")
    selector.clear_expired()
    time.sleep(1)

del selector
