from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app

class BlogHandler(webapp.RequestHandler):
  def get(self, tail = ''):
    self.redirect('/generator/'+tail, permanent = True)

application = webapp.WSGIApplication(
        [
        (r'^/generator$',     BlogHandler)
        ])

def main():
  run_wsgi_app(application)

if __name__ == "__main__":
  main()
