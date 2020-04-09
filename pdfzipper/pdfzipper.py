import fitz
import glob
import os

'''
pdffile = "/Users/cuichengyuan/Downloads/071117071120171189.pdf"
doc = fitz.open(pdffile)
width, height = fitz.PaperSize("a4")

totaling = doc.pageCount

for pg in range(totaling):
    page = doc[pg]
    zoom = int(100)
    rotate = int(0)
    print(page)
    trans = fitz.Matrix(zoom / 100.0, zoom / 100.0).preRotate(rotate)
    pm = page.getPixmap(matrix=trans, alpha=False)

    lurl = '/Users/cuichengyuan/Downloads/pdf/%s.jpg' % str(pg + 1)
    pm.writePNG(lurl)
doc.close()
'''


# coding:utf-8


def pictopdf():
    doc = fitz.open()
    for img in sorted(glob.glob("/Users/cuichengyuan/Downloads/pdf/*")):  # 读取图片，确保按文件名排序
        print(img)
        imgdoc = fitz.open(img)  # 打开图片
        pdfbytes = imgdoc.convertToPDF()  # 使用图片创建单页的 PDF
        imgpdf = fitz.open("pdf", pdfbytes)
        doc.insertPDF(imgpdf)  # 将当前页插入文档
    if os.path.exists("newpdf.pdf"):  # 若文件存在先删除
        os.remove("/Users/cuichengyuan/Downloads/071117071120171189_2.pdf")
    doc.save("/Users/cuichengyuan/Downloads/071117071120171189_2.pdf")  # 保存pdf文件
    doc.close()
