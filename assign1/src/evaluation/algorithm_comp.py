import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import dataframe_image as dfi
import os

####################### LOADING CSV FILE AND CONFIGURATIONS

working_dir = str(os.path.dirname(__file__))

results_1_cpp = pd.read_csv(working_dir + '/processed_docs/1_cpp.csv', header = 0, sep = ',')
results_1_java = pd.read_csv(working_dir + '/processed_docs/1_java.csv', header = 0, sep = ',')
results_2a_cpp = pd.read_csv(working_dir + '/processed_docs/2a_cpp.csv', header = 0, sep = ',')
results_2a_java = pd.read_csv(working_dir + '/processed_docs/2a_java.csv', header = 0, sep = ',')
results_2b_cpp = pd.read_csv(working_dir + '/processed_docs/2b_cpp.csv', header = 0, sep = ',')
results_3_cpp = pd.read_csv(working_dir + '/processed_docs/3_cpp.csv', header = 0, sep = ',')
results_3_java = pd.read_csv(working_dir + '/processed_docs/3_java.csv', header = 0, sep = ',')

############# crating the necessary variables

# 1 cpp
cpp1_matrix_size = results_1_cpp['Matrix Size'] # matrix size
cpp1_time = results_1_cpp['Avg Time'] # time to run
cpp1_l1_dcm = results_1_cpp['Avg L1 DCM'] # number of l1 cache misses
cpp1_l2_dcm = results_1_cpp['Avg L2 DCM'] # number of l2 cache misses
cpp1_tot_ins = results_1_cpp['Avg TOT INS'] # number of instructions exec
cpp1_tot_cyc = results_1_cpp['Avg TOT CYC'] # nuber of cycles exec
cpp1_gigaflops = results_1_cpp['Avg GigaFlops'] # gigaflops

# 2a cpp
cpp2a_matrix_size = results_2a_cpp['Matrix Size'] # matrix size
cpp2a_time = results_2a_cpp['Avg Time'] # time to run
cpp2a_l1_dcm = results_2a_cpp['Avg L1 DCM'] # number of l1 cache misses
cpp2a_l2_dcm = results_2a_cpp['Avg L2 DCM'] # number of l2 cache misses
cpp2a_tot_ins = results_2a_cpp['Avg TOT INS'] # number of instructions exec
cpp2a_tot_cyc = results_2a_cpp['Avg TOT CYC'] # nuber of cycles exec
cpp2a_gigaflops = results_2a_cpp['Avg GigaFlops'] # gigaflops

# 2b cpp
cpp2b_matrix_size = results_2b_cpp['Matrix Size'] # matrix size
cpp2b_time = results_2b_cpp['Avg Time'] # time to run
cpp2b_l1_dcm = results_2b_cpp['Avg L1 DCM'] # number of l1 cache misses
cpp2b_l2_dcm = results_2b_cpp['Avg L2 DCM'] # number of l2 cache misses
cpp2b_tot_ins = results_2b_cpp['Avg TOT INS'] # number of instructions exec
cpp2b_tot_cyc = results_2b_cpp['Avg TOT CYC'] # nuber of cycles exec
cpp2b_gigaflops = results_2b_cpp['Avg GigaFlops'] # gigaflops

# 3 cpp
cpp3_matrix_size = results_3_cpp['Matrix Size'] # matrix size
cpp3_block_size = results_3_cpp["Block Size"] # block size
cpp3_time = results_3_cpp['Avg Time'] # time to run
cpp3_l1_dcm = results_3_cpp['Avg L1 DCM'] # number of l1 cache misses
cpp3_l2_dcm = results_3_cpp['Avg L2 DCM'] # number of l2 cache misses
cpp3_tot_ins = results_3_cpp['Avg TOT INS'] # number of instructions exec
cpp3_tot_cyc = results_3_cpp['Avg TOT CYC'] # nuber of cycles exec
cpp3_gigaflops = results_3_cpp['Avg GigaFlops'] # gigaflops

# 1 java
java1_matrix_size = results_1_java['Matrix Size'] # matrix size
java1_time = results_1_java['Avg Time'] # time to run
java1_gigaflops = results_1_java['Avg GigaFlops'] # gigaflops


# 2a java
java2a_matrix_size = results_2a_java['Matrix Size'] # matrix size
java2a_time = results_2a_java['Avg Time'] # time to run
java2a_gigaflops = results_2a_java['Avg GigaFlops'] # gigaflops


# 3 java
java3_matrix_size = results_3_java['Matrix Size'] # matrix size
java3_block_size = results_3_java['Block Size'] # block size
java3_time = results_3_java['Avg Time'] # time to run
java3_gigaflops = results_3_java['Avg GigaFlops'] # gigaflops

###################### MAKING THE GRAPHS AND PLOTS

### experience 1  (cpp vs java) #######################################################################################################################

## time

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp1_matrix_size,cpp1_time, label = 'C++') # cpp
ax.plot(java1_matrix_size,java1_time, label='Java') # java

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("Average Time")
ax.set_title("Execution time depending on matrix size - experiment 1")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp1_times.png', bbox_inches='tight') # save png

## gigaflops

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp1_matrix_size,cpp1_gigaflops, label = 'C++') # cpp
ax.plot(java1_matrix_size,java1_gigaflops, label='Java') # java

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("Average Gigaflops")
ax.set_title("Gigaflops depending on matrix size - experiment 1")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp1_gigaflops.png', bbox_inches='tight') # save png

### experience 1 vs experience two (cpp) ###########################################################################################################

## time

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp1_matrix_size,cpp1_time, label = 'C++ EXP. 1') # cpp
ax.plot(cpp2a_matrix_size,cpp2a_time, label='C++ EXP. 2')

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("Average Time")
ax.set_title("Execution time depending on matrix size in Exp 1 and Exp 2 (C++)")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp1_exp2_cpp_time.png', bbox_inches='tight') # save png

## gigaflops

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp1_matrix_size,cpp1_gigaflops, label = 'C++ EXP. 1') # cpp
ax.plot(cpp2a_matrix_size,cpp2a_gigaflops, label='C++ EXP. 2')

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("Average Gigaflops")
ax.set_title("Gigaflops depending on matrix size in Exp 1 and Exp 2 (C++)")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp1_exp2_cpp_gigaflops.png', bbox_inches='tight') # save png

## l1 cache misses

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp1_matrix_size,cpp1_l1_dcm, label = 'C++ EXP. 1') # cpp
ax.plot(cpp2a_matrix_size,cpp2a_l1_dcm, label='C++ EXP. 2')

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("L1 Data Cache Misses")
ax.set_title("L1 Data Cache Misses depending on matrix size in Exp 1 and Exp 2 (C++)")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp1_exp2_cpp_l1_dcm.png', bbox_inches='tight') # save png

## l2 cache misses

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp1_matrix_size,cpp1_l2_dcm, label = 'C++ EXP. 1') # cpp
ax.plot(cpp2a_matrix_size,cpp2a_l2_dcm, label='C++ EXP. 2')

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("L2 Data Cache Misses")
ax.set_title("L2 Data Cache Misses depending on matrix size in Exp 1 and Exp 2 (C++)")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp1_exp2_cpp_l2_dcm.png', bbox_inches='tight') # save png

## total instructions made

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp1_matrix_size,cpp1_tot_ins, label = 'C++ EXP. 1') # cpp
ax.plot(cpp2a_matrix_size,cpp2a_tot_ins, label='C++ EXP. 2')

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("Number of instructions")
ax.set_title("Number of instructions depending on matrix size in Exp 1 and Exp 2 (C++)")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp1_exp2_cpp_tot_ins.png', bbox_inches='tight') # save png

## total cycles made

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp1_matrix_size,cpp1_tot_cyc, label = 'C++ EXP. 1') # cpp
ax.plot(cpp2a_matrix_size,cpp2a_tot_cyc, label='C++ EXP. 2')

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("Number of cycles")
ax.set_title("Number of cycles depending on matrix size in Exp 1 and Exp 2 (C++)")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp1_exp2_cpp_tot_cyc.png', bbox_inches='tight') # save png

### experience 2 (cpp vs java) #######################################################################################################################

## time

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp2a_matrix_size,cpp2a_time, label = 'C++') # cpp
ax.plot(java2a_matrix_size,java2a_time, label='Java')

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("Average Time")
ax.set_title("Execution time depending on matrix size - experiment 2")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp2_times.png', bbox_inches='tight') # save png

## gigaflops

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp2a_matrix_size,cpp2a_gigaflops, label = 'C++') # cpp
ax.plot(java2a_matrix_size,java2a_gigaflops, label='Java')

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("Average Gigaflops")
ax.set_title("Gigaflops depending on matrix size - experiment 2")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp2_gigaflops.png', bbox_inches='tight') # save png

### experience 2a vs experience 2b (cpp) #############################################################################################################

## time

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp2a_matrix_size+cpp2b_matrix_size,cpp2a_time+cpp2b_time, label = 'C++ EXP. 2') # cpp

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("Average Time")
ax.set_title("Execution time depending on matrix size in Exp. 2a + Exp. 2b (C++)")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp2_cpp_time.png', bbox_inches='tight') # save png

## gigaflops

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp2a_matrix_size+cpp2b_matrix_size,cpp2a_gigaflops+cpp2b_gigaflops, label = 'C++ EXP. 2') # cpp

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("Average Gigaflops")
ax.set_title("Gigaflops depending on matrix size in Exp. 2a + Exp. 2b (C++)")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp2_cpp_gigaflops.png', bbox_inches='tight') # save png

## l1 cache misses

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp2a_matrix_size+cpp2b_matrix_size,cpp2a_l1_dcm+cpp2b_l1_dcm, label = 'C++ EXP. 2') # cpp

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("L1 Data Cache Misses")
ax.set_title("L1 Data Cache Misses depending on matrix size in Exp. 2a + Exp. 2b (C++)")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp2_cpp_l1_dcm.png', bbox_inches='tight') # save png

## l2 cache misses

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp2a_matrix_size+cpp2b_matrix_size,cpp2a_l2_dcm+cpp2b_l2_dcm, label = 'C++ EXP. 2') # cpp

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("L2 Data Cache Misses")
ax.set_title("L2 Data Cache Misses depending on matrix size in Exp. 2a + Exp. 2b (C++)")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp2_cpp_l2_dcm.png', bbox_inches='tight') # save png

## total instructions made

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp2a_matrix_size+cpp2b_matrix_size,cpp2a_tot_ins+cpp2b_tot_ins, label = 'C++ EXP. 2') # cpp

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("Number of instructions")
ax.set_title("Total instructions depending on matrix size in Exp. 2a + Exp. 2b (C++)")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp2_cpp_tot_ins.png', bbox_inches='tight') # save png

## total cycles made

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp2a_matrix_size+cpp2b_matrix_size,cpp2a_tot_cyc+cpp2b_tot_cyc, label = 'C++ EXP. 2') # cpp

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("Number of cycles")
ax.set_title("Number of cycles depending on matrix size in Exp. 2a + Exp. 2b (C++)")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp2_cpp_tot_cyc.png', bbox_inches='tight') # save png

### EXPERIENCE 3 CPP ##################################################################################################################################

#separating blocks

cpp3_block_128 = results_3_cpp[results_3_cpp['Block Size'] == 128]
cpp3_block_256 = results_3_cpp[results_3_cpp['Block Size'] == 256]
cpp3_block_512 = results_3_cpp[results_3_cpp['Block Size'] == 512]

# time graph

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp3_block_128['Matrix Size'],cpp3_block_128['Avg Time'], label = 'C++: Block Size of 128') # block 128
ax.plot(cpp3_block_256['Matrix Size'],cpp3_block_256['Avg Time'], label = 'C++: Block Size of 256') # block 256
ax.plot(cpp3_block_512['Matrix Size'],cpp3_block_512['Avg Time'], label = 'C++: Block Size of 512') # block 512

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("Average Time")
ax.set_title("Execution time depending on matrix size - Experiment 3")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp3_times.png', bbox_inches='tight') # save png

# gigaflops graph

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp3_block_128['Matrix Size'],cpp3_block_128['Avg GigaFlops'], label = 'C++: Block Size of 128') # block 128
ax.plot(cpp3_block_256['Matrix Size'],cpp3_block_256['Avg GigaFlops'], label = 'C++: Block Size of 256') # block 256
ax.plot(cpp3_block_512['Matrix Size'],cpp3_block_512['Avg GigaFlops'], label = 'C++: Block Size of 512') # block 512

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("Gigaflops")
ax.set_title("Gigaflops depending on matrix size - Experiment 3")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp3_gigaflops.png', bbox_inches='tight') # save png

# l1 dcm graph

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp3_block_128['Matrix Size'],cpp3_block_128['Avg L1 DCM'], label = 'C++: Block Size of 128') # block 128
ax.plot(cpp3_block_256['Matrix Size'],cpp3_block_256['Avg L1 DCM'], label = 'C++: Block Size of 256') # block 256
ax.plot(cpp3_block_512['Matrix Size'],cpp3_block_512['Avg L1 DCM'], label = 'C++: Block Size of 512') # block 512

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("L1 Data Cache Misses")
ax.set_title("L1 Data Cache Misses depending on matrix size - Experiment 3")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp3_l1_dcm.png', bbox_inches='tight') # save png

# l2 dcm graph

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp3_block_128['Matrix Size'],cpp3_block_128['Avg L2 DCM'], label = 'C++: Block Size of 128') # block 128
ax.plot(cpp3_block_256['Matrix Size'],cpp3_block_256['Avg L2 DCM'], label = 'C++: Block Size of 256') # block 256
ax.plot(cpp3_block_512['Matrix Size'],cpp3_block_512['Avg L2 DCM'], label = 'C++: Block Size of 512') # block 512

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("L2 Data Cache Misses")
ax.set_title("L2 Data Cache Misses depending on matrix size - Experiment 3")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp3_l2_dcm.png', bbox_inches='tight') # save png

# tot ins graph

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp3_block_128['Matrix Size'],cpp3_block_128['Avg TOT INS'], label = 'C++: Block Size of 128') # block 128
ax.plot(cpp3_block_256['Matrix Size'],cpp3_block_256['Avg TOT INS'], label = 'C++: Block Size of 256') # block 256
ax.plot(cpp3_block_512['Matrix Size'],cpp3_block_512['Avg TOT INS'], label = 'C++: Block Size of 512') # block 512

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("Number of Instructions")
ax.set_title("Number of instructions depending on matrix size - Experiment 3")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp3_tot_ins.png', bbox_inches='tight') # save png

# tot cyc graph

fig, ax = plt.subplots(figsize=(10,7))
ax.plot(cpp3_block_128['Matrix Size'],cpp3_block_128['Avg TOT CYC'], label = 'C++: Block Size of 128') # block 128
ax.plot(cpp3_block_256['Matrix Size'],cpp3_block_256['Avg TOT CYC'], label = 'C++: Block Size of 256') # block 256
ax.plot(cpp3_block_512['Matrix Size'],cpp3_block_512['Avg TOT CYC'], label = 'C++: Block Size of 512') # block 512

#labels
ax.set_xlabel("Matrix Size")
ax.set_ylabel("Number of Cycles")
ax.set_title("Number of cycles depending on matrix size - Experiment 3")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp3_tot_cyc.png', bbox_inches='tight') # save png

#### BAR PLOTS EXPERIMENT 3 ##################################

# time

index=np.arange(len(cpp3_block_128['Matrix Size']))
bar_width = 0.20
fig, ax = plt.subplots()
bar_128 = ax.bar(index - bar_width ,cpp3_block_128['Avg Time'], bar_width, label='Block Size 128')
bar_256 = ax.bar(index ,cpp3_block_256['Avg Time'], bar_width, label='Block Size 256')
bar_512 = ax.bar(index + bar_width ,cpp3_block_512['Avg Time'], bar_width, label='Block Size 512')

ax.set_xticks(index)
ax.set_xticklabels(cpp3_block_128['Matrix Size'])
ax.set_yticks(cpp3_block_128['Avg Time'])
ax.set_yticklabels(cpp3_block_128['Avg Time'])

ax.set_xlabel("Matrix Size")
ax.set_ylabel("Time")
ax.set_title("Time depending on matrix size - Experiment 3")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp3_bar_time.png', bbox_inches='tight') # save png

# gigaflop

index=np.arange(len(cpp3_block_128['Matrix Size']))
bar_width = 0.20
fig, ax = plt.subplots()
bar_128 = ax.bar(index - bar_width ,cpp3_block_128['Avg GigaFlops'], bar_width, label='Block Size 128')
bar_256 = ax.bar(index ,cpp3_block_256['Avg GigaFlops'], bar_width, label='Block Size 256')
bar_512 = ax.bar(index + bar_width ,cpp3_block_512['Avg GigaFlops'], bar_width, label='Block Size 512')

ax.set_xticks(index)
ax.set_xticklabels(cpp3_block_128['Matrix Size'])
ax.set_yticks(cpp3_block_128['Avg GigaFlops'])
ax.set_yticklabels(cpp3_block_128['Avg GigaFlops'])

ax.set_xlabel("Matrix Size")
ax.set_ylabel("Gigaflops")
ax.set_title("Gigaflops depending on matrix size - Experiment 3")
ax.legend()

plt.savefig(working_dir+'/produced_docs/exp3_bar_gigaflops.png', bbox_inches='tight') # save png
