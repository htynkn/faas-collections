echo "Processing" + $1
node deploy.js $1
cd $1
npm install
fun deploy