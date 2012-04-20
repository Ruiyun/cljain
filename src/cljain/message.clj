(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.message
  (:use cljain.core)
  (:require [cljain.header :as header]
            [cljain.address :as addr])
  (:import [javax.sip SipFactory SipProvider]
           [javax.sip.message MessageFactory Request]))

(def ^{:doc "place doc string here"
       :added "0.2.0"
       :private true}
  factory (.createMessageFactory sip-factory))

(defn request
  "place doc string here"
  {:added "0.2.0"}
  [ctx-name method req-uri & headers]
  (let [ctx (get-ctx ctx-name)
        {:keys [host port transport]} (:listening-point ctx)
        headers (apply hash-map (mapcat #(vector (.getName %) %) headers))]
    (doto (.createRequest factory
            req-uri method
            (get headers "Call-ID" (header/call-id-by-ctx ctx-name))
            (get headers "CSeq" (header/c-seq 1 method))
            (get headers "From" (header/from-by-ctx ctx-name))
            (get headers "To" (header/to (addr/address req-uri) nil))
            (get headers "Via" [])
            (get headers "Max-Forwards" (header/max-forwards 70)))
      (.setHeader (get headers "Contact"
                    (header/contact (addr/address (addr/uri host :port port
                                                    :transport transport)))))
      )))

(defn response
  "place doc string here"
  {:added "0.2.0"}
  [status-code reqest]
  (.createResponse factory status-code reqest))


