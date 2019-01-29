var folderName = process.argv[2];
console.log('Start processing '+ folderName)

var envList = ["GITHUB_TOKEN"]
console.log('Replacing env config in template file')

var fs = require('fs')
var filePath = folderName + '/template.yml'


for (var envNameIndex in envList){
    var envName = envList[envNameIndex];

    console.log("Replacing "+ envName)
    fs.readFile(filePath, 'utf8', function (err,data) {
        if (err) {
          return console.log(err);
        }
        var result = data.replace("env["+envName+"]", process.env[envName]);
      
        fs.writeFile(filePath, result, 'utf8', function (err) {
           if (err) return console.log(err);
        });
      });
}

