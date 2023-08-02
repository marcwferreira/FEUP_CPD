#include <stdio.h>
#include <iostream>
#include <iomanip>
#include <time.h>
#include <cstdlib>
#include <papi.h>
#include <string.h>

using namespace std;

#define SYSTEMTIME clock_t

 
void OnMult(int m_ar, int m_br, bool human_prints) 
{
	
	SYSTEMTIME Time1, Time2;
	
	char st[100];
	double temp;
	int i, j, k;

	double *pha, *phb, *phc;
	
    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for(i=0; i<m_ar; i++)
		for(j=0; j<m_ar; j++)
			pha[i*m_ar + j] = (double)1.0;



	for(i=0; i<m_br; i++)
		for(j=0; j<m_br; j++)
			phb[i*m_br + j] = (double)(i+1);



    Time1 = clock();

	for(i=0; i<m_ar; i++)
	{	for( j=0; j<m_br; j++)
		{	temp = 0;
			for( k=0; k<m_ar; k++)
			{	
				temp += pha[i*m_ar+k] * phb[k*m_br+j];
			}
			phc[i*m_ar+j]=temp;
		}
	}


    Time2 = clock();
	if(human_prints){
		sprintf(st, "Time: %3.3f seconds\n", (double)(Time2 - Time1) / CLOCKS_PER_SEC);
		cout << st;

		// display 10 elements of the result matrix tto verify correctness
		cout << "Result matrix: " << endl;
		for(i=0; i<1; i++)
		{	for(j=0; j<min(10,m_br); j++)
				cout << phc[j] << " ";
		}
		cout << endl;
	}
	else {
		cout << (double)(Time2 - Time1) / CLOCKS_PER_SEC << ",";	
	}

    free(pha);
    free(phb);
    free(phc);
	
	
}

// add code here for line x line matriz multiplication
void OnMultLine(int m_ar, int m_br, bool human_prints)
{
    SYSTEMTIME Time1, Time2;
	
	char st[100];
	double temp;
	int i, j, k;

	double *pha, *phb, *phc;
	

		
    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	memset(phc,0,m_ar*m_br*sizeof(double));

	for(i=0; i<m_ar; i++)
		for(j=0; j<m_ar; j++)
			pha[i*m_ar + j] = (double)1.0;



	for(i=0; i<m_br; i++)
		for(j=0; j<m_br; j++)
			phb[i*m_br + j] = (double)(i+1);



    Time1 = clock();

	for(i=0; i<m_ar; i++)
	{	for( k=0; k<m_ar; k++)
		{
			for( j=0; j<m_br; j++)
			{	
				phc[i*m_ar+j] += pha[i*m_ar+k] * phb[k*m_br+j];
			}
		}
	}


    Time2 = clock();
	if(human_prints){
		sprintf(st, "Time: %3.3f seconds\n", (double)(Time2 - Time1) / CLOCKS_PER_SEC);
		cout << st;

		// display 10 elements of the result matrix tto verify correctness
		cout << "Result matrix: " << endl;
		for(i=0; i<1; i++)
		{	for(j=0; j<min(10,m_br); j++)
				cout << phc[j] << " ";
		}
		cout << endl;
	}
	else {
		cout << (double)(Time2 - Time1) / CLOCKS_PER_SEC << ",";	
	}

    free(pha);
    free(phb);
    free(phc);
	
    
}

// add code here for block x block matriz multiplication
void OnMultBlock(int m_ar, int m_br, int bkSize, bool human_prints)
{
	SYSTEMTIME Time1, Time2;
	
	char st[100];
	double temp;
	int a_line_block, k_block, b_col_block, a_line, k, b_col;
    int n_blocks = m_ar / bkSize;

	double *pha, *phb, *phc;
	
    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	memset(phc,0,m_ar*m_br*sizeof(double));

	for(int i=0; i<m_ar; i++)
		for(int j=0; j<m_ar; j++)
			pha[i*m_ar + j] = (double)1.0;



	for(int i=0; i<m_br; i++)
		for(int j=0; j<m_br; j++)
			phb[i*m_br + j] = (double)(i+1);

    Time1 = clock();

	for(a_line_block = 0; a_line_block < n_blocks; a_line_block++) {
		for( k_block=0; k_block < n_blocks; k_block++) {	
			for( b_col_block=0; b_col_block < n_blocks; b_col_block++) {
				int next_line_block = (a_line_block+1) * bkSize;
				for (a_line = a_line_block*bkSize; a_line < next_line_block; a_line++) {
					int k_next_block = (k_block + 1) * bkSize;
					for ( k = k_block * bkSize; k < k_next_block; k++) {
						int b_next_block = (b_col_block+1)*bkSize;
						for (b_col = b_col_block * bkSize; b_col < b_next_block; b_col++) {
							phc[a_line * m_ar + b_col] += pha[a_line * m_ar + k] * phb[k * m_ar + b_col];
						}
					}
				}
			}
        }
    }

	Time2 = clock();
	if(human_prints) {
		sprintf(st, "Time: %3.3f seconds\n", (double)(Time2 - Time1) / CLOCKS_PER_SEC);
		cout << st;

		// display 10 elements of the result matrix tto verify correctness
		cout << "Result matrix: " << endl;
		for(int i=0; i<1; i++)
		{	for(int j=0; j<min(10,m_br); j++)
				cout << phc[j] << " ";
		}
		cout << endl;
	}
	else{
		cout << (double)(Time2 - Time1) / CLOCKS_PER_SEC << ",";	
	}

    free(pha);
    free(phb);
    free(phc); 
    
}



void handle_error (int retval)
{
  printf("PAPI error %d: %s\n", retval, PAPI_strerror(retval));
  exit(1);
}

void init_papi() {
  int retval = PAPI_library_init(PAPI_VER_CURRENT);
  if (retval != PAPI_VER_CURRENT && retval < 0) {
    printf("PAPI library version mismatch!\n");
    exit(1);
  }
  if (retval < 0) handle_error(retval);

  std::cout << "PAPI Version Number: MAJOR: " << PAPI_VERSION_MAJOR(retval)
            << " MINOR: " << PAPI_VERSION_MINOR(retval)
            << " REVISION: " << PAPI_VERSION_REVISION(retval) << "\n";
}


int main (int argc, char *argv[])
{

	bool human_prints = true;

	char c;
	int lin, col, blockSize;
	int op;
	
	int EventSet = PAPI_NULL;
  	long long values[4];
  	int ret;
	

	ret = PAPI_library_init( PAPI_VER_CURRENT );
	if ( ret != PAPI_VER_CURRENT )
		std::cout << "FAIL" << endl;


	ret = PAPI_create_eventset(&EventSet);
		if (ret != PAPI_OK) cout << "ERROR: create eventset" << endl;


	ret = PAPI_add_event(EventSet,PAPI_L1_DCM );
	if (ret != PAPI_OK) cout << "ERROR: PAPI_L1_DCM" << endl;


	ret = PAPI_add_event(EventSet,PAPI_L2_DCM);
	if (ret != PAPI_OK) cout << "ERROR: PAPI_L2_DCM" << endl;

	ret = PAPI_add_event(EventSet,PAPI_TOT_INS);
	if (ret != PAPI_OK) cout << "ERROR: PAPI_TOT_INS" << endl;

	ret = PAPI_add_event(EventSet,PAPI_TOT_CYC);
	if (ret != PAPI_OK) cout << "ERROR: PAPI_TOT_CYC" << endl;


	op=1;
	do {
		if(human_prints) {
			cout << endl << "1. Multiplication" << endl;
			cout << "2. Line Multiplication" << endl;
			cout << "3. Block Multiplication" << endl;
			cout << "Selection?: ";
		}
		cin >>op;
		if (op == 0)
			break;
		if(human_prints) printf("Dimensions: lins=cols ? ");
   		cin >> lin;
   		col = lin;


		// Start counting
		ret = PAPI_start(EventSet);
		if (ret != PAPI_OK) cout << "ERROR: Start PAPI" << endl;

		
		switch (op){
			case 1: 
				OnMult(lin, col, human_prints);
				break;
			case 2:
				OnMultLine(lin, col, human_prints);  
				break;
			case 3:
				if(human_prints) cout << "Block Size? ";
				cin >> blockSize;
				OnMultBlock(lin, col, blockSize, human_prints);  
				break;

		}

  		ret = PAPI_stop(EventSet, values);
	
  		if (ret != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;

		if(human_prints) { 
			printf("L1 DCM: %lld \n",values[0]);
			printf("L2 DCM: %lld \n",values[1]);
			printf("TOT INS: %lld \n",values[2]);
			printf("TOT CYC: %lld \n",values[3]);
		}
		else{
			cout << values[0] << ",";
			cout << values[1] << ",";
			cout << values[2] << ",";
			cout << values[3] << endl;
		}

		ret = PAPI_reset( EventSet );
		if ( ret != PAPI_OK )
			std::cout << "FAIL reset" << endl; 

	}while (op != 0 && human_prints );

	ret = PAPI_remove_event( EventSet, PAPI_L1_DCM );
	if ( ret != PAPI_OK )
		std::cout << "FAIL remove event" << endl; 

	ret = PAPI_remove_event( EventSet, PAPI_L2_DCM );
	if ( ret != PAPI_OK )
		std::cout << "FAIL remove event" << endl;
		

	ret = PAPI_remove_event( EventSet, PAPI_TOT_INS );
	if ( ret != PAPI_OK )
		std::cout << "FAIL remove event" << endl; 

	ret = PAPI_remove_event( EventSet, PAPI_TOT_CYC );
	if ( ret != PAPI_OK )
		std::cout << "FAIL remove event" << endl; 

	ret = PAPI_destroy_eventset( &EventSet );
	if ( ret != PAPI_OK )
		std::cout << "FAIL destroy" << endl;

}
