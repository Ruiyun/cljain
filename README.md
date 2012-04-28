ClojureQL
=========

ClojureQL is an abstraction layer sitting on top of JAIN-SIP library.
It let's you use it as clojure way.

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

    (use 'cljain.dum)
    (require '[cljain.sip.core :as core]
             '[cljain.sip.header :as header]
             '[cljain.sip.address :as address]
             '[cljain.sip.message :as message])

    (def-request-handler :MESSAGE [request transaction dialog]
      (send-response! 200 :in transaction :pack "I receive your message."))

    (core/global-bind-sip-provider! (core/sip-provider! "my-app" "127.0.0.1" 5060 "udp"))
    (initialize! :user "bob" :domain "home" :display-name "Bob")
    (core/start!)

    (let [alice (address/address (address/sip-uri "127.0.0.1" :port 5070 :user "alice") "Alice")]
      (send-request! :MESSAGE :to alice :pack "Hello, Alice."
        :on-success (fn [_ _ _] (println "Message has been sent successfully."))
        :on-failure (fn [_ _ response] (println "oops," (message/reason response)))
        :on-timeout (fn [_] (println "Timeout, try it later."))))

License
=======

Copyright (C) 2012 Ruiyun Wen

Distributed under the Eclipse Public License, the same as Clojure.
