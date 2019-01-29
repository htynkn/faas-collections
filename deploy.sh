echo "Processing" + $1
node deploy.js $1
cd $1
if [ -e "package.json" ]; then
    npm install
fi
if [ -e "pom.xml" ]; then
    ./mvnw clean package
fi
fun deploy