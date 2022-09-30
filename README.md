# quarkus-suntimes-auth-trivial
 
Version 1.0.0
Kevin Boone, September 2022

# What is this?

This application demonstrates the simplest application of security 
I could think of, in a REST webservice implemented with 
Camel extensions for Quarkus. It exposes a service using HTTPS,
and accepts credentials using the HTTP Basic method. User credentials
are stored in a file, but the passwords are hashed, not in plaintext.
So, although this is a simple application, it demonstrates a robust,
if old-fashioned, approach to security.

The webservice itself calculates sunrise and sunset times for a specific
location on a specific date. I've described the application itself in
my `quarkus-suntimes` application -- here I will only describe what extra
steps are needed to implement security.

## Requirements

The webservice provides two REST APIs: "/suntimes" and "/health". 
The first of these performs the actual calculation, and will only
be accessible to authorized users. The second is unrestricted, and
just returns some text if the service is working.

The application will use HTTP basic authentication. In this mode of
authentication, a user and password are sent with the request as
HTTP headers. Of course, it's only remotely safe to use this method
of authentication if the client-service request is HTTPS, so it will
be necessary to enable TLS support in the application.

The user credentials are stored in a file `users.properties`, which 
is bundled into the JAR file when the application is built. The 
server certificate is stored in a Java keystore which is also bundled.
The locations of these files are defined in `application.properties`, and
_can't_ be overridden at run-time using environment variables, unlike
many Quarkus settings -- see below for more on this point.

The `users.properties` file stores hashed, not cleartext, 
credentials -- see the notes at the end of this document for
information on hashing the credentials.

## Prerequisites

- Java JDK 11 or later
- Maven version 3.2.8 or later
- GraalVM with native compilation extension installed, to test the native-code compilation support
- An HTTP utility like cURL for testing

## Building

    $ mvn clean package

This will build a "fat" JAR containing the application and all its
dependencies. 

To build a native binary using GraalVM, use:

    $ GRAALVM_HOME=/path/to/graalvm mvn clean package -Pnative

## Running

To run the self-contained JAR: 

   java -jar target/quarkus-suntimes-auth-trivial-1.0.0-runner.jar 

To run in development mode, use:

    mvn quarkus:dev

## Testing

It should be possible to invoke the `/health' endpoint without any
credentials: 

    $ curl https://localhost:8443/health
    OK

But the `/suntimes' endpoints require a user and password. For example:

    $ curl -u fred:bloggs -k https://localhost:8443/suntimes/local/Europe:Paris/2022-09-28

Note that the certificate embedded in the application is self-signed, so
`curl` needs to be invoked with the `-k` switch, or it will reject the
certificate. At present, the only user defined (see the `users.properties`
file) is 'fred' with password 'bloggs'. Of course, it is easy to
add additional users to the file.

## Notes

### Creating the certificate

The TLS certificate starts life in the `/src/main/resources` directory, and
gets included by the build in to the JAR at the top of the classpath. For
example:

    $ keytool -genkey -alias server-alias -keyalg RSA -keypass changeit -keystore src/main/resources/keystore.jks

The name of the file, and the password that protects it, are specified in
`application.properties`.

    quarkus.http.ssl.certificate.key-store-file=keystore.jks
    quarkus.http.ssl.certificate.key-store-password=changeit

### Managing users at runtime

In this simple application, the user credentials are defined in files
bundled into the application. The file locations are defined in
`application.properties`. Although these settings can be overridden using
environment variables, any attempt to do so will fail -- Quarkus simply
won't allow it.

If you want to store credentials files (users and roles) in the filesystem,
so that they can be edited, their locations need to be determined at
build time. Then the file locations can be placed into the configuration 
variables

    quarkus.security.users.file.users=/path/to/users.properties
    quarkus.security.users.file.roles=/path/to/roles.properties
 
Remember, though, that Quarkus will search the classpath first, so
embedded files will be found before the filesystem is searched. So
far as I know, there is no way to have an embedded credentials file
as a kind of fallback, which can be overridden at runtime.

### Issues related to HTTPS support 

In a container environment like Kubernetes, an ingress controller
might terminate TLS, and pass plaintext to the application. In that
case it might not be necessary for the application itself to support HTTPS.
However, operating in this mode passes the HTTP
data in the clear around the container cluster, including the credentials.
Whether this is a problem or not depends on site security policies.

As configured, this application exposes both HTTP and HTTPS services. 
You can disable the HTTP service using the configuration property:

    quarkus.http.insecure-requests=disabled

or redirect requests to the HTTPS service using

    quarkus.http.insecure-requests=redirect

### Hashing user credentials

It's not really safe to store cleartext passwords in files, even if these
files are bundled into an application. If cleartext passwords
_are_ enabled, then the `users.properties` file has entries of the form

    user1=password1
    ...

If cleartext passwords are not enabled -- which is the default with
Quarkus -- then the "password" field is replaced by an MD5 hash.

The hash isn't just of the password, but of the combination

    user:realm:password

where realm is defined in the configuration property

    quarkus.security.users.file.realm-name=MyRealm

So, to generate the hash for user "fred" with password "bloggs" I
did:

    $ echo -n fred:MyRealm:bloggs | md5sum

The whole text goes into the properties file (32 hex digits) 
directly after '=". That is, there is no
special token to indicate that this is a hashed password -- Quarkus
presently only supports MD5.

