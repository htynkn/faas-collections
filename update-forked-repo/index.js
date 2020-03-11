var request = require("request");
var async = require("async");

var targets = [
  "htynkn/spark",
  "htynkn/rpc-benchmark",
  "htynkn/spring-boot",
  "htynkn/dubbo",
  "htynkn/dubbo-admin",
  "htynkn/dubbo-samples",
  "htynkn/dubbo-go",
  "htynkn/fish-redux"
];

module.exports.handler = function(event, context, callback) {
  var options = {
    headers: {
      "User-Agent": "update-forked-repo",
      Authorization: "token " + process.env.GITHUB_TOKEN
    }
  };
  var execResult = [];
  async.each(
    targets,
    function(value, eachCallback) {
      async.waterfall(
        [
          function(cb) {
            request.get(
              "https://api.github.com/repos/" + value,
              options,
              function(error, response, body) {
                if (error) {
                  cb(error);
                }
                const responseAsJson = JSON.parse(body);
                var defaultBranch = responseAsJson.default_branch;
                if (responseAsJson.parent != null) {
                  var parentName = responseAsJson.parent.full_name;
                  cb(null, {
                    defaultBranch: defaultBranch,
                    parentName: parentName
                  });
                } else {
                  cb("No parent, skip");
                }
              }
            );
          },
          function(info, cb) {
            request.get(
              "https://api.github.com/repos/" +
                info.parentName +
                "/branches/" +
                info.defaultBranch,
              options,
              function(error, response, body) {
                if (error) {
                  cb(error);
                } else {
                  var sha = JSON.parse(body).commit.sha;
                  cb(null, {
                    defaultBranch: info.defaultBranch,
                    sha: sha
                  });
                }
              }
            );
          },
          function(info, cb) {
            request.patch(
              "https://api.github.com/repos/" +
                value +
                "/git/refs/heads/" +
                info.defaultBranch,
              {
                headers: options.headers,
                json: {
                  sha: info.sha,
                  force: false
                }
              },
              function(error, response, body) {
                if (error) {
                  cb(error);
                }
                cb(null, body);
              }
            );
          }
        ],
        function(err, result) {
          if (err) {
            eachCallback(err);
          } else {
            execResult.push({
              target: value,
              time: new Date(),
              result: result.object
            });
            eachCallback();
          }
        }
      );
    },
    function(err) {
      if (err) {
        callback(err);
      } else {
        console.log(execResult);
        callback(null, execResult);
      }
    }
  );
};
