.PHONY: trace amd64 arm64 java clean amd64-docker arm64-docker default-docker

UNAME_P := $(shell uname -p 2> /dev/null || "x86_64")
DEFAULT := "amd64"
ifneq ($(filter arm%,$(UNAME_P)),)
	DEFAULT := "aarch64"
endif

trace:
	# ./gradlew shadowJar
	$(JAVA_HOME)/bin/java -agentlib:native-image-agent=config-merge-dir=native-image-config \
		-jar build/libs/xls2json-*-all.jar \
		src/test/resources/sample.xlsx \
		src/test/resources/sample.xls \
		src/test/resources/sampleTwoSheets.xls \
		src/test/resources/empty.xls \
		> /dev/null
	$(JAVA_HOME)/bin/java -agentlib:native-image-agent=config-merge-dir=native-image-config \
		-jar build/libs/xls2json-*-all.jar --pretty \
		src/test/resources/sample.xlsx \
		src/test/resources/sample.xls \
		src/test/resources/sampleTwoSheets.xls \
		src/test/resources/empty.xls \
		> /dev/null


amd64-docker:
	docker build --build-arg ARCH=amd64/ \
				 --build-arg GVM_PLATFORM=amd64 \
				 -t xls2json-builder:amd64 .

arm64-docker:
	docker build --build-arg ARCH=arm64v8/ \
				 --build-arg GVM_PLATFORM=aarch64 \
				 -t xls2json-builder:arm64v8 .

default-docker:
	docker build --build-arg GVM_PLATFORM=$(DEFAULT) \
				 -t xls2json-builder:default .


amd64:
	docker run --platform linux/amd64 -it --rm \
			   -v $(PWD):/app \
			   xls2json-builder:amd64 \
			   bash -c "./gradlew nativeImage"
	mkdir -p dist
	cp build/executable/xls2json dist/xls2json-amd64


arm64:
	docker run --platform linux/arm64 -it --rm \
			    -v $(PWD):/app xls2json-builder:arm64v8 \
			    bash -c "./gradlew nativeImage"
	mkdir -p dist
	cp build/executable/xls2json dist/xls2json-arm64

java:
	docker run -it --rm \
			   -v $(PWD):/app \
			   xls2json-builder:default \
			   bash -c "./gradlew build"
	mkdir -p dist
	cp build/distributions/* dist/
	cp build/libs/*-all.jar dist/

clean:
	docker run -it --rm \
			   -v $(PWD):/app \
			   debian:10-slim \
			   bash -c "rm -rf .gradle build dist"

dist:
	$(MAKE) default-docker arm64-docker amd64-docker clean java amd64 arm64
