var folderName = process.argv[2];
console.log('Start processing ' + folderName)

var envList = ["GITHUB_TOKEN"]
console.log('Replacing env config in template file')

var fs = require('fs')
var filePath = folderName + '/template.yml'


var data = fs.readFileSync(filePath, 'utf8');
var result = data;
for (var envNameIndex in envList) {
    var envName = envList[envNameIndex];
    console.log("Replacing " + envName)
    result = result.replace("env[" + envName + "]", process.env[envName]);
}
fs.writeFileSync(filePath, result, 'utf8');