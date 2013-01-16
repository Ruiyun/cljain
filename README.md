cljain
=========

The cljain is an abstraction layer sitting on top of [JAIN-SIP].
It let's you use it in [Clojure]'s way.

Installation
============

Add the following to your **project.clj** or pom.xml:

Lein artifact:

    [cljain "0.4.0-SNAPSHOT"]

Maven:

    <dependency>
      <groupId>cljain</groupId>
      <artifactId>cljain</artifactId>
      <version>0.4.0-SNAPSHOT</version>
    </dependency>

Then execute

    lein deps

And here is an example to show how to work with cljain.

```clojure
    (use 'cljain.dum)
    (require '[cljain.sip.core :as sip]
      '[cljain.sip.address :as addr])

    (def-request-handler :MESSAGE [request transaction dialog]
      (println "Received: " (.getContent request))
      (send-response! 200 :in transaction :pack "I receive your message."))

    (sip/global-bind-sip-provider! (sip/sip-provider! "my-app" "localhost" 5060 "udp"))
    (initialize! :user "bob" :domain "home" :display-name "Bob")
    (sip/start!)

    (send-request! :MESSAGE :to (addr/address "sip:alice@localhost") :pack "Hello, Alice."
      :on-success (fn [_ _ response] (println "Fine! response: " (.getContent response)))
      :on-failure (fn [_ _ response] (println "Oops!" (.getStatusCode response)))
      :on-timeout (fn [_] (println "Timeout, try it later.")))
```

Documentation
=============

For more detailed information on **cljain**, please refer to the  [documentation].

License
=======

Copyright (C) 2012 Ruiyun Wen

Distributed under the Eclipse Public License, the same as Clojure.

[JAIN-SIP]:             http://hudson.jboss.org/hudson/job/jain-sip/lastSuccessfulBuild/artifact/javadoc/index.html
[Clojure]:              http://clojure.org/
[documentation]:        http://ruiyun.github.com/cljain/
