import argparse
import datetime
import requests
import threading
import time
from threading import Timer

# Helper class
class AtomicCounter:
    def __init__(self, initial=0):
        """Initialize a new atomic counter to given initial value (default 0)."""
        self.value = initial
        self._lock = threading.Lock()

    def increment(self, num=1):
        """Atomically increment the counter by num (default 1) and return the
        new value.
        """
        with self._lock:
            self.value += num
            return self.value

# Global variables (constant + cross-thread counters)
fullpass = AtomicCounter()
playedback = AtomicCounter()
sessionTokenError = AtomicCounter()

# Get all command line arguments
def getArguments():
  parser = argparse.ArgumentParser()
  parser.add_argument('--agency', dest="agency", action="store",default='spurbury.qa.evidence.com', help='Agency URL')
  parser.add_argument('--agency-id', dest="agencyid", action="store",default='8e6b1253-e6f6-46f3-bd82-95eff899c1ec', help='Agency ID')
  parser.add_argument('--evidence-id', dest="evidenceid", action="store",default='20fddd49eeba48a2baf1bc8b677c7c72', help='Evidence ID. For now, we use one hardcoded piece.')
  parser.add_argument('--file-id', dest="fileid", action="store",default='d9bb44b00ec34482b67b3450927d1fc5', help='File ID. For now, we use one hardcoded piece.')
  parser.add_argument('--username', dest="username", action="store", help='Username', required=True)
  parser.add_argument('--password', dest="password", action="store", help='Password', required=True)
  parser.add_argument('--batchsize', dest="batchsize", action="store",default='100', help='How many connections should be established per batch.')
  parser.add_argument('--batches', dest="batches", action="store",default='10', help='Number of batches with the given size to establish')
  parser.add_argument('--rampup', dest="rampup", action="store",default='10', help='How many seconds to wait between batches.')
  parser.add_argument('--verbose', dest="verbose", action="store_true", help='Whether or not to emit verbose logs')
  return parser.parse_args()

class RepeatedTimer(object):
  def __init__(self, interval, function, *args, **kwargs):
    self._timer     = None
    self.interval   = interval
    self.function   = function
    self.args       = args
    self.kwargs     = kwargs
    self.is_running = False
    self.start()

  def _run(self):
    self.is_running = False
    self.start()
    self.function(*self.args, **self.kwargs)

  def start(self):
    if not self.is_running:
      self._timer = Timer(self.interval, self._run)
      self._timer.start()
      self.is_running = True

  def stop(self):
    self._timer.cancel()
    self.is_running = False

def login(user, password, agency, agencyIdConc):
  """Creates a session that can be used for tests.

  :param user: username of user logging in
  :param password: password of user logging in
  :param agencyId: Agency ID

  :return: cookie object
  """
  headers = {
    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
    'X-Requested-With': 'XMLHttpRequest',
    'Connection': 'keep-alive',
  }

  form_data = {
    'format': 'json',
    'class': 'Subscriber',
    'proc': 'Login',
    'action': 'login',
    'resource_id': 'www',
    'username': user,
    'password': password,
    'partner_id': agencyIdConc,
  }

  response = requests.request(
    method='POST',
    url='https://{}/index.aspx'.format(agency),
    headers=headers,
    data=form_data
  )

  session = response.cookies['AXONSESSION']

  return {
    'AXONSESSION': session,
  }

# This will start a media session and download all segments of a file
def streamMedia(agency, agencyIdConc, evidenceId, fileId, _cookies):
  startUrl = 'https://' + agency + '/api/v1/media/start?partner_id=' + agencyIdConc + '&evidence_id=' + evidenceId + '&file_id=' + fileId
  response = requests.get(
    startUrl,
    cookies=_cookies
  )
  sessionToken = ''
  try:
    sessionToken = response.json()['streamingSessionToken']
  except:
    sessionTokenError.increment()
    return
  variantUrl = 'https://' + agency + '/api/v1/media/hls/variant?partner_id=' + agencyIdConc + '&evidence_id=' + evidenceId + '&file_id=' + fileId + '&streamingSessionToken=' + sessionToken + '&level=1&size_wxh=854x480&autorotate=true'
  response = requests.get(
    variantUrl,
    cookies=_cookies
  ).text.splitlines()
  urls = []
  
  for line in response:
    if '/api' in line:
      urls.append('https://' + agency + line)
  for url in urls:
    response = requests.get(
      url,
      cookies=_cookies
    ).text
    playedback.increment()
  fullpass.increment()
  
def testRun(agency, agencyId, evidenceId, fileId, username, password, batches, batchsize, rampupPause, verbose):
  agencyIdConc = agencyId.replace('-', '')
  cookie = login(username, password, agency, agencyIdConc)
  threads = []
  for batchNum in range(batches):
    localThreads = []
    for runNum in range(batchsize):
      t = threading.Thread(target=streamMedia, args=(agency, agencyIdConc, evidenceId, fileId, cookie))
      threads.append(t)
      localThreads.append(t)
    for x in localThreads:
      x.start()
    if verbose:
      printWithDateTime(str((batchNum + 1) * batchsize) + ' streams kicked off.')
    time.sleep(rampupPause)
  printWithDateTime('All streams kicked off. Waiting for playbacks to complete.')
  for x in threads:
    x.join()

def printWithDateTime(message):
  now = str(datetime.datetime.now()) + " "
  print(now + message)

def keepAlive(agency):
  keepAliveUrl = 'https://' + agency + '/index.aspx?format=json&proc=KeepAlive&token_code_af=a'
  print('Sending keepAlive request (url=%s).' % keepAliveUrl)
  response = requests.request(
    method='GET',
    url = keepAliveUrl
  )

print ('Starting.')
args = getArguments()
rt = RepeatedTimer(300, keepAlive, args.agency)
try:
  testRun(args.agency, args.agencyid, args.evidenceid, args.fileid, args.username, args.password, int(args.batches), int(args.batchsize), int(args.rampup), args.verbose)
finally:
  rt.stop()
  printWithDateTime('Run finished.')
  printWithDateTime('Media streams attempted: ' + str(int(args.batches) * int(args.batchsize)))
  printWithDateTime('Media streams survived: ' + str(fullpass.value))
  printWithDateTime('Sections played back: ' + str(playedback.value))
  printWithDateTime('Session Token retrieval errors: ' + str(sessionTokenError.value))
