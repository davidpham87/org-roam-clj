.PHONY:  doc
doc:
	clojure -m materiala.core -d doc src test
	mkdocs build

help:
	clojure -m materiala.core -h

.PHONY: http
http:
	cd docs/ && python3 -m http.server

.PHONY: test
test:
	clojure -Atest
