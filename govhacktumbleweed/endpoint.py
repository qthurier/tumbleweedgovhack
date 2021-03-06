
from google.appengine.ext import ndb
import webapp2
import logging
import json
import urllib2
import os

import hmac
import hashlib

class EndpointKey(ndb.Model):
    """A model to store the endpoint keys"""
    key = ndb.StringProperty()
    created_at = ndb.DateTimeProperty(auto_now_add=True)

class EndpointToken(ndb.Model):
    """A model to store the endpoint tokens"""
    installation_id = ndb.StringProperty()
    token = ndb.StringProperty()
    created_at = ndb.DateTimeProperty(auto_now_add=True)

def handle_401(request, response, exception):
    logging.debug("header check didnt pass")
    body = {'status': 'authentication failed'}
    response.status = 401
    response.headers['Content-Type'] = 'application/json'   
    response.out.write(json.dumps(body))

class BasicHandler(webapp2.RequestHandler):
    def __init__(self, request, response):
        webapp2.RequestHandler.__init__(self, request, response)
        client_token = self.request.headers['Token']
        installation_id = self.request.headers['Id']
        EndpointToken.query(EndpointToken.installation_id == installation_id).fetch(1)
        server_token = EndpointToken.query(EndpointToken.installation_id == installation_id).fetch(1) # suppose uniquness of installation_id
        logging.debug(client_token)
        logging.debug(server_token)
        if len(server_token) < 1 or client_token != server_token[0].token:
          self.abort(401)

class Token(webapp2.RequestHandler): # the only one which unherit from BasicHandler
    def get(self):
      key = str(EndpointKey.query().fetch(1)[0].key)
      installation_id = self.request.get('installation_id')
      token = str(os.urandom(24).encode('hex'))
      old_token_list = EndpointToken.query(EndpointToken.installation_id == installation_id).fetch(100)
      if len(old_token_list) > 0:
        for t in old_token_list:
          t.delete()
      hashed = hmac.new(key, token, hashlib.sha256).hexdigest()
      EndpointToken(installation_id=installation_id, token=hashed).put()
      body = {'token': token}
      self.response.status = 200
      self.response.headers['Content-Type'] = 'application/json'   
      self.response.out.write(json.dumps(body))

###### 

class Rating(ndb.Model):
    """A model to store a playground rating"""
    record_id = ndb.IntegerProperty()
    playground_name = ndb.StringProperty()
    installation_id = ndb.StringProperty()
    mark = ndb.IntegerProperty()
    created_at = ndb.DateTimeProperty(auto_now_add=True)

class Playground(ndb.Model):
    """Model of a playground"""
    name = ndb.StringProperty()
    nb_clicks = ndb.IntegerProperty()
    nb_ratings = ndb.IntegerProperty()
    rating = ndb.FloatProperty()

class RatingEndpoint(BasicHandler):
    """Store a rating"""
    def _update_playground_rating(self, playground_name, rating):
        playground = Playground.query(Playground.name == playground_name).fetch(1)

        if len(playground) > 0:
            playground = playground[0]
            logging.debug(playground)
            playground.rating = (playground.rating * playground.nb_ratings + rating) / (playground.nb_ratings + 1)
            playground.nb_ratings += 1
        else:
            playground = Playground(
                name = playground_name,
                nb_clicks = 1,
                nb_ratings = 1,
                rating = rating
                )

        playground.put()

    def _update_user_rating(self, installation_id, record_id, playground_name, mark):
        rating = Rating.query(
            ndb.AND(
                Rating.installation_id == installation_id, 
                Rating.playground_name == playground_name)
            ).fetch(1)

        # load existing record or create a new one
        if len(rating) > 0 :
            rating = rating[0]
            logging.debug(rating)
            rating.mark = int(mark)
            rating.record_id = int(record_id)
            rating.put()
        else:
            rating = Rating(
                installation_id=installation_id, 
                record_id=int(record_id), 
                playground_name=playground_name,
                mark=int(mark))

        # store record
        rating.put()        

    def post(self):
        installation_id = self.request.POST.get('installation_id')
        record_id = self.request.POST.get('record_id')
        playground_name = self.request.POST.get('playground_name')
        mark = self.request.POST.get('mark')

        # add or update user rating of the playground
        self._update_user_rating(installation_id, record_id, playground_name, mark)

        # add rating to the playground
        self._update_playground_rating(playground_name, int(mark))

        self.response.status = 200
    
    """Get a rating"""
    def get(self):
      playground_name = self.request.get('playground_name')
      installation_id = self.request.get('installation_id', None)
      if installation_id is None:
        rating_list = Rating.query(Rating.playground_name == playground_name)
        logging.debug("get global rating")
      else:
        rating_list = Rating.query(ndb.AND(Rating.playground_name == playground_name,
                                           Rating.installation_id == installation_id)
                                  )   
        logging.debug("get installation rating")       
      # None checking just in case to avoid the sum error  
      # and cast to float to avoid int division in the sum                
      mark_list = [float(r.mark) for r in rating_list if r.mark is not None] 
      logging.debug(mark_list)
      if len(mark_list) > 0:
        body = {'rating': round(sum(mark_list)/len(mark_list), 1), 'count': len(mark_list)}
      else:
        body = {'rating': None, 'count': 0}
      self.response.status = 200
      self.response.headers['Content-Type'] = 'application/json'   
      self.response.out.write(json.dumps(body))
      
class Click(ndb.Model):
    """A model to store clicks"""
    installation_id = ndb.StringProperty()
    record_id = ndb.IntegerProperty()
    playground_name = ndb.StringProperty()
    latitude = ndb.FloatProperty()
    longitude = ndb.FloatProperty()
    created_at = ndb.DateTimeProperty(auto_now_add=True)
                                           
class StoreClick(BasicHandler):
    """Store a click"""
    def post(self):
      installation_id = self.request.POST.get('installation_id')
      record_id = self.request.POST.get('record_id')
      playground_name = self.request.POST.get('playground_name')
      latitude = self.request.POST.get('latitude')
      longitude = self.request.POST.get('longitude')
      logging.debug(self.request.POST)
      logging.debug(installation_id)
      logging.debug(record_id)
      logging.debug(playground_name)
      Click(installation_id=installation_id, 
            record_id=int(record_id), 
            playground_name=playground_name,
            latitude=float(latitude),
            longitude=float(longitude)).put()
      self.response.status = 200


#########

class Favorites(ndb.Model):
    """A model to store the list of favorite playgrounds per installation_id"""
    installation_id = ndb.StringProperty()
    record_id_list = ndb.IntegerProperty(repeated=True)
    playground_name_list = ndb.StringProperty(repeated=True)

                                           
class FavoriteEndpoint(BasicHandler):

    """register a playground as a favorite"""
    def post(self):
      installation_id = self.request.POST.get('installation_id')
      record_id = self.request.POST.get('record_id')
      playground_name = self.request.POST.get('playground_name')
      what_to_do = self.request.POST.get('type')
      if what_to_do == "save":
        favorites = Favorites.query(Favorites.installation_id == installation_id).fetch(1)
        if len(favorites) > 0 :
          if playground_name not in favorites[0].playground_name_list: # should always be true but just in case 
            favorites[0].record_id_list.append(int(record_id))
            favorites[0].playground_name_list.append(playground_name)
            favorites[0].put()
        else:
          Favorites(installation_id=installation_id, 
                  record_id_list=[int(record_id)], 
                  playground_name_list=[playground_name]).put()
        self.response.status = 200
      else:
        favorites = Favorites.query(Favorites.installation_id == installation_id).fetch(1)
        if len(favorites) > 0 :
          if playground_name in favorites[0].playground_name_list: # should always be true just in case 
            favorites[0].record_id_list.remove(int(record_id))
            favorites[0].playground_name_list.remove(playground_name)
            favorites[0].put()
        else:
          pass # shouldnt happen
        self.response.status = 200

    """get a list of favorite playgrounds"""
    def get(self):
      installation_id = self.request.get('installation_id')
      favorites = Favorites.query(Favorites.installation_id == installation_id).fetch(1)
      if len(favorites) > 0 :
        body = {'record_id_list': favorites[0].record_id_list, 'playground_name_list': favorites[0].playground_name_list}
      else:
         body = {'record_id_list': [], 'playground_name_list': []}
      self.response.status = 200
      self.response.headers['Content-Type'] = 'application/json'   
      self.response.out.write(json.dumps(body))


class CheckFavoriteEndpoint(BasicHandler):

    """check if a playground belongs to the favorite"""
    def get(self):
      installation_id = self.request.get('installation_id')
      playground_name = self.request.get('playground_name')
      favorites = Favorites.query(ndb.AND(Favorites.installation_id == installation_id, 
                                          Favorites.playground_name_list.IN([playground_name]))).fetch(1)
      if len(favorites) > 0 :
        body = {'check': True}
      else:
         body = {'check': False}
      self.response.status = 200
      self.response.headers['Content-Type'] = 'application/json'   
      self.response.out.write(json.dumps(body))

#############

class Visited(ndb.Model):
    """A model to store the list of visited playgrounds per installation_id"""
    installation_id = ndb.StringProperty()
    record_id_list = ndb.IntegerProperty(repeated=True)
    playground_name_list = ndb.StringProperty(repeated=True)

class VisitedEndpoint(BasicHandler):

    """register a visited playground"""
    def post(self):
      installation_id = self.request.POST.get('installation_id')
      record_id = self.request.POST.get('record_id')
      playground_name = self.request.POST.get('playground_name')
      what_to_do = self.request.POST.get('type')
      if what_to_do == "save":
        visited = Visited.query(Visited.installation_id == installation_id).fetch(1)
        if len(visited) > 0 :
          if playground_name not in visited[0].playground_name_list: # should always be true but just in case 
            visited[0].record_id_list.append(int(record_id))
            visited[0].playground_name_list.append(playground_name)
            visited[0].put()
        else:
          Visited(installation_id=installation_id, 
                  record_id_list=[int(record_id)], 
                  playground_name_list=[playground_name]).put()
        self.response.status = 200
      else:
        visited = Visited.query(Visited.installation_id == installation_id).fetch(1)
        if len(visited) > 0 :
          if playground_name in visited[0].playground_name_list: # should always be true just in case 
            visited[0].record_id_list.remove(int(record_id))
            visited[0].playground_name_list.remove(playground_name)
            visited[0].put()
        else:
          pass # shouldnt happen
        self.response.status = 200

    """get a list of favorite playgrounds"""
    def get(self):
      installation_id = self.request.get('installation_id')
      visited = Visited.query(Visited.installation_id == installation_id).fetch(1)
      if len(visited) > 0 :
        body = {'record_id_list': visited[0].record_id_list, 'playground_name_list': visited[0].playground_name_list}
      else:
         body = {'record_id_list': [], 'playground_name_list': []}
      self.response.status = 200
      self.response.headers['Content-Type'] = 'application/json'   
      self.response.out.write(json.dumps(body))

class CheckVisitedEndpoint(BasicHandler):

    """check if a playground has been visited"""
    def get(self):
      installation_id = self.request.get('installation_id')
      playground_name = self.request.get('playground_name')
      visited = Visited.query(ndb.AND(Visited.installation_id == installation_id, 
                                      Visited.playground_name_list.IN([playground_name]))).fetch(1)
      if len(visited) > 0 :
        body = {'check': True}
      else:
         body = {'check': False}
      self.response.status = 200
      self.response.headers['Content-Type'] = 'application/json'   
      self.response.out.write(json.dumps(body))

##############

class TopEndpoint(BasicHandler):

    """get the list of favorite playgrounds"""
    def get(self):
      top = self.request.get('top')
      playgrounds = Playground.query(Playground.rating > 0).order(-Playground.rating).fetch(int(top))
      if len(playgrounds) > 0 :
        playground_name_list = [playground.name for playground in playgrounds]
        body = {'playground_name_list': playground_name_list}
      else:
         body = {'playground_name_list': []}
      self.response.status = 200
      self.response.headers['Content-Type'] = 'application/json'   
      self.response.out.write(json.dumps(body))



APPLICATION = webapp2.WSGIApplication([('/store_click', StoreClick),
                                       ('/rating', RatingEndpoint),
                                       ('/favorite_endpoint', FavoriteEndpoint),
                                       ('/visit_endpoint', VisitedEndpoint),
                                       ('/check_favorite', CheckFavoriteEndpoint),
                                       ('/check_visit', CheckVisitedEndpoint),
                                       ('/top', TopEndpoint),
                                       ('/token', Token)], debug=True)


APPLICATION.error_handlers[401] = handle_401
