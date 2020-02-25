import sys
import random
import time

sys.setrecursionlimit(100000)

k2 = []
fo = open('out.txt')
for line in fo.readlines():
    k2.append(int(line))

start = time.time()


def partition(arr, low, high):
    i = (low - 1)
    pivot = arr[high]
    for j in range(low, high):
        if arr[j] <= pivot:
            i = i + 1
            arr[i], arr[j] = arr[j], arr[i]

    arr[i + 1], arr[high] = arr[high], arr[i + 1]
    return i + 1


def quickSort(arr, low, high):
    if low < high:
        pi = partition(arr, low, high)

        quickSort(arr, low, pi - 1)
        quickSort(arr, pi + 1, high)


n = len(k2)
quickSort(k2, 0, n - 1)
print(k2)
end = time.time()
print("程序的运行时间为: %f ms" % ((end - start) * 100))
