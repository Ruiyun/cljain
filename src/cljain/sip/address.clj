(ns cljain.sip.address
  (:require [clojure.string :refer [lower-case]]
            [cljain.sip.core :refer [sip-factory]])
  (:import [javax.sip SipFactory]
           [javax.sip.address AddressFactory URI SipURI Address]))

(def ^{:doc "The factory to create Address and URI."
       :tag AddressFactory
       :private true}
  factory (.createAddressFactory ^SipFactory sip-factory))

(defn sip-uri
  "Create a new SipURI object.

  (sip-uri \"localhost\" :port 5060 :transport \"udp\" :user \"tom\")"
  [host & {:keys [user port transport]}]
  {:pre [(or (nil? port) (and (integer? port) (pos? port)))
         (or (nil? transport) (#{"udp" "tcp"} (lower-case transport)))]
   :post [(instance? SipURI %)]}
  (let [uri (.createSipURI factory nil host)]
    (and user (.setUser uri user))
    (and port (.setPort uri port))
    (and transport (.setTransportParam uri transport))
    uri))

(defn tel-uri
  "Create a new TelURI object with a phone number.

  (tel-uri 12345678) or (tel-uri \"12345678\")"
  [phone-number]
  (.createTelURL factory (str phone-number)))

(defn address
  "Create a new Address object using a URI.
  It useful to create the To header etc."
  ([^URI uri]
    {:post [(instance? Address %)]}
    (.createAddress factory uri))
  ([^URI uri display-name]
    {:post [(instance? Address %)]}
    (.createAddress factory display-name uri)))

(defn sip-address
  "A convenient way to create a new Address object that limited to sip uri.

  (sip-address \"localhost\" :user \"tom\" :display-name \"Tom\" :port 5060 :transport \"udp\")"
  [host & {:keys [user port transport display-name] :as options}]
  (let [uri (apply sip-uri host (flatten (seq options)))]
    (.createAddress factory display-name uri)))

(defn address?
  "Check the 'obj' is an instance of javax.sip.Address."
  [object]
  (instance? Address object))

(defn uri?
  "Check the 'obj' is an instance of javax.sip.URI."
  [object]
  (instance? URI object))
