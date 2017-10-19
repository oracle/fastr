pdf(file = "reg-plot-latin1.pdf", encoding = "ISOLatin1",
    width = 7, height = 7, paper = "a4r", compress = FALSE)
if(FALSE) { # [FastR] BEGIN Test snippet disabled due to graphics package use
library(graphics) # to be sure
example(text)     # has examples that need to he plotted in latin-1
} # [FastR] END Test snippet disabled due to graphics package use
q("no")
