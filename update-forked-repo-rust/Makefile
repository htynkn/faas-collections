build-img:
	docker build -t fc-rust-runtime  -f build-image/Dockerfile build-image

build: build-img
	docker run --rm -v $$(pwd):/tmp fc-rust-runtime bash -c "cd /tmp/code/ && cargo build --release && mv target/release/code bootstrap && chmod +x bootstrap"

deploy: build
	fun deploy -y