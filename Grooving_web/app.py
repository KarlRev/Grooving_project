import os
import pymongo
from bson import ObjectId
from gridfs import GridFS
from werkzeug.security import check_password_hash, generate_password_hash

from flask import Flask, request, Response, render_template, flash, redirect, url_for, session, g
from pymongo import MongoClient


def create_app(test_config=None):
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

    @app.route('/', methods=('POST', 'GET'))
    def home():
        if g.user is not None:
            return redirect(url_for('wall'))
        if request.method == 'POST':
            inserted_email = request.form['email']
            inserted_pass = request.form['password']
            error = None

            if not inserted_email:
                 error = 'Inserisci un indirizzo e-mail'

            if not inserted_pass:
                error = 'Inserisci una password'

            db = get_db()
            query_utente = db.utenti.find_one({'email': inserted_email})
            if query_utente is None:
                error = 'Utente non registrato'
            else:
                query_pass = db.utenti.find_one({'email': inserted_email})
                if not check_password_hash(query_pass['password'], inserted_pass):
                    error = 'Password inserita errata'

            if error is None:
                session.clear()
                session['user_id'] = str(ObjectId(query_utente['_id']))
                return redirect(url_for('wall'))

            flash(error)

        return render_template('home.xhtml')

    @app.route('/register', methods=('POST', 'GET'))
    def register():
        if request.method == 'POST':
            email = request.form['email']
            password = request.form['password']
            repassword = request.form['repassword']
            username = request.form['username']
            error = None
            if not email:
                error = 'Inserisci un indirizzo e-mail'

            if not password:
                error = 'Inserisci una password'

            if not username:
                error = 'Inserisci un username'

            if '@' not in email:
                error = 'Inserisci un indirizzo e-mail valido'

            if password != repassword:
                error = 'Le password inserite non sono uguali'

            db = get_db()
            gia_inserito = db.utenti.find_one({'email': email})

            if gia_inserito is not None:
                error = "Utente gia' registrato"

            nickname_esistente = db.utenti.find_one({'username': username})

            if nickname_esistente is not None:
                error = 'Nickname esistente'

            if error is None:
                db.utenti.insert({'email': email, 'password': generate_password_hash(password),
                                'username': username})  # Inserisco i dati nel db

                return redirect(url_for('home'))

            flash(error)
        return render_template('registrati.xhtml')

    @app.before_request
    def user_logged_in():
        logged_user = session.get('user_id')
        if logged_user is None:
            g.user = None
        else:
            db = get_db()
            query = db.utenti.find_one({'_id': ObjectId(logged_user)})
            g.user = query['username']

    @app.route('/logout')
    def logout():
        session.clear()
        return redirect(url_for('home'))

    @app.route('/wall')
    def wall():
        grooves = []
        db = get_db()
        query = db.grooves.files.find({}).sort('_id', pymongo.DESCENDING)
        for q in query:
            grooves.append({'ID': str(ObjectId(q['_id'])),
                            'Nome_groove': q['groove_name'],
                            'Autore': q['author'],
                            'Categoria': q['category'],
                            'UploadDate': q['uploadDate'],
                            'Strumenti': q['instruments']})

        return render_template('bacheca.xhtml', grooves=grooves)

    @app.route('/stream/<id_groove>', methods=['GET'])
    def stream(id_groove):
        db = get_db()
        groove = db.grooves.files.find_one({'_id': ObjectId(id_groove)})
        grid_fs = GridFS(db, collection='grooves')
        data = grid_fs.get(groove['_id']).read()
        return Response(data, mimetype='audio/wav')

    @app.route('/comment/<groove_id>', methods=('GET', 'POST'))
    def comment(groove_id):
        if request.method == 'POST':
            comment_text = request.form['comment_text']
            error = None

            if comment_text == ' ':
                error = 'Inserisci un commento'

            if len(comment_text) > 100:
                error = 'Inserisci un commento più breve'

            if error is None:
                db = get_db()
                user = db.utenti.find_one({'username': g.user})
                groove = db.grooves.files.find_one({'_id': ObjectId(groove_id)})
                db.commenti.insert({'ID_utente': str(ObjectId(user['_id'])),
                                    'ID_groove': str(ObjectId(groove['_id'])),
                                    'Testo_commento': comment_text})
                return redirect(url_for('wall'))

            flash(error)

        return render_template('form_commento.xhtml')

    @app.route('/groove_comments/<idgroove>', methods=['GET'])
    def groove_comments(idgroove):
        comments = []
        db = get_db()
        query = db.commenti.find({'ID_groove': idgroove}).sort('_id', pymongo.ASCENDING)
        for q in query:
            user = db.utenti.find_one({'_id': ObjectId(q['ID_utente'])})
            user_username = user['username']
            comment_text = q['Testo_commento']
            comments.append({'Autore': user_username,
                            'Commento': comment_text})

        return render_template('commenti.xhtml', commenti=comments)

    @app.route('/suggest/<idgroovetosuggest>', methods=('GET', 'POST'))
    def suggest(idgroovetosuggest):
        if request.method == 'POST':
            id_groove_chosen = request.form['id_groove_chosen']
            error = None

            db = get_db()
            groove = db.grooves.files.find_one({'_id': ObjectId(idgroovetosuggest)})
            groove_suggerito = db.grooves.files.find_one({'_id': ObjectId(id_groove_chosen)})

            query = db.suggerimenti.find_one({'ID_groove': str(ObjectId(groove['_id'])),
                                              'ID_groove_suggerito': str(ObjectId(groove_suggerito['_id']))})

            if query is not None:
                error = 'Hai già suggerito questo groove'

            if idgroovetosuggest == id_groove_chosen:
                error = 'Non puoi suggerire lo stesso groove'

            if error is None:
                db.suggerimenti.insert({'ID_groove': str(ObjectId(groove['_id'])),
                                        'ID_groove_suggerito': str(ObjectId(groove_suggerito['_id']))})

                return redirect(url_for('wall'))

            flash(error)

        grooves = []
        db = get_db()
        query = db.grooves.files.find({'author': g.user})
        for q in query:
            grooves.append({'ID': str(ObjectId(q['_id'])),
                            'Nome_groove': q['groove_name'],
                            'Autore': q['author'],
                            'Categoria': q['category'],
                            'UploadDate': q['uploadDate'],
                            'Strumenti': q['instruments']})

        return render_template('form_suggerisci.xhtml', grooves=grooves)

    @app.route('/suggestions/<idsuggestion>', methods=['GET'])
    def suggestions(idsuggestion):
        grooves = []
        db = get_db()
        query = db.suggerimenti.find({'ID_groove': idsuggestion}).sort('_id', pymongo.ASCENDING)

        for q in query:
            groove = db.grooves.files.find_one({'_id': ObjectId(q['ID_groove_suggerito'])})
            grooves.append({'ID': str(ObjectId(groove['_id'])),
                            'Nome_groove': groove['groove_name'],
                            'Autore': groove['author'],
                            'Categoria': groove['category'],
                            'UploadDate': groove['uploadDate'],
                            'Strumenti': groove['instruments']})

        return render_template('visualizza_suggeriti.xhtml', grooves=grooves)

    @app.route('/mywall', methods=['GET'])
    def mywall():
        grooves = []
        db = get_db()
        query = db.grooves.files.find({'author': g.user}).sort('uploadDate', pymongo.DESCENDING)
        for q in query:
            grooves.append({'ID': str(ObjectId(q['_id'])),
                            'Nome_groove': q['groove_name'],
                            'Autore': q['author'],
                            'Categoria': q['category'],
                            'UploadDate': q['uploadDate'],
                            'Strumenti': q['instruments']})

        return render_template('la_mia_bacheca.xhtml', grooves=grooves)

    return app
