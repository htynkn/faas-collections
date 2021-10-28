#[macro_use]
extern crate log;
extern crate pretty_env_logger;

use warp::Filter;

mod handler;

#[tokio::main]
async fn main() {
    pretty_env_logger::init();
    // POST /invoke
    let route = warp::path!("invoke")
        .and(warp::post())
        .and_then(handler::run);

    info!("Starting server ...");
    warp::serve(route).run(([0, 0, 0, 0], 9000)).await;
}
