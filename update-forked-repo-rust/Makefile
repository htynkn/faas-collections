build-img:
	docker build -t fc-rust-runtime  -f build-image/Dockerfile build-image

build: build-img

deploy: build
	fun deploy -y