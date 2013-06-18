# What's this?

An app that accepts POSTs of interesting URLs, fetches their titles if
possible, and displays them with attribution in a realtime stream using Server Sent Events.

There is a small read/write API.

The primordial use case for this app was to accept POSTs from a bot
capturing URLs out of IRC, but it will accept any well-formed POST and
tries not to enforce any data constraints apart from a simple schema.

This is a [Pedestal service](http://pedestal.io) targeting Heroku using Postgres for persistence
and Enlive for templating.


# Setup

These steps assume you already have a Heroku account and the [Heroku toolbelt](https://toolbelt.heroku.com/) installed.

Check out the dirt-magnet source...

`git clone git@github.com:daemianmack/dirt-magnet.git`

Inside that new directory, view `src/dirt-magnet/config.clj` and edit as
appropriate.

In particular, you'll want to modify the
`dirt-magnet.config/link-acceptable?` to suit your security needs.
See the Configuration section for more details.


Authenticate to Heroku...

```
> heroku login
Enter your Heroku credentials.
Email: daemianmack@gmail.com
Password (typing will be hidden):
Authentication successful.
```


Create the app...

```
> heroku create
Creating damp-woodland-3654... done, stack is cedar
http://damp-woodland-3654.herokuapp.com/ | git@heroku.com:damp-woodland-3654.git
Git remote heroku added
```


Add the Postgresql addon...

```
> heroku addons:add heroku-postgresql:dev
Adding heroku-postgresql:dev on damp-woodland-3654... done, v3 (free)
Attached as HEROKU_POSTGRESQL_NAVY_URL
Database has been created and is available
 ! This database is empty. If upgrading, you can transfer
 ! data from another database with pgbackups:restore.
Use `heroku addons:docs heroku-postgresql:dev` to view documentation.
```


Push local source to the app...

`> git push heroku master`


Apply the schema...

`> heroku run lein run -m dirt-magnet.storage/apply-schema`


Test the POST interface...

`> curl -X POST --data "url=http://www.example.com&source=testguy&password=professor-falken" http://damp-woodland-3654.herokuapp.com/links
{:created_at #inst "2013-06-09T18:49:33.248000000-00:00", :is_image false, :url "http://www.example.com", :source "testguy", :title nil, :id 1}`

Load your app in a browser and enjoy.


# Configuration

There are three functions in `src/dirt-magnet/config.clj` that give you
control over the input and output of link POSTing.

#### link-acceptable?

`config/link-acceptable?` is passed the request, where you may do with
it as you wish.

If `config/link-acceptable?` returns a truthy value, the link will be accepted,
and control will pass to `config/link-accepted`.

Otherwise, if `config/link-acceptable?` returns a falsy value, the link will be rejected,
and control will pass to `config/link-rejected`.

#### link-accepted

`config/link-accepted` will be passed the request and a map representing the
newly-created link. The usual case here will be to construct and
return a 'success' response to the POSTer.

#### link-rejected

`config/link-rejected` will be passed the request. The usual case here will
be to construct and return some response expressing
your displeasure with their meddling.

#### JSON, timestamps, and you: learning to live with disappointment

If you plan to use a JSON response anywhere, first consider using edn instead.

If you must use JSON, note that JSON responses containing timestamps will pass through special handling to convert any `java.util.Date` descendent into epoch time.


# Minimal web API

The read/write API allows edn, JSON, and HTML, and requires the Content-Type and Accept headers.

If an Accept header is not specified, the system will respond with a default of edn.

The [content negotiation interceptor](https://github.com/ToBeReplaced/pedestal-content-negotiation) doesn't handle non-success status codes, so responses will need to tunnel their own status information inside the body as shown in `dirt-magnet.config`...

```clojure
(response {:status-code 400
           :message "Nope."})
```

### Listing links

You can GET a link listing via edn, JSON, and HTML.

#### edn

```
> curl -i http://damp-woodland-3654.herokuapp.com/ -H "Accept: application/edn"
HTTP/1.1 200 OK
Content-Encoding: identity
Content-Type: application/edn;charset=utf-8
Date: Sun, 16 Jun 2013 16:59:11 GMT
Server: Jetty(8.1.9.v20130131)
transfer-encoding: chunked
Connection: keep-alive

({:created_at #inst "2013-06-16T13:02:38.101000000-00:00",
:is_image false,
:url "http://zombo.com",
:source "zombo",
:title "ZOMBO",
:id 4} ...)
```

#### JSON

Note that for dates you get epoch timestamps.

```
> curl -i http://damp-woodland-3654.herokuapp.com/ -H "Accept: application/json"
HTTP/1.1 200 OK
Content-Encoding: identity
Content-Type: application/json;charset=utf-8
Date: Sun, 16 Jun 2013 17:00:37 GMT
Server: Jetty(8.1.9.v20130131)
transfer-encoding: chunked
Connection: keep-alive

[{"created_at":1371418084000,
"is_image":false,
"url":"http:\/\/zombo.com",
"source":"zombo",
"title":"ZOMBO",
"id":4}, ...]
```

#### HTML

Really you should just be doing this in a browser.

```
> curl -i http://damp-woodland-3654.herokuapp.com/ -H "Accept: text/html"
HTTP/1.1 200 OK
Content-Encoding: identity
Content-Type: text/html;charset=UTF-8
Date: Sun, 16 Jun 2013 17:02:01 GMT
Server: Jetty(8.1.9.v20130131)
transfer-encoding: chunked
Connection: keep-alive

<html>

  <head>
    <title>dirt magnet</title>
...
```

### Creating new links

You can POST new links via edn, json, and x-www-form-urlencoded.

These examples are hitting a system that's configured to respond to a successful link creation by returning that link, as seen in the stock `src/dirt-magnet/config.clj`.

Link title-fetching happens asynchronously via a future, so the POSTing client doesn't have to wait for the fetch to complete before receiving the result.

#### edn

```
> curl -i -X POST --data '{:password "professor-falken" :url "http://zombo.com/", :source "zombo"}' http://damp-woodland-3654.herokuapp.com/links -H "Content-Type: application/edn" -H "Accept: application/edn"
HTTP/1.1 200 OK
Content-Encoding: identity
Content-Type: application/edn;charset=utf-8
Date: Sun, 16 Jun 2013 20:43:54 GMT
Server: Jetty(8.1.9.v20130131)
transfer-encoding: chunked
Connection: keep-alive

{:status-code 201, :message ({:created_at #inst "2013-06-16T20:43:54.670000000-00:00", :is_image false, :url "http://zombo.com/", :source "zombo", :title nil, :id 5})}

```

#### JSON

Note that for dates you get epoch timestamps.

```
> curl -i -X POST --data '{"password":"professor-falken", "url":"http://zombo.com/", "source":"zombo"}' http://damp-woodland-3654.herokuapp.com/links -H "Content-Type: application/json" -H "Accept: application/json"
HTTP/1.1 200 OK
Content-Encoding: identity
Content-Type: application/json;charset=utf-8
Date: Sun, 16 Jun 2013 20:52:07 GMT
Server: Jetty(8.1.9.v20130131)
transfer-encoding: chunked
Connection: keep-alive

{"status-code":201,
 "message":
 [{"created_at":1371418084000,
 "is_image":false,
 "url":"http:\/\/zombo.com\/",
 "source":"zombo",
 "title":null,
 "id":6}]}
```

#### x-www-form-urlencoded

The Content-Type header is specified here for completeness; curl sets this Content-Type automatically when it receives the --data option.

Note there is no corresponding return type configured, so we receive the system default of edn.

```
> curl -i -X POST --data "password=professor-falken&url=http://zombo.com/&source=zombo" \
http://damp-woodland-3654.herokuapp.com/links -H "Content-Type: application/x-www-form-urlencoded"
HTTP/1.1 200 OK
Content-Encoding: identity
Content-Type: application/edn;charset=utf-8
Date: Sun, 16 Jun 2013 20:54:22 GMT
Server: Jetty(8.1.9.v20130131)
transfer-encoding: chunked
Connection: keep-alive

{:status-code 201,
:message
({:created_at #inst "2013-06-16T20:54:22.052000000-00:00",
:is_image false,
:url "http://zombo.com/",
:source "zombo",
:title nil,
:id 7})}
```

# Import

If you have existing data you'd like to bring into dirt-magnet's
database, you can look at `src/dirt-magnet/import.clj` for some example
patterns that may save you some drudgery.

The quickest way to handle an import to a Heroku app would be to use a
local dirt-magnet instance to run an import to your local Postgres,
then dump the resulting database and restore it to the remote Heroku database.

You can do a `heroku config` to determine the DATABASE_URL. Pick out
the salient bits from the `HEROKU_POSTGRES_<SOME_URL>` var and place in
an `.env` file in the format...

`export DATABASE_URL=postgres://<username>:<password>@<hostname>:<port>/<dbname>`


After populating your local database, take a dump...

`PGPASSWORD=<password> /usr/local/bin/pg_dump -Fc --no-acl --no-owner -h localhost -U <username> <database> > db.dump`


Then perform the restore against the remote Heroku database...

`PGPASSWORD=<heroku_password> pg_restore --verbose --clean --no-acl --no-owner -h <heroku_host> -U <heroku_username> -d <heroku_database> -p <heroku_port> db.dump`


# Testing

`lein test`
