import System.IO
import System.CPUTime
import Text.Printf

taint :: [Char]->Int
taint' :: Char->Int
taint' s
    |s=='1' =1
    |s=='2' =2
    |s=='3' =3
    |s=='4' =4
    |s=='5' =5
    |s=='6' =6
    |s=='7' =7
    |s=='8' =8
    |s=='9' =9
    |s=='0' =0
    |otherwise = 0
taint (s:xs) = if null xs then taint' s else (taint xs)*10+(taint' s)

hGetLines :: Handle -> IO [Int]
hGetLines h = do fileOk <- hIsOpen h
                 fileEof <- hIsEOF h
                 if fileOk && not fileEof
                 then do
                     current <- hGetLine h
                     let k = taint(reverse(current))
                     others  <- hGetLines h
                     return $ k:others
                 else return []

quickSort :: [Int] -> [Int]
quickSort [] = []
quickSort (x : xs) =
    let smallerOrEqual = [a | a <- xs, a <= x]
        larger         = [a | a <- xs, a > x]
    in quickSort smallerOrEqual ++ [x] ++ quickSort larger

puts = do
    h <- openFile "/Users/cuichengyuan/Development/编译原理/编译原理实验一/C/C/out.txt" ReadMode
    content <- hGetLines h
    start <- getCPUTime
    let k = quickSort content
    end <- getCPUTime
    print k
    end1<- getCPUTime
    let diff = (fromIntegral (end - start)) / (10^9)
    printf "程序运行时间: %0.9f ms\n" (diff :: Double)
    let diff2 = (fromIntegral (end1 - start)) / (10^9)
    printf "程序运行时间: %0.9f ms\n" (diff2 :: Double)
    --quickSort content

main = puts
