//
//  main.c
//  C
//
//  Created by 崔程远 on 2020/2/21.
//  Copyright © 2020 qc. All rights reserved.
//

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#define MAX_LINE 1024

void swap(int *arr1, int *arr2){
    int temp=*arr2;
    *arr2=*arr1;
    *arr1=temp;
}

int partition(int *arr,int low,int high){
    int i = low-1;
    int pivot = arr[high];
    for(int j=low;j<high;j++){
        if(arr[j]<=pivot){
            i=i+1;
            swap(&arr[i],&arr[j]);
        }
    }
    swap(&arr[i+1], &arr[high]);
    return i+1;
}

void quickSort(int *arr,int low,int high){
    if(low<high){
        int pi=partition(arr,low,high);
        quickSort(arr,low,pi-1);
        quickSort(arr,pi+1,high);
    }
}

int main()
{
    char buf[MAX_LINE];  /*缓冲区*/
    FILE *fp;            /*文件指针*/
    long long int len;             /*行字符个数*/
    int g[500000];
    if((fp = fopen("/Users/cuichengyuan/Development/编译原理/C/C/out.txt","r")) == NULL)
    {
        perror("fail to read");
        exit (1) ;
    }
    int k=0;
    while(fgets(buf,MAX_LINE,fp) != NULL)
    {
        len = strlen(buf);
        buf[len-1] = '\0';  /*去掉换行符*/
        g[k++]=atoi(buf);
    }
    struct timeval start,end;
    gettimeofday(&start, NULL);
    quickSort(g,0,49999);
    printf("[");
    for(int i=0;i<=49999;i++){
        printf("%d,",g[i]);
    }
    printf("]");
    gettimeofday(&end, NULL);
    printf("程序的运行时间为 %ld ms\n",(end.tv_sec-start.tv_sec)*1000+(end.tv_usec-start.tv_usec)/1000);
    return 0;
}
