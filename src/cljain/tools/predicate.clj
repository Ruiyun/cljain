(ns ^{:author "ruiyun"
      :added "0.2.0"
      :deprecated "0.4.0"}
  cljain.tools.predicate)

(defn in?
  "Chekc whether v is in the coll or not."
  {:added "0.2.0"}
  [v coll]
  (some #(= % v) coll))

(defmacro legal-option?
  "place doc string here"
  {:added "0.2.0"}
  [required? & decl]
  (let [options     (first decl)
        decl        (next decl)
        option-key  (first decl)
        decl        (next decl)
        modifier    (if (= :by (first decl))
      (fnext decl)
      identity)
        decl        (if (= :by (first decl))
      (nnext decl)
      decl)
        f           (first decl)
        args        (next decl)]
    `(let [popts# (partition-by (partial = ~option-key) ~options)
           opt_exist?# (> (count popts#) 1)]
       (if opt_exist?#
         (let [opt# (~modifier (first (last popts#)))]
           (~f opt# ~@args))
         (not ~required?)))))

(defmacro check-required
  "place doc string here"
  {:arglists '([options option-key :by? option-modifier? f & args])
   :added "0.2.0"}
  [& decl]
  `(legal-option? true ~@decl))

(defmacro check-optional
  "place doc string here"
  {:arglists '([options option-key :by? option-modifier? f & args])
   :added "0.2.0"}
  [& decl]
  `(legal-option? false ~@decl))
