build-img:
	docker build -t fc-go-runtime  -f build-image/Dockerfile build-image

build: build-img
	docker run --rm -v $$(pwd):/tmp fc-go-runtime bash -c "cd /tmp/code/ && go build . && mv code bootstrap"
	chmod +x code/bootstrap

deploy: build
	fun deploy -y