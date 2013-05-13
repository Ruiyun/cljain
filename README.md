cljain
=========

The cljain is an abstraction layer sitting on top of [JAIN-SIP].
It let's you use it in [Clojure]'s way.

Installation
============

Add the following to your **project.clj** :

    [cljain "0.5.0-RC2"]

Then execute

    lein deps

And here is an example to show how to work with cljain.

```clojure
(use 'cljain.dum)
(require '[cljain.sip.core :as sip]
  '[cljain.sip.address :as addr])

(defmethod handle-request :MESSAGE [request transcation _]
  (println "Received: " (.getContent request))
  (send-response! 200 :in transaction :pack "I receive the message from myself."))

(global-set-account :user "bob" :domain "localhost" :display-name "Bob" :password "thepwd")
(sip/global-bind-sip-provider! (sip/sip-provider! "my-app" "localhost" 5060 "udp"))
(sip/set-listener! (dum-listener))
(sip/start!)

(send-request! :MESSAGE :to (addr/address "sip:bob@localhost") :pack "Hello, Bob."
  :on-success (fn [& {:keys [response]}] (println "Fine! response: " (.getContent response)))
  :on-failure (fn [& {:keys [response]}] (println "Oops!" (.getStatusCode response)))
  :on-timeout (fn [_] (println "Timeout, try it later.")))
```

Documentation
=============

For more detailed information on **cljain**, please refer to the  [documentation].

License
=======

Copyright (C) 2013 Ruiyun Wen

Distributed under the Eclipse Public License, the same as Clojure.

[JAIN-SIP]:             http://hudson.jboss.org/hudson/job/jain-sip/lastSuccessfulBuild/artifact/javadoc/index.html
[Clojure]:http://clojure.org/
[documentation]:        http://ruiyun.github.io/cljain/
