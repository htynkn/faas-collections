echo "Processing" + $1
node deploy.js $1
cd $1
if [ -e "package.json" ]; then
    echo "node project"
    npm install
fi
if [ -e "pom.xml" ]; then
    echo "java maven project"
    ./mvnw clean package
fi
fun deploy