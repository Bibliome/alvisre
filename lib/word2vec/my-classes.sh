#make
#if [ ! -e text8 ]; then
#  wget http://mattmahoney.net/dc/text8.zip -O text8.gz
#  gzip -d text8.gz -f
#fi
time ./word2vec -train biotopes-combined.txt -output classes.txt -cbow 0 -size 200 -window 5 -negative 0 -hs 1 -sample 1e-3 -threads 12 -classes 500
sort classes.txt -k 2 -n > classes.sorted.txt
echo The word classes were saved to file classes.sorted.txt
mkdir classes/
classno=`tail -1 classes.sorted.txt |cut -d" " -f 2`
for (( c=0; c<=$classno; c++ ))
do  
	echo $c
	grep " $c$" classes.sorted.txt | cut -d" "  -f 1 > classes/$c.txt
done
echo Class files written in classes/ directory
FILES=classes/*txt
for f in $FILES
do 
	echo -e "\t<SemanticClass>\n\t\t<words>" >> semclasses.txt
	echo -en "\t\t" >> semclasses.txt
	while read line
		do echo -en "$line,"  >> semclasses.txt
	done< $f
echo -e "\b\n\t\t</words>\n\t</SemanticClass>" >>semclasses.txt
done

