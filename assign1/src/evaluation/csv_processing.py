import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import os

# function to calculate gigaflops
def gFlops(matrix_size, exec_time, is_block, block_size):
    if is_block:
        NumOps = 2 * (pow(matrix_size,3) * pow(block_size,3))      
    else:
        NumOps = 2 * pow(matrix_size,3)
    gflops = 1.0e-9 * NumOps / exec_time
    return gflops

####################### LOADING CSV FILE AND CONFIGURATIONS

working_dir = str(os.path.dirname(__file__))

prog_lang = input("Programming language?")
file_name = input("File name?")

results = pd.read_csv(working_dir + '/../results/'+file_name+'.csv', header = 0, sep = ',')

blocks_function = False
if 'Block Size' in results.columns:
    blocks_function = True

###################### MODIFYING THE TABLES WITH NECESSARY RESULTS

# calculating the gigaflops
results['gFlops'] = gFlops(results['Matrix Size'],results['Time'],False,0)

# aggregating multiples runs into one
if blocks_function:
    if prog_lang == 'cpp':
        results = results.groupby(["Matrix Size","Block Size"], as_index=False).agg(
                    min_time=pd.NamedAgg(column='Time', aggfunc='min'),
                    max_time=pd.NamedAgg(column='Time', aggfunc='max'),
                    avg_time=pd.NamedAgg(column='Time', aggfunc=np.mean),
                    std_time=pd.NamedAgg(column='Time', aggfunc=np.std),

                    min_gigaflops=pd.NamedAgg(column='gFlops', aggfunc='min'),
                    max_gigaflops=pd.NamedAgg(column='gFlops', aggfunc='max'),
                    avg_gigaflops=pd.NamedAgg(column='gFlops', aggfunc=np.mean),
                    std_gigaflops=pd.NamedAgg(column='gFlops', aggfunc=np.std),

                    min_l1_dcm=pd.NamedAgg(column='L1 DCM', aggfunc='min'),
                    max_l1_dcm=pd.NamedAgg(column='L1 DCM', aggfunc='max'),
                    avg_l1_dcm=pd.NamedAgg(column='L1 DCM', aggfunc=np.mean),
                    std_l1_dcm=pd.NamedAgg(column='L1 DCM', aggfunc=np.std),

                    min_l2_dcm=pd.NamedAgg(column='L2 DCM', aggfunc='min'),
                    max_l2_dcm=pd.NamedAgg(column='L2 DCM', aggfunc='max'),
                    avg_l2_dcm=pd.NamedAgg(column='L2 DCM', aggfunc=np.mean),
                    std_l2_dcm=pd.NamedAgg(column='L2 DCM', aggfunc=np.std),

                    min_tot_ins=pd.NamedAgg(column='TOT INS', aggfunc='min'),
                    max_tot_ins=pd.NamedAgg(column='TOT INS', aggfunc='max'),
                    avg_tot_ins=pd.NamedAgg(column='TOT INS', aggfunc=np.mean),
                    std_tot_ins=pd.NamedAgg(column='TOT INS', aggfunc=np.std),

                    min_tot_cyc=pd.NamedAgg(column='TOT CYC', aggfunc='min'),
                    max_tot_cyc=pd.NamedAgg(column='TOT CYC', aggfunc='max'),
                    avg_tot_cyc=pd.NamedAgg(column='TOT CYC', aggfunc=np.mean),
                    std_tot_cyc=pd.NamedAgg(column='TOT CYC', aggfunc=np.std),
        )
    elif prog_lang == 'java':
        results = results.groupby(["Matrix Size","Block Size"], as_index=False).agg(
                    min_time=pd.NamedAgg(column='Time', aggfunc='min'),
                    max_time=pd.NamedAgg(column='Time', aggfunc='max'),
                    avg_time=pd.NamedAgg(column='Time', aggfunc=np.mean),
                    std_time=pd.NamedAgg(column='Time', aggfunc=np.std),

                    min_gigaflops=pd.NamedAgg(column='gFlops', aggfunc='min'),
                    max_gigaflops=pd.NamedAgg(column='gFlops', aggfunc='max'),
                    avg_gigaflops=pd.NamedAgg(column='gFlops', aggfunc=np.mean),
                    std_gigaflops=pd.NamedAgg(column='gFlops', aggfunc=np.std),
        )
else:   
    if prog_lang == 'cpp':
        results = results.groupby("Matrix Size", as_index=False).agg(
                    min_time=pd.NamedAgg(column='Time', aggfunc='min'),
                    max_time=pd.NamedAgg(column='Time', aggfunc='max'),
                    avg_time=pd.NamedAgg(column='Time', aggfunc=np.mean),
                    std_time=pd.NamedAgg(column='Time', aggfunc=np.std),

                    min_gigaflops=pd.NamedAgg(column='gFlops', aggfunc='min'),
                    max_gigaflops=pd.NamedAgg(column='gFlops', aggfunc='max'),
                    avg_gigaflops=pd.NamedAgg(column='gFlops', aggfunc=np.mean),
                    std_gigaflops=pd.NamedAgg(column='gFlops', aggfunc=np.std),

                    min_l1_dcm=pd.NamedAgg(column='L1 DCM', aggfunc='min'),
                    max_l1_dcm=pd.NamedAgg(column='L1 DCM', aggfunc='max'),
                    avg_l1_dcm=pd.NamedAgg(column='L1 DCM', aggfunc=np.mean),
                    std_l1_dcm=pd.NamedAgg(column='L1 DCM', aggfunc=np.std),

                    min_l2_dcm=pd.NamedAgg(column='L2 DCM', aggfunc='min'),
                    max_l2_dcm=pd.NamedAgg(column='L2 DCM', aggfunc='max'),
                    avg_l2_dcm=pd.NamedAgg(column='L2 DCM', aggfunc=np.mean),
                    std_l2_dcm=pd.NamedAgg(column='L2 DCM', aggfunc=np.std),

                    min_tot_ins=pd.NamedAgg(column='TOT INS', aggfunc='min'),
                    max_tot_ins=pd.NamedAgg(column='TOT INS', aggfunc='max'),
                    avg_tot_ins=pd.NamedAgg(column='TOT INS', aggfunc=np.mean),
                    std_tot_ins=pd.NamedAgg(column='TOT INS', aggfunc=np.std),

                    min_tot_cyc=pd.NamedAgg(column='TOT CYC', aggfunc='min'),
                    max_tot_cyc=pd.NamedAgg(column='TOT CYC', aggfunc='max'),
                    avg_tot_cyc=pd.NamedAgg(column='TOT CYC', aggfunc=np.mean),
                    std_tot_cyc=pd.NamedAgg(column='TOT CYC', aggfunc=np.std),
        )
    elif prog_lang == 'java':
        results = results.groupby("Matrix Size", as_index=False).agg(
                    min_time=pd.NamedAgg(column='Time', aggfunc='min'),
                    max_time=pd.NamedAgg(column='Time', aggfunc='max'),
                    avg_time=pd.NamedAgg(column='Time', aggfunc=np.mean),
                    std_time=pd.NamedAgg(column='Time', aggfunc=np.std),

                    min_gigaflops=pd.NamedAgg(column='gFlops', aggfunc='min'),
                    max_gigaflops=pd.NamedAgg(column='gFlops', aggfunc='max'),
                    avg_gigaflops=pd.NamedAgg(column='gFlops', aggfunc=np.mean),
                    std_gigaflops=pd.NamedAgg(column='gFlops', aggfunc=np.std),
        )

# rename table columns' name
results.rename(columns={
    'min_time': 'Min Time',
    'max_time': 'Max Time',
    'avg_time': 'Avg Time',
    'std_time': 'Avg Time Standard Deviation',

    'min_gigaflops': 'Min GigaFlops',
    'max_gigaflops': 'Max GigaFlops',
    'avg_gigaflops': 'Avg GigaFlops',
    'std_gigaflops': 'Avg GigaFlops Standard Deviation',}, inplace = True)
if prog_lang == 'cpp':
    results.rename(columns={
        'min_l1_dcm': 'Min L1 DCM',
        'max_l1_dcm': 'Max L1 DCM',
        'avg_l1_dcm': 'Avg L1 DCM',
        'std_l1_dcm': 'Avg L1 DCM Standard Deviation',

        'min_l2_dcm': 'Min L2 DCM',
        'max_l2_dcm': 'Max L2 DCM',
        'avg_l2_dcm': 'Avg L2 DCM',
        'std_l2_dcm': 'Avg L2 DCM Standard Deviation',

        'min_tot_ins': 'Min TOT INS',
        'max_tot_ins': 'Max TOT INS',
        'avg_tot_ins': 'Avg TOT INS',
        'std_tot_ins': 'Avg TOT INS Standard Deviation',

        'min_tot_cyc': 'Min TOT CYC',
        'max_tot_cyc': 'Max TOT CYC',
        'avg_tot_cyc': 'Avg TOT CYC',
        'std_tot_cyc': 'Avg TOT CYC Standard Deviation',}, inplace = True)
    results.insert(0, "Language", "C++")
elif prog_lang == 'java':
    results.insert(0, "Language", "Java")

######################### SAVING AS A CSV FILE

if (prog_lang == 'cpp') or (prog_lang == 'java'):
    # save results in csv file
    results.to_csv(working_dir+"/processed_docs/"+file_name+'.csv', index=False)
