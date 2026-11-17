set -e
cd "$(dirname "$0")"
export PATH=/tmp/android/jdk/bin:$PATH
rm -rf build/test && mkdir -p build/test
javac -d build/test src/com/qwerty/keyboard/AutoSend.java test/AutoSendTest.java
java -cp build/test AutoSendTest
