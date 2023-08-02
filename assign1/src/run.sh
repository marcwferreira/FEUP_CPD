run_1_cpp=false
run_1_java=false
run_2a_cpp=false
run_2a_java=false
run_2b_cpp=false
run_3_cpp=true
run_3_java=true

if [ "$run_1_cpp" = true ] ; then
    echo -e "Language,Matrix Size,Time,L1 DCM,L2 DCM,TOT INS,TOT CYC" >> ./results/1_cpp.csv
    for i in {600..3000..400}
        do
            for j in {1..10..1}
                do
                    echo "C++ 1. $i x $i"
                    echo -n -e "C++,$i," >> ./results/1_cpp.csv
                    echo -e "1\n$i" | ./test >> ./results/1_cpp.csv

                done
        done
fi

if [ "$run_1_java" = true ] ; then
    echo -e "Language,Matrix Size,Time" >> ./results/1_java.csv
    for i in {600..3000..400}
        do
            for j in {1..10..1}
                do
                    echo "Java 1. $i x $i"
                    echo -n -e "Java," >> ./results/1_java.csv
                    echo -e "1\n$i" | java Main >> ./results/1_java.csv
                done
        done
fi

if [ "$run_2a_cpp" = true ] ; then
    echo -e "Language,Matrix Size,Time,L1 DCM,L2 DCM,TOT INS,TOT CYC" >> ./results/2a_cpp.csv
    for i in {600..3000..400}
        do
            for j in {1..10..1}
                do
                    echo " C++ 2a. $i x $i"
                    echo -n -e "C++,$i," >> ./results/2a_cpp.csv
                    echo -e "2\n$i" | ./test >> ./results/2a_cpp.csv
                done
        done
fi


if [ "$run_2a_java" = true ] ; then
    echo -e "Language,Matrix Size,Time" >> ./results/2a_java.csv
    for i in {600..3000..400}
        do
            for j in {1..10..1}
                do
                    echo "Java 2a. $i x $i"
                    echo -n -e "Java," >> ./results/2a_java.csv
                    echo -e "2\n$i" | java Main >> ./results/2a_java.csv
                done
        done
fi

if [ "$run_2b_cpp" = true ] ; then
    echo -e "Language,Matrix Size,Time,L1 DCM,L2 DCM,TOT INS,TOT CYC" >> ./results/2b_cpp.csv
    for i in {4096..10240..2048}
        do
            for j in {1..10..1}
                do
                    echo "C++ 2b. $i x $i"
                    echo -n -e "C++,$i," >> ./results/2b_cpp.csv
                    echo -e "2\n$i" | ./test >> ./results/2b_cpp.csv
                done
        done
fi


if [ "$run_3_cpp" = true ] ; then
    echo -e "Language,Matrix Size,Block Size,Time,L1 DCM, L2 DCM, TOT INS, TOT CYC" >> ./results/3_cpp.csv
    for i in {4096..10240..2048}
        do
            for j in 128 256 512
            do
                for k in {1..10..1}
                    do
                        echo "C++ 3. $i x $i (block size: $j)"
                        echo -n -e "C++,$i,$j," >> ./results/3_cpp.csv
                        echo -e "3\n$i\n$j" | ./test >> ./results/3_cpp.csv
                    done
            done
        done
fi


if [ "$run_3_java" = true ] ; then
    echo -e "Language,Matrix Size,Block Size,Time" >> ./results/3_java.csv
    for i in {4096..10240..2048}
        do
            for j in 128 256 512
            do
                for k in {1..10..1}
                    do
                        echo "Java 3. $i x $i (block size: $j)"
                        echo -n -e "Java," >> ./results/3_java.csv
                        echo -e "3\n$i\n$j" | java Main >> ./results/3_java.csv
                    done
            done
        done
fi
