# Ignored
# This test does currently not work on Java because there is simply no way in Java for opening a UNIX named pipe non-blocking.
if(capabilities("fifo")) {
  zz <- fifo("foo-fifo", "w+")
  writeLines("abc", zz)
  print(readLines(zz))
  close(zz)
  unlink("foo-fifo")
}
