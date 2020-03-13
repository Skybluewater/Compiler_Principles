import sys

k1 = []
k2 = []

fo0 = open(input("FilePath1: "))
fo1 = open(input("FilePath2: "))

for line in fo0.readlines():
    k2.append(str(line))

for line in fo1.readlines():
    k1.append(str(line))

for i in range(k1.__len__()):
    str1 = k1[i]
    str2 = k2[i]
    if k1 == k2:
        continue
    else:
        print("Line \(i) is different")

print("All is well")
