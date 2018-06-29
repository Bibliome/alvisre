if [ $4 = ref ]
	then
	echo "Copying a2 files";
	 for i in `ls -1 $1`; do grep -e "^R" $1/$i | grep -v Dependency |grep -v Anaphora > ref/"$i"2; done
	
	echo "Copying a1 files";
	 for i in `ls -1 $1`; do grep -e "^T" $1/$i |grep -v Word | grep -v Sentence | cut -d"|" -f1 > ref/"$i"1; done	
	
	
	echo "Copying txt files";
	cp $2/*txt ref/
fi
 
echo "Evaluation:"
 $3 --task 2 --a2-dir ref/ --a1-dir ref/ --pred-dir pred/  ref/*txt

