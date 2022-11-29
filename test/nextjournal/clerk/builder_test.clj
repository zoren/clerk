(ns nextjournal.clerk.builder-test
  (:require [babashka.fs :as fs]
            [clojure.java.browse :as browse]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [nextjournal.clerk.builder :as builder])
  (:import (java.io File)))

(deftest url-canonicalize
  (testing "canonicalization of file components into url components"
    (let [dice (str/join (File/separator) ["notebooks" "dice.clj"])]
      (is (= (#'builder/path-to-url-canonicalize dice) (str/replace dice  (File/separator) "/"))))))

(deftest static-app
  (let [url* (volatile! nil)]
    (with-redefs [clojure.java.browse/browse-url (fn [path]
                                                   (vreset! url* path))]
      (testing "browser receives canonical url in this system arch"
        (fs/with-temp-dir [temp {}]
          (let [expected (-> (str/join (java.io.File/separator) [(.toString temp) "index.html"])
                             (str/replace (java.io.File/separator) "/"))]
            (builder/build-static-app! {:browse? true
                                        :paths ["notebooks/hello.clj"]
                                        :out-path temp})
            (is (= expected @url*))))))))

(deftest expand-paths
  (testing "expands glob patterns"
    (let [paths (builder/expand-paths {:paths ["notebooks/*clj"]})]
      (is (> (count paths) 25))
      (is (every? #(str/ends-with? % ".clj") paths))))

  (testing "supports index"
    (is (= ["book.clj"] (builder/expand-paths {:index "book.clj"}))))

  (testing "supports index"
    (is (= ["book.clj"] (builder/expand-paths {:paths ["book.clj"]}))))

  (testing "invalid args"
    (is (thrown? Exception (builder/expand-paths {})))
    (is (thrown? Exception (builder/expand-paths {:paths-fn 'foo})))
    (is (thrown? Exception (builder/expand-paths {:paths-fn "hi"})))
    (is (thrown? Exception (builder/expand-paths {:index ["book.clj"]})))))

(deftest process-build-opts
  (testing "assigns index when only one path is given"
    (is (= (str (fs/file "notebooks" "rule_30.clj"))
           (:index (builder/process-build-opts {:paths [(str (fs/file "notebooks" "rule_30.clj"))]
                                                :expand-paths? true})))))

  (testing "coerces index symbol arg and adds it to expanded-paths"
    (is (= ["book.clj"] (:expanded-paths (builder/process-build-opts {:index 'book.clj :expand-paths? true}))))))

