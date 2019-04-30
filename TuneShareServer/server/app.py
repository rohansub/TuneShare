from flask import Flask
app = Flask(__name__)

token = ""

@app.route('/')
def hello():
    return "Hello World!"

@app.route('/login')
def login():
    scopes = ['playlist-modify-public']
    return redirect(, code=302)


if __name__ == '__main__':
    app.run()

    