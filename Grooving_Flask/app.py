import os
import pymongo
import base64
from gridfs import GridFS
from bson.json_util import dumps, loads
from werkzeug.security import check_password_hash, generate_password_hash

from flask import Flask, jsonify, request, Response, redirect, session
from pymongo import MongoClient


def create_app(test_config=None):
    # create and configure the app
    app = Flask(__name__, instance_relative_config=True)
    app.config.from_mapping(
        SECRET_KEY='dev',
        DATABASE='mongodb://localhost:27017'
    )

    if test_config is None:

        app.config.from_pyfile('config.py', silent=True)
    else:

        app.config.from_mapping(test_config)

    try:
        os.makedirs(app.instance_path)
    except OSError:
        pass

    def get_db():
        client = MongoClient(app.config['DATABASE'])
        db = client.grooving
        return db

    @app.route('/')
    def home():
        return jsonify({'status': 'Welcome'})

    @app.route('/register', methods=('POST', 'GET'))  # Uso questo collegamento per registrare un utente
    def register():
        if request.method == 'POST':
            email = request.form['email']  # Richiedo i dati dal client per effettuare una registrazione e li controllo
            password = request.form['password']
            username = request.form['username']
            if not email:
                return jsonify({'error': 'Inserisci un indirizzo e-mail'})

            if not password:
                return jsonify({'error': 'Inserisci una password'})

            if not username:
                return jsonify({'error': 'Inserisci un username'})

            if '@' not in email:
                return jsonify({'error': 'Inserisci un indirizzo e-mail valido'})
            db = get_db()
            gia_inserito = db.utenti.find_one({'email': email})

            if gia_inserito is not None:
                return jsonify({'error': "Utente gia' registrato"})

            nickname_esistente= db.utenti.find_one({'username': username})

            if nickname_esistente is not None:
                return jsonify({'error': 'Nickname esistente'})

            db.utenti.insert({'email': email, 'password': generate_password_hash(password), 'username': username})  #Inserisco i dati nel db

            return jsonify({'status': 'Utente aggiunto'})
        return jsonify({'error': 'Hai richiamato un metodo GET'})

    @app.route('/login', methods=('POST', 'GET'))  # Uso questo collegamento per effettuare il login
    def login():
        if request.method == 'POST':  # Controllo i dati inseriti dal client
            inserted_email = request.form['email']
            inserted_pass = request.form['password']

            if not inserted_email:
                return jsonify({'error': 'Inserisci un indirizzo e-mail'})

            if not inserted_pass:
                return jsonify({'error': 'Inserisci una password'})

            db = get_db()
            query_utente = db.utenti.find_one({'email': inserted_email})
            if query_utente is None:
                return jsonify({'error': 'Utente non registrato'})
            else:
                query_pass = db.utenti.find_one({'email': inserted_email})
                if not check_password_hash(query_pass['password'], inserted_pass):
                    return jsonify({'error': 'Password inserita errata'})
                else:
                    return jsonify({'status': 'Login effettuato con successo'})

        return jsonify({'error': 'Hai richiamato un metodo GET'})

    @app.route('/logout', methods=('POST', 'GET'))  # Uso questo collegamento per effettuare il logout (Da migliorare con il lato client)
    def logout():
        if request.method == 'POST':
            inserted_email = request.form['email']
            db = get_db()
            query = db.utenti.find_one({'email': inserted_email})
            db.logged_in.remove({'_id': query['_id']})
            return jsonify({'status': 'Disconnesso'})
        return jsonify({'error': 'Hai richiamato un metodo GET'})

    @app.route('/upload', methods=('POST', 'GET'))  # Uso questo collegamento per uploadare un file
    def upload():
        if request.method == 'POST':
            inserted_email = request.form['email']  #Ricevo i dati dal client e li controllo
            inserted_name = request.form['groove_name']
            inserted_category = request.form['category']
            inserted_groove = request.form['groove']
            inserted_instrument = request.form['instrument']

            if not inserted_name:
                return jsonify({'error': 'Inserisci un nome al tuo groove'})
            if not inserted_category:
                return jsonify({'error': 'Inserisci un genere per il tuo groove'})
            if not inserted_instrument:
                return jsonify({'error': 'Inserisci almeno uno strumento per il tuo groove'})

            db = get_db()
            query_utente = db.utenti.find_one({'email': inserted_email})

            if query_utente is None:
                return jsonify({'error': 'Utente non esistente'})

            username_utente = query_utente['username']
            query = db.grooves.files.find_one({'groove_name': inserted_name, 'author': username_utente})
            if query is not None:
                return jsonify({'error': "Groove gia' esistente"})

            audio_file = base64.b64decode(inserted_groove)  #Se è tutto a posto decodifico la stringa ricevuta e la inserisco nel DB
            grid_fs = GridFS(db, collection='grooves')
            grid_fs.put(audio_file, author=username_utente,
                        groove_name=inserted_name,
                        category=inserted_category,
                        instruments=inserted_instrument)
            return jsonify({'status': 'Groove registrato con successo'})
        return jsonify({'error': 'Hai richiamato un metodo GET'})

    @app.route('/wall', methods=['GET'])  #Uso questa funzione per recuperare i file pubblicati
    def wall():
        grooves = []
        db = get_db()
        query = db.grooves.files.find({}).sort('uploadDate', pymongo.DESCENDING)
        for q in query:
            grooves.append({'Nome_groove': q['groove_name'],
                            'Autore': q['author'],
                            'Categoria': q['category'],
                            'UploadDate': q['uploadDate'],
                            'Strumenti': q['instruments']})
        return jsonify({'results': grooves})

    @app.route('/requeststream', methods=['POST'])  #Restituisco il file come json
    def stream():
        inserted_author = request.form['author_groove']
        inserted_groove_name = request.form['groove_name']
        db = get_db()
        groove = db.grooves.files.find_one({'author': inserted_author, 'groove_name': inserted_groove_name})
        grid_fs = GridFS(db, collection='grooves')
        data = grid_fs.get(groove['_id']).read()
        string_audio = dumps(data)
        return Response(string_audio, mimetype='application/json')

    @app.route('/my_grooves', methods=['POST'])  #Utilizzo questo indirizzo per visualizzare i groove inseriti solo dall'utente corrente
    def my_grooves():
        inserted_email = request.form['email']
        grooves = []
        db = get_db()
        search_user = db.utenti.find_one({'email': inserted_email})
        query = db.grooves.files.find({'author': search_user['username']}).sort('uploadDate', pymongo.DESCENDING)
        for q in query:
            grooves.append({'Nome_groove': q['groove_name'],
                            'Autore': q['author'],
                            'Categoria': q['category'],
                            'UploadDate': q['uploadDate'],
                            'Strumenti': q['instruments']})
        return jsonify({'results': grooves})

    return app
