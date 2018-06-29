echo "Copying a2 files";
 for i in `ls -1 /bibdev/travail/biotope_work/bionlp2013_task2/ccg/anaphora/output/dev`; do grep -e "^R" /bibdev/travail/biotope_work/bionlp2013_task2/ccg/anaphora/output/dev/$i | grep -v Dependency |grep -v Anaphora > ref/"$i"2; done

echo "Copying a1 files";
 for i in `ls -1 /bibdev/travail/biotope_work/bionlp2013_task2/ccg/anaphora/output/dev`; do grep -e "^T" /bibdev/travail/biotope_work/bionlp2013_task2/ccg/anaphora/output/dev/$i |grep -v Word | grep -v Sentence | cut -d"|" -f1 > ref/"$i"1; done


echo "Copying txt files";
cp ~dvalsamou/workspace/re-kernels/data/biotopes/official/dev/*txt ref/
 
echo "Evaluation:"
 ~dvalsamou/workspace/re-kernels/data/biotopes/BB.py --task 2 --a2-dir ref/ --a1-dir ref/ --pred-dir pred/  ref/*txt

