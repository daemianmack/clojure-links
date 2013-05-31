(ns dirt-magnet.fixtures)


(def url "http://zombo.com")
(def title "ZOMBO")

(def body-response
  {:body
   "<html>
    <head>
    <title>ZOMBO</title>
    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">
    </head>

    <body bgcolor=\"#FFFFFF\">
    You can do anything at Zombo.com
    </body>
    </html>"})

(def html-headers
  {:status 200
   :headers {"date" "Thu, 30 May 2013 00:39:07 GMT"
             "server" "Apache/2.0.63 (Unix) mod_ssl/2.0.63"
             "last-modified" "Tue, 23 Apr 2002 05:19:28 GMT"
             "etag" "\"b2013c-2f6-f5f17800\""
             "accept-ranges" "bytes"
             "content-length" "758"
             "connection" "close"
             "content-type" "text/html"}
   :body nil})

(def jpeg-headers
  {:status 200
   :headers {"date" "Thu, 30 May 2013 00:45:50 GMT"
             "server" "Apache/2.2.13 (CentOS)"
             "last-modified" "Thu, 25 Mar 2010 14:02:22 GMT"
             "etag" "\"1431ad2-136be-482a07ee29780\""
             "accept-ranges" "bytes"
             "content-length" "79550"
             "connection" "close"
             "content-type" "image/jpeg"}
   :body nil})
