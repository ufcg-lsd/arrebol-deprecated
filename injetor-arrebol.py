import requests

req = requests.Session()

n = req.get("http://10.11.5.160:44444/arrebol/nonce")
print n.text
header = {"X-auth-credentials":'{ username: igorvcs, password: 0k4s3t0k4, nonce: '+ n.text+' }' }
req.headers.update(header)
print req.headers["X-auth-credentials"]
r = req.get("http://10.11.5.160:44444/arrebol/job")

n = req.get("http://10.11.5.160:44444/arrebol/nonce")
postheader = { "X-auth-credentials":'{ username: igorvcs, password: 0k4s3t0k4, nonce: '+ n.text+' }'}
req.headers.update(postheader)
dados = { "jdffilepath":"/home/igorvcs/git/arrrebol/sleep.jdf" }
r = req.post("http://10.11.5.160:44444/arrebol/job", data=dados)



print r.content
