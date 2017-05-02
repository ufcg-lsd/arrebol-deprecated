import requests
import time

req = requests.Session()

#n = req.get("http://##.##.##.##:#####/arrebol/nonce")
#print n.text
#req.headers.update(header)
#r = req.get("http://##.##.##.##:#####/arrebol/job")

def getalljobs() :
    n = req.get("http://##.##.##.##:#####/arrebol/nonce")
    n = req.get("http://##.##.##.##:#####/arrebol/nonce")
    header = { "X-auth-credentials":"{ username: #######, password: ######, nonce: "+ n.text+" }"}
    r = req.get("http://##.##.##.##:#####/arrebol/job", headers=header)
    return r

def postsleepjob() :
    n = req.get("http://##.##.##.##:#####/arrebol/nonce")
    n = req.get("http://##.##.##.##:#####/arrebol/nonce")
    header = { "X-auth-credentials":"{ username: #######, password: ######, nonce: "+ n.text+" }"}
    r = req.post("http://##.##.##.##:#####/arrebol/job",
         files={
             "jdffilepath": ("", "/home/igorvcs/git/arrebol/sleep.jdf"),
             "X-auth-credentials": ("", "{ username: #######, password: #######, nonce: "+ n.text+ "}")}, headers=header)
    return r

def getsleepjob() :
    n = req.get("http://##.##.##.##:#####/arrebol/nonce")
    header = { "X-auth-credentials":"{ username: #######, password: #######, nonce: "+ n.text+" }"}
    r= req.get("http://##.##.##.##:#####/arrebol/job/job233", headers=header)
    return r

def deletesleepjob() :
    n = req.get("http://##.##.##.##:#####/arrebol/nonce")
    header = { "X-auth-credentials":"{ username: #######, password: ########, nonce: "+ n.text+" }"}
    r= req.delete("http://##.##.##.##:#####/arrebol/job/job233",
       files={
              "X-auth-credentials": ("", "{ username: #######, password: #######, nonce: "+ n.text+ "}")}, headers=header)
    return r

def checksleepjob(reqstring) :
    if "COMPLETED" in reqstring:
        return True
    return False

def positiveresult() :
    print "PASSOU!"

def negativeresult() :
    print "NAO PASSOU!"

def main():
    postsleepjob()
    t = getalljobs()
    print t.content
    t = getsleepjob()
    print "pre alocation" +  t.content
    time.sleep(900)
    t = getsleepjob()
    if checksleepjob(t.content):
        positiveresult()
    else :
        negativeresult()
    deletesleepjob()



if __name__ == "__main__":
    main()
