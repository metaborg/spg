#/bin/bash
shopt -s nullglob

for f in /tmp/pas/*.pas; do
    #echo "Processing $f file..";
    fpc $f | grep 'Syntax error'
done
