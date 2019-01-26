var request = require("request");
var async = require("async")

var targets = { 
    "diamondblack/spring-boot": "spring-projects/spring-boot",
    "diamondblack/incubator-dubbo": "apache/incubator-dubbo",
    "diamondblack/incubator-openwhisk": "apache/incubator-openwhisk",
    "htynkn/dubbo-benchmark": "dubbo/dubbo-benchmark",
    "htynkn/spark": "apache/spark",
    "htynkn/incubator-dubbo": "apache/incubator-dubbo",
    "htynkn/incubator-dubbo-ops": "apache/incubator-dubbo-ops",
    "htynkn/spring-boot": "spring-projects/spring-boot",
};

module.exports.handler = function (event, context, callback) {
    var options = {
        headers: {
            "User-Agent": "update-forked-repo",
            "Authorization": "token " + process.env.GITHUB_TOKEN
        }
    };
    var execResult = [];
    async.forEachOf(targets, function (value, key, eachCallback) {
        async.waterfall([
            function (cb) {
                request.get("https://api.github.com/repos/" + value + "/branches/master", options, function (error, response, body) {
                    if (error) {
                        cb(error)
                    }
                    var sha = JSON.parse(body).commit.sha;
                    cb(null, sha);
                }
                )
            },
            function (sha, cb) {
                request.patch("https://api.github.com/repos/" + key + "/git/refs/heads/master", {
                    headers: options.headers,
                    json: {
                        "sha": sha,
                        "force": false
                    }
                }, function (error, response, body) {
                    if (error) {
                        cb(error)
                    }
                    cb(null, body);
                })
            }], function (err, result) {
                if (err) {
                    eachCallback(err)
                } else {
                    execResult.push({
                        target: key,
                        time: new Date(),
                        result: result.object,
                    });
                    eachCallback();
                }
            }
        );
    }, function (err) {
        if (err) {
            callback(err)
        } else {
            console.log(execResult);
            callback(null, execResult);
        }
    })
};