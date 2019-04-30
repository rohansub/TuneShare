from flask import Flask, redirect, request, make_response, g

app = Flask(__name__)

with app.app_context():
    token = "Test"


from urllib.parse import quote
from credentials import *


@app.route('/')
def login():
    print(token)
    redirect_uri = quote("http://localhost:8000/token")
    url = 'https://accounts.spotify.com/authorize?response_type=code' + '&client_id=' + CLIENT_ID +'&scope=playlist-modify-public''&redirect_uri=' + redirect_uri
    return redirect(url, code=302)

@app.route('/token')
def login_response():
    token = request.args.get('code')
    resp = make_response("Finshed Login! You may close the browser")
    # g.token = token
    return resp
    

if __name__ == '__main__':
    app.run(port=8000)
        

    