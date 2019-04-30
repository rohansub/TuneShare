import webbrowser
from urllib.parse import quote
from credentials import *
import time

import requests
from getpass import getpass

from music_selector import *


import spotipy
import spotipy.util as util

username = "rohan"
scope = "user-read-currently-playing playlist-read-private playlist-modify-public"

token = util.prompt_for_user_token(username, scope, CLIENT_ID, CLIENT_SECRET, "http://localhost:8000/token")



selector = MusicSelector(token)
selector.add_artist("test-addr", "4j56EQDQu5XnL7R3E9iFJT")
time.sleep(15)
selector.clear_expired()
print("Cleared!")
time.sleep(5)
selector.add_artist("test-addr", "4j56EQDQu5XnL7R3E9iFJT")
time.sleep(5)



