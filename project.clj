(defproject guestbookv3 "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[ch.qos.logback/logback-classic "1.2.3"]
                 [cheshire "5.9.0"]
                 [clojure.java-time "0.3.2"]
                 [conman "0.8.4"]
                 [cprop "0.1.15"]
                 [expound "0.8.3"]
                 [funcool/struct "1.4.0"]
                 [luminus-http-kit "0.1.6"]
                 [luminus-migrations "0.6.6"]
                 [luminus-transit "0.1.2"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [markdown-clj "1.10.1"]
                 [metosin/muuntaja "0.6.6"]
                 [metosin/reitit "0.3.10"]
                 [metosin/ring-http-response "0.9.1"]
                 [mount "0.1.16"]
                 [nrepl "0.6.0"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "0.4.2"]
                 [org.clojure/tools.logging "0.5.0"]
                 [org.webjars.npm/bulma "0.8.0"]
                 [org.webjars.npm/material-icons "0.3.1"]
                 [org.webjars/webjars-locator "0.38"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.8.0"]
                 [ring/ring-defaults "0.3.2"]
                 [selmer "1.12.18"]
                 [org.clojure/clojurescript "1.10.773" :scope "provided"]
                 [reagent "0.10.0"]
                 [cljs-ajax "0.8.0"]
                 [re-frame "1.0.0"]
                 [thheller/shadow-cljs "2.11.4"]
                 [com.google.javascript/closure-compiler-unshaded "v20200830"]
                 [com.taoensso/sente "1.15.0"]
                 [org.postgresql/postgresql "42.2.6"]
                 [buddy "2.0.0"]]

  :min-lein-version "2.0.0"
  
  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot guestbookv3.core

  :plugins []
  ;; :cljsbuild
  ;; {:builds
  ;;  {:app {:source-paths ["src/cljs" "src/cljc"]
  ;;         :compiler {:output-to "target/cljsbuild/public/js/app.js"
  ;;                    :output-dir "target/cljsbuild/public/js/out"
  ;;                    :main "guestbookv3.core"
  ;;                    :asset-path "/js/out"
  ;;                    :optimizations :none
  ;;                    :source-map true
  ;;                    "pretty-print" true}}}}

  ;; :clean-targets
  ;; ^{:protect false}
  ;; [:target-path
  ;;  [:cljsbuild :builds :app :compiler :output-dir]
  ;;  [:cljsbuild :builds :app :compiler :output-to]]


  :profiles
  {:uberjar {:omit-source true
             :aot :all
             :uberjar-name "guestbookv3.jar"
             :source-paths ["env/prod/clj" "env/prod/cljs" "env/prod/cljc"]
             :resource-paths ["env/prod/resources"]
             :prep-tasks ["compile" ["run" "-m" "shadow.cljs.devtools.cli" "release" "app"]]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:jvm-opts ["-Dconf=dev-config.edn"]
                  :dependencies [[binaryage/devtools "0.9.10"]
                                 [pjstadig/humane-test-output "0.10.0"]
                                 [prone "2019-07-08"]
                                 [ring/ring-devel "1.8.0"]
                                 [ring/ring-mock "0.4.0"]
                                 [day8.re-frame/re-frame-10x "0.7.0"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                 [jonase/eastwood "0.3.5"]]
                  
                  :source-paths ["env/dev/clj" "env/dev/cljs" "env/dev/cljc"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:jvm-opts ["-Dconf=test-config.edn"]
                  :resource-paths ["env/test/resources"]}
   :profiles/dev {}
   :profiles/test {}})
