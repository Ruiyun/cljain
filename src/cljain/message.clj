(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.message
  (:use cljain.core)
  (:require [cljain.header :as header]
            [cljain.address :as addr])
  (:import [javax.sip SipFactory]
           [javax.sip.message MessageFactory Request]))

(def ^{:doc "place doc string here"
       :added "0.2.0"
       :private true}
  factory (.createMessageFactory sip-factory))

(defn request
  "place doc string here"
  {:added "0.2.0"}
  [method req-uri from to call-id & more-headers]
  (let [headers (apply hash-map (mapcat #(vector (.getName %) %) more-headers))]
    (doto (.createRequest factory
            req-uri method call-id from
            (get headers "CSeq" (header/c-seq 1 method))
            (get headers "To" (header/to (addr/address req-uri) nil))
            (get headers "Via" [])
            (get headers "Max-Forwards" (header/max-forwards 70))))))

(defn response
  "place doc string here"
  {:added "0.2.0"}
  [status-code reqest]
  (.createResponse factory status-code reqest))


