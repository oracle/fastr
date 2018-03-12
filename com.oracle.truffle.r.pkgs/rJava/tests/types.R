library(rJava)
.jinit(".")
cat(" - instantiate Types class\n")
t=.jnew("Types")
ts=c("Z","B","C","T","I","J","D","F","Ljava/lang/String;")
tn=c("z","b","c","s","i","j","d","f","S")
ty=c("boolean","byte","char","short","int","long","double","float","string")
ev=list(TRUE, 123L, 99L, 1234L, 1234L, 1234567890, 1234.567, 1234.567, "ok")
for (i in 1:length(ts)) {
  cat(" -",ty[i],"\n")
#  cat("    static call\n");
  sr=.jcall("Types",ts[i],paste('sr',tn[i],sep=''))
#  cat("    call\n");
  r=.jcall(t,ts[i],paste('r',tn[i],sep=''))
#  cat("    static+sig\n");
  sfr=.jfield("Types",ts[i],paste('s',tn[i],sep=''))
#  cat("    sig\n");
  fr=.jfield(t,ts[i],tn[i])
#  cat("    static\n");
  .sfr=.jfield("Types",,paste('s',tn[i],sep=''))
  if (.sfr != sfr) stop("static field test failed: different results with and without a signature")
#  cat("    no sig\n");
  .fr=.jfield(t,,tn[i])
  if (.fr != fr) stop("field test failed: different results with and without a signature")
  ..sfr=.jfield(t,,paste('s',tn[i],sep=''))
  if (..sfr != .sfr) stop("field test failed: different results fetching a static field from an instance and the class")
  if (tn[i] == 'f') {
    if ((sr-ev[[i]])^2>1e-8) stop("failed static return test for ",ts[i])
    if ((r-ev[[i]])^2>1e-8) stop("failed return test for ",ts[i])
    if ((sfr-ev[[i]])^2>1e-8) stop("failed static field test for ",ts[i])
    if ((fr-ev[[i]])^2>1e-8) stop("failed field test for ",ts[i])
  } else {
    if (sr != ev[[i]]) stop("failed static return test for ",ts[i])
    if (r != ev[[i]]) stop("failed return test for ",ts[i])
    if (sfr != ev[[i]]) stop("failed static field test for ",ts[i])
    if (fr != ev[[i]]) stop("failed field test for ",ts[i])
  }
}
cat(" - calling a static method with all types\n")
.jcall("Types", "V", "szbcsijfdS",
       TRUE, .jbyte(-123), .jchar(66), .jshort(4321), 4321L,
       .jlong(9876543210), .jfloat(4321.12), 4321.1234, "foo")
cat(" - calling a method with all types\n")
.jcall(t, "V", "zbcsijfdS",
       TRUE, .jbyte(-123), .jchar(66), .jshort(4321), 4321L,
       .jlong(9876543210), .jfloat(4321.12), 4321.1234, "foo")
