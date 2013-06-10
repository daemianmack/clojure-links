What's this?
=

An app that accepts POSTs of interesting URLs, fetches their titles if
possible, and displays them with attribution in a
paginated stream, in realtime, using Server Sent Events.

The use case for which it was created was to accept POSTs from a bot
capturing URLs out of IRC, but it will accept POSTs from anything and
tries not to enforce any data constraints apart from a simple schema.

This is a [Pedestal service](http://pedestal.io) targeting Heroku using Postgres for persistence
and Enlive for templating.


Setup
=

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


Configuration
=

There are three functions in `src/dirt-magnet/config.clj` that give you
control over the input and output of link POSTing.

`config/link-acceptable?` is passed the request, where you may do with
it as you wish.

If `config/link-acceptable?` returns a truthy value, the link will be accepted,
and control will pass to `config/link-accepted`.

Otherwise, if `config/link-acceptable?` returns a falsy value, the link will be rejected,
and control will pass to `config/link-rejected`.

`config/link-accepted` will be passed the request and a map representing the
newly-created link. The usual case here will be to construct and
deliver a 'success' response to the POSTer.

`config/link-rejected` will be passed the request. The usual case here will
be to construct and deliver to the POSTer some response expressing
your displeasure with their meddling.


Import
=

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


Testing
=

`lein test`
