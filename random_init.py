import random

k = random.sample(range(0, 1000000), 50000)
with open('out2.txt', 'w', encoding='utf-8') as f:
    for out in k:
        f.write(str(out) + '\n')
