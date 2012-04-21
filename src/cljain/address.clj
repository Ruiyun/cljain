(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.address
  (:use [cljain.core :only [sip-factory]])
  (:import [javax.sip SipFactory]
           [javax.sip.address AddressFactory URI SipURI Address]))

(def ^{:doc "place doc string here"
       :added "0.2.0"
       :private true}
  factory (.createAddressFactory sip-factory))

(defn sip-uri
  "Create a new SipURI object.
  It is useful to create the sip ReqURI or Address.

  (sip-uri \"localhost\" :port 5060 :transport \"udp\" :user \"tom\")"
  {:added "0.1.0"}
  [host & options]
  {:pre [(even? (count options))]
   :post [(instance? SipURI %)]}
  (let [{:keys [user port transport]} (apply hash-map options)
        uri (.createSipURI factory nil host)]
    (and user (.setUser uri user))
    (and port (.setPort uri port))
    (and transport (.setTransportParam uri transport))
    uri))

(defn tel-uri
  "Create a new TelURI object with a phone number.

  (tel-uri 12345678) or (tel-uri \"12345678\")"
  {:added "0.2.0"}
  [phone-number]
  (.createTelURL factory (str phone-number)))

(defn address
  "Create a new Address object using a URI.
  It useful to create the To header etc."
  {:added "0.1.0"}
  ([^URI uri]
    {:post [(instance? Address %)]}
    (.createAddress factory uri))
  ([^URI uri display-name]
    {:post [(instance? Address %)]}
    (.createAddress factory display-name uri)))
