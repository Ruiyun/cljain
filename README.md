cljain
=========

The cljain is an abstraction layer sitting on top of [JAIN-SIP].
It let's you use it as [Clojure] way.

Installation
============

Add the following to your **project.clj** or pom.xml:

Lein artifact:

    [cljain "0.2.0"]

Maven:

    <dependency>
      <groupId>cljain</groupId>
      <artifactId>cljain</artifactId>
      <version>0.2.0</version>
    </dependency>

Then execute

    lein deps

And here is an example to show how to work with cljain.

```clojure
    (use 'cljain.dum)
    (require '[cljain.sip.core :as sip]
             '[cljain.sip.header :as header]
             '[cljain.sip.address :as address]
             '[cljain.sip.message :as message])

    (def-request-handler :MESSAGE [request transaction dialog]
      (send-response! 200 :in transaction :pack "I receive your message."))

    (sip/global-bind-sip-provider! (sip/sip-provider! "my-app" "127.0.0.1" 5060 "udp"))
    (initialize! :user "bob" :domain "home" :display-name "Bob")
    (sip/start!)

    (let [alice (address/address (address/sip-uri "127.0.0.1" :port 5070 :user "alice") "Alice")]
      (send-request! :MESSAGE :to alice :pack "Hello, Alice."
        :on-success (fn [_ _ _] (println "Message has been sent successfully."))
        :on-failure (fn [_ _ response] (println "oops," (message/reason response)))
        :on-timeout (fn [_] (println "Timeout, try it later."))))
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