from datetime import datetime
from collections import defaultdict

import spotipy
import spotipy.util as util

from urllib.parse import quote
from credentials import *


PLAYLIST_NAME = "TuneShare Playlist"

EXPIRATION_TIME = 60 * 60 # 1 hour


class MusicSelector:

    def __init__(self, token):
        self.sp = spotipy.Spotify(token)
        
        # key - mac address, value - timestamp
        self.macAddresses = {}

        # key - artist, value - timestamp
        self.artists = {}

        # key - artist, value - set of song ids
        self.artistSongs = defaultdict(list)

        self.userId = self.sp.current_user()["id"]
        self.playlistId = self.__get_playlist_id()
    
    def __del__(self):
        pairs = [(k, v) for k,v in self.artists.items()]
        for k, v in pairs:
            del self.artists[k]
            self.sp.user_playlist_remove_all_occurrences_of_tracks(
                self.userId,
                self.playlistId,
                self.artistSongs[k]
            )
            del self.artistSongs[k]

    def add_artists(self, addr, artistIds):
        # Don't include preference if this address has been
        # considered already
        if addr in self.macAddresses:
            return
        self.macAddresses[addr] = datetime.now()
        # if this artist has already been added, don't add 
        # again, but update timestamp
        songs = []
        for artistId in artistIds:
            isOld = artistId in self.artists
            self.artists[artistId] = datetime.now()
            if isOld:
                continue

            self.artists[artistId] = datetime.now()
            tracks = self.sp.artist_top_tracks(artistId)
            for t in tracks["tracks"]:
                self.artistSongs[artistId].append(t["id"])
                songs.append(t["id"])
        if len(songs) > 0:
            self.sp.user_playlist_add_tracks(
                self.userId, 
                self.playlistId, 
                songs
            )
            print("Sent:", songs)
        

    def __get_playlist_id(self):
        # check if playlist exists
        playlists = self.sp.current_user_playlists()
        for p in playlists['items']:
            if p['owner']['id'] == self.userId and p['name'] == PLAYLIST_NAME:
                # return, since playlist is already there
                return p['id']

        # create the playlist
        plt = self.sp.user_playlist_create(self.userId, PLAYLIST_NAME)
        return plt["id"]


    def clear_expired(self):
        # clear out expired mac address 
        now = datetime.now()
        for k, v in self.macAddresses.items():
            dlt = now - v
            if dlt.total_seconds() > EXPIRATION_TIME:
                del self.macAddresses[k]
        
        # clear out expired artists
        pairs = [(k, v) for k, v in self.artists.items()]
        for k, v in pairs:
            dlt = now - v
            if dlt.total_seconds() > EXPIRATION_TIME:
                del self.artists[k]
                self.sp.user_playlist_remove_all_occurrences_of_tracks(
                    self.userId,
                    self.playlistId,
                    self.artistSongs[k]
                )
                del self.artistSongs[k]


