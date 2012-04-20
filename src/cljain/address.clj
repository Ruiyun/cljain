(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.address
  (:use cljain.core)
  (:import [javax.sip SipFactory]
           [javax.sip.address AddressFactory Address SipURI]))

(def ^{:doc "place doc string here"
       :added "0.2.0"
       :private true}
  factory (.createAddressFactory sip-factory))

(defn uri
  "place doc string here"
  {:added "0.2.0"}
  [host & options]
  {:pre [(even? (count options))]
   :post [(instance? SipURI %)]}
  (let [{:keys [user port transport]} (apply hash-map options)
        uri (.createSipURI factory nil host)]
    (and user (.setUser uri user))
    (and port (.setPort uri port))
    (and transport (.setTransportParam uri transport))
    uri))

(defn address
  "place doc string here"
  {:added "0.2.0"}
  ([uri]
    {:post [(instance? Address %)]}
    (.createAddress factory uri))
  ([uri display-name]
    {:post [(instance? Address %)]}
    (.createAddress factory display-name uri)))
